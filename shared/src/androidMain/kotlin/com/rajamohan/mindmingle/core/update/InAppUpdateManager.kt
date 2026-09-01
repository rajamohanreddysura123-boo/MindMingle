package com.rajamohan.mindmingle.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.rajamohan.mindmingle.domain.model.AppUpdateConfig
import com.rajamohan.mindmingle.domain.usecase.GetAppUpdateConfigUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import java.lang.ref.WeakReference

/**
 * The single owner of Google Play In-App Updates for the Android app.
 *
 * ## Why a process-scoped object
 * A Play update flow outlives the activity that started it: the user can rotate, background the
 * app mid-download, or be sent to the Play install screen and come back minutes later. Holding
 * the [AppUpdateManager] and the "already asked this session" flags in a process singleton (the
 * same shape as [com.rajamohan.mindmingle.core.media.ImagePicker] and GoogleAuthLauncher in this
 * codebase) is what keeps activity recreation from firing a second check or a second dialog.
 * The activity is held weakly and re-attached in onCreate, so no leak survives a rotation.
 *
 * ## The four separate concepts, kept separate on purpose
 *  - **Play update availability** — [AppUpdateInfo.updateAvailability]. Only Play knows this,
 *    and only for the exact track/rollout this device is eligible for.
 *  - **Optional update** — an available update with no urgency signal. Runs FLEXIBLE: Play asks,
 *    downloads in the background, the app stays usable, the app asks to restart at the end.
 *  - **Mandatory update** — urgency, from either Play's per-release `updatePriority` (>= 4),
 *    Play's `clientVersionStalenessDays`, or the backend version floor. Runs IMMEDIATE: Play
 *    takes over the screen and restarts the app itself.
 *  - **Minimum supported version** — [AppUpdateConfig], a Firestore doc. It is the only one of
 *    the four that can change *after* a release has shipped.
 *
 * All comparisons use versionCode (Long), never versionName — "1.8.10" < "1.8.9" as a string.
 *
 * ## Install source
 * Play In-App Updates only work for an app installed and updatable by the Play Store. Debug
 * builds, `adb install`, sideloaded APKs and non-Play distributions get an error from the API
 * (typically -3 INSTALL_NOT_ALLOWED / -10 APP_NOT_OWNED); every one of those is logged and
 * swallowed, and the app keeps running.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"

    /** Delivered to MainActivity.onActivityResult; kept distinct from RC_SIGN_IN/RC_PICK_IMAGES. */
    const val RC_APP_UPDATE = 9010

    /**
     * Play's per-release `inAppUpdatePriority` (0..5), set through the Publishing API at rollout
     * time. 4 and 5 are documented as "ask immediately / update as soon as possible", so they map
     * to the blocking IMMEDIATE flow. 0..3 stay optional.
     */
    private const val IMMEDIATE_PRIORITY_THRESHOLD = 4

    /** A user who has been ignoring an available update for this long stops getting the choice. */
    private const val IMMEDIATE_STALENESS_DAYS = 14

    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    // Runs the one suspending piece of the flow (the Firestore config read). Play's own Task
    // callbacks arrive on the main thread, so nothing here needs Dispatchers.Main.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var activityRef: WeakReference<Activity>? = null
    private var playUpdateManager: AppUpdateManager? = null
    private var promptListener: ((AppUpdatePrompt) -> Unit)? = null

    // --- session state -----------------------------------------------------------------
    // Process-scoped on purpose: "Later" must survive an activity recreation but not a cold
    // start, which is exactly the "ask again on a future launch" strategy asked for.
    private var checkInFlight = false
    private var updateFlowInProgress = false
    private var declinedThisSession = false
    private var awaitingUserRetry = false
    private var mandatory = false
    private var lastFlowType = AppUpdateType.FLEXIBLE
    private var remoteConfig: AppUpdateConfig? = null
    private var installedVersionCode = 0L

    private val installListener = InstallStateUpdatedListener { state -> onInstallStateChanged(state) }

    // ---------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------

    /**
     * Call from MainActivity.onCreate. Safe to call again after an activity recreation — the
     * Play manager and every session flag are deliberately kept.
     */
    fun attach(activity: Activity, onPrompt: (AppUpdatePrompt) -> Unit) {
        try {
            activityRef = WeakReference(activity)
            promptListener = onPrompt
            installedVersionCode = readInstalledVersionCode(activity)

            if (playUpdateManager == null) {
                playUpdateManager = AppUpdateManagerFactory.create(activity.applicationContext).also {
                    it.registerListener(installListener)
                }
            }
            Napier.i(tag = TAG) {
                "attached versionCode=$installedVersionCode installer=${installerPackage(activity)}"
            }
        } catch (e: Exception) {
            // Never let update plumbing take the app down on startup.
            Napier.e(throwable = e, tag = TAG) { "attach failed, update checks disabled this session" }
            playUpdateManager = null
        }
    }

    /**
     * Call from MainActivity.onResume — the single entry point for both "check for an update"
     * and "resume whatever was already in flight". One Play request answers both questions, so
     * returning to the foreground can never produce two dialogs.
     */
    fun onResume() {
        checkForUpdate()
    }

    /** Call from MainActivity.onDestroy. Only a finishing activity tears the Play manager down. */
    fun detach(activity: Activity) {
        try {
            if (activityRef?.get() !== activity) return
            activityRef = null
            promptListener = null

            // A configuration change destroys the activity but not the flow — keep listening.
            // A real finish means the process may be going away; drop the listener so the Play
            // manager holds nothing.
            if (activity.isFinishing) {
                playUpdateManager?.unregisterListener(installListener)
                playUpdateManager = null
            }
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "detach failed" }
        }
    }

    // ---------------------------------------------------------------------------------
    // Update check
    // ---------------------------------------------------------------------------------

    /**
     * Asks Play what it has, then decides flexible vs immediate vs nothing. Re-entrant calls
     * while a check or a Play dialog is in flight are dropped, which is what stops duplicate
     * prompts when onResume fires twice in quick succession.
     */
    fun checkForUpdate() {
        if (checkInFlight || updateFlowInProgress) {
            Napier.d(tag = TAG) { "check skipped (inFlight=$checkInFlight flow=$updateFlowInProgress)" }
            return
        }
        if (playUpdateManager == null) return

        checkInFlight = true
        scope.launch {
            loadRemoteConfigIfNeeded()
            requestUpdateInfo { info -> evaluate(info) }
        }
    }

    /** Wired to "Update Now" on the mandatory wall, after the user backed out of Play's screen. */
    fun startImmediateUpdate() {
        awaitingUserRetry = false
        requestUpdateInfo { info -> launchFlow(info, AppUpdateType.IMMEDIATE) }
    }

    /**
     * Restarts the app into a flexible update that has finished downloading. Play kills and
     * relaunches the process, so nothing after this call is guaranteed to run.
     */
    fun completeFlexibleUpdate() {
        try {
            playUpdateManager?.completeUpdate()
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "completeUpdate failed" }
            emit(AppUpdatePrompt.None)
        }
    }

    /**
     * Last resort for a forced update Play cannot serve in-app: hand the user the store listing.
     * Falls back to the browser when the Play app is missing.
     */
    fun openPlayStoreListing() {
        val activity = activityRef?.get() ?: return
        val packageName = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .setPackage(PLAY_STORE_PACKAGE)
            )
        } catch (e: ActivityNotFoundException) {
            Napier.w(throwable = e, tag = TAG) { "Play app missing, falling back to web listing" }
            try {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            } catch (inner: Exception) {
                Napier.e(throwable = inner, tag = TAG) { "no handler for the Play listing" }
            }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "openPlayStoreListing failed" }
        }
    }

    // ---------------------------------------------------------------------------------
    // Results
    // ---------------------------------------------------------------------------------

    /**
     * Wired from MainActivity.onActivityResult, the same way GoogleAuthLauncher and ImagePicker
     * are. Returns true when the result belonged to an update flow.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != RC_APP_UPDATE) return false
        handleUpdateResult(resultCode)
        return true
    }

    /** The result half of [handleActivityResult], split out so it can be tested/called directly. */
    fun handleUpdateResult(resultCode: Int) {
        updateFlowInProgress = false
        when (resultCode) {
            Activity.RESULT_OK -> {
                // FLEXIBLE: the user accepted, the download is now running in the background.
                // IMMEDIATE: rarely seen — Play normally restarts the app instead of returning.
                Napier.i(tag = TAG) { "update flow accepted (type=$lastFlowType)" }
            }

            Activity.RESULT_CANCELED -> {
                Napier.i(tag = TAG) { "update flow cancelled by user (type=$lastFlowType)" }
                onFlowDismissed()
            }

            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                Napier.w(tag = TAG) { "update flow failed inside Play (type=$lastFlowType)" }
                onFlowDismissed()
            }

            else -> Napier.w(tag = TAG) { "unexpected update resultCode=$resultCode" }
        }
    }

    /**
     * A dismissed *optional* update goes quiet for the rest of the session. A dismissed
     * *mandatory* one puts up the wall instead of instantly relaunching Play, which would trap
     * the user in a loop they cannot read or escape.
     */
    private fun onFlowDismissed() {
        if (mandatory || lastFlowType == AppUpdateType.IMMEDIATE) {
            awaitingUserRetry = true
            emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = true))
        } else {
            declinedThisSession = true
            emit(AppUpdatePrompt.None)
        }
    }

    private fun onInstallStateChanged(state: InstallState) {
        try {
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val total = state.totalBytesToDownload()
                    val done = state.bytesDownloaded()
                    val percent = if (total > 0L) ((done * 100L) / total).toInt() else 0
                    emit(AppUpdatePrompt.Downloading(percent))
                }

                InstallStatus.DOWNLOADED -> {
                    updateFlowInProgress = false
                    Napier.i(tag = TAG) { "flexible update downloaded, asking to restart" }
                    emit(AppUpdatePrompt.ReadyToInstall)
                }

                InstallStatus.INSTALLED -> {
                    updateFlowInProgress = false
                    emit(AppUpdatePrompt.None)
                }

                InstallStatus.CANCELED -> {
                    updateFlowInProgress = false
                    declinedThisSession = true
                    emit(AppUpdatePrompt.None)
                }

                InstallStatus.FAILED -> {
                    updateFlowInProgress = false
                    Napier.w(tag = TAG) { "flexible download failed, error=${state.installErrorCode()}" }
                    emit(AppUpdatePrompt.None)
                }

                else -> Unit
            }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "install state handling failed" }
        }
    }

    // ---------------------------------------------------------------------------------
    // Decision
    // ---------------------------------------------------------------------------------

    private fun evaluate(info: AppUpdateInfo) {
        val belowMinimum = remoteConfig?.isBelowMinimum(installedVersionCode) == true
        mandatory = belowMinimum

        // A finished flexible download outranks everything: the bits are already on the device.
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            emit(AppUpdatePrompt.ReadyToInstall)
            return
        }

        when (info.updateAvailability()) {
            // An immediate update was started and then interrupted (user left the Play screen,
            // process died mid-flow). Play expects it to be resumed.
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                if (awaitingUserRetry) {
                    emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = true))
                } else {
                    launchFlow(info, AppUpdateType.IMMEDIATE)
                }
            }

            UpdateAvailability.UPDATE_AVAILABLE -> {
                val staleness = info.clientVersionStalenessDays() ?: 0
                val priority = info.updatePriority()
                val forced = belowMinimum ||
                    priority >= IMMEDIATE_PRIORITY_THRESHOLD ||
                    staleness >= IMMEDIATE_STALENESS_DAYS
                mandatory = forced

                Napier.i(tag = TAG) {
                    "update available code=${info.availableVersionCode()} priority=$priority " +
                        "staleness=$staleness belowMinimum=$belowMinimum"
                }

                when {
                    forced && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                        if (awaitingUserRetry) {
                            emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = true))
                        } else {
                            launchFlow(info, AppUpdateType.IMMEDIATE)
                        }
                    }

                    // Forced, but this device cannot run an immediate flow (rare: unmanaged
                    // install, asset-pack constraint). Send them to the listing instead.
                    forced -> emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = false))

                    declinedThisSession -> Napier.d(tag = TAG) { "optional update suppressed for this session" }

                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                        launchFlow(info, AppUpdateType.FLEXIBLE)

                    else -> Napier.w(tag = TAG) { "no update type allowed for this install" }
                }
            }

            UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                // Play has nothing newer, yet the backend says this build is out of support —
                // usually a rollout that has not reached this device. Blocking with a store
                // link is the only honest option.
                if (belowMinimum) {
                    emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = false))
                } else {
                    emit(AppUpdatePrompt.None)
                }
            }

            else -> {
                // UNKNOWN: Play could not answer (no Play Store, sideloaded build, offline).
                Napier.i(tag = TAG) { "update availability unknown" }
                if (belowMinimum) emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = false))
            }
        }
    }

    /** [type] is an [AppUpdateType] constant — FLEXIBLE or IMMEDIATE. */
    private fun launchFlow(info: AppUpdateInfo, type: Int) {
        val activity = activityRef?.get()
        val manager = playUpdateManager
        if (activity == null || manager == null) {
            Napier.w(tag = TAG) { "no activity/manager to launch the update flow" }
            return
        }
        if (updateFlowInProgress) return

        try {
            updateFlowInProgress = true
            lastFlowType = type
            manager.startUpdateFlowForResult(
                info,
                activity,
                AppUpdateOptions.newBuilder(type).build(),
                RC_APP_UPDATE
            )
            Napier.i(tag = TAG) { "started ${if (type == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"} flow" }
        } catch (e: Exception) {
            // SendIntentException, a dead activity, an AppUpdateInfo already consumed by an
            // earlier flow — none of these are worth a crash.
            updateFlowInProgress = false
            Napier.e(throwable = e, tag = TAG) { "startUpdateFlowForResult failed" }
            if (mandatory) emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = false))
        }
    }

    private fun requestUpdateInfo(onInfo: (AppUpdateInfo) -> Unit) {
        val manager = playUpdateManager
        if (manager == null) {
            checkInFlight = false
            return
        }
        try {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    checkInFlight = false
                    try {
                        onInfo(info)
                    } catch (e: Exception) {
                        Napier.e(throwable = e, tag = TAG) { "update evaluation failed" }
                    }
                }
                .addOnFailureListener { error ->
                    checkInFlight = false
                    onCheckFailed(error)
                }
        } catch (e: Exception) {
            checkInFlight = false
            onCheckFailed(e)
        }
    }

    /**
     * Play could not be reached or refuses to serve this install (debug build, sideload, no
     * Play Services, offline). The app carries on — unless it has already decided on its own
     * that this build is out of support, which is the one case a failed check must not clear.
     */
    private fun onCheckFailed(error: Throwable) {
        Napier.w(throwable = error, tag = TAG) { "app update check failed" }
        if (remoteConfig?.isBelowMinimum(installedVersionCode) == true) {
            emit(AppUpdatePrompt.UpdateRequired(canUseInAppUpdate = false))
        }
    }

    // ---------------------------------------------------------------------------------
    // Backend floor + platform bits
    // ---------------------------------------------------------------------------------

    /** Read once per process; a failure caches the neutral "no floor" config rather than retrying. */
    private suspend fun loadRemoteConfigIfNeeded() {
        if (remoteConfig != null) return
        remoteConfig = try {
            KoinPlatform.getKoin().get<GetAppUpdateConfigUseCase>().invoke()
        } catch (e: Exception) {
            // Koin not started yet, or Firestore unreachable. Fail open.
            Napier.w(throwable = e, tag = TAG) { "minimum supported version unavailable" }
            AppUpdateConfig()
        }
        mandatory = remoteConfig?.isBelowMinimum(installedVersionCode) == true
        Napier.i(tag = TAG) {
            "version floor=${remoteConfig?.minimumVersionCode} installed=$installedVersionCode mandatory=$mandatory"
        }
    }

    private fun readInstalledVersionCode(activity: Activity): Long = try {
        // minSdk is 29, so longVersionCode is always available — no deprecated versionCode path.
        activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
    } catch (e: Exception) {
        Napier.e(throwable = e, tag = TAG) { "could not read own versionCode" }
        0L
    }

    /** Diagnostics only. A non-Play installer explains every "update not available" in the logs. */
    private fun installerPackage(activity: Activity): String? = try {
        val pm = activity.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(activity.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(activity.packageName)
        }
    } catch (e: Exception) {
        null
    }

    private fun emit(prompt: AppUpdatePrompt) {
        val activity = activityRef?.get() ?: return
        val listener = promptListener ?: return
        try {
            activity.runOnUiThread {
                try {
                    listener(prompt)
                } catch (e: Exception) {
                    Napier.e(throwable = e, tag = TAG) { "update prompt listener threw" }
                }
            }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "could not deliver update prompt" }
        }
    }
}
