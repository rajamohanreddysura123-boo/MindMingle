package com.rajamohan.mindmingle

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rajamohan.mindmingle.core.AppContext
import com.rajamohan.mindmingle.core.media.ImagePicker
import com.rajamohan.mindmingle.core.payments.RazorpayBridge
import com.rajamohan.mindmingle.core.push.NotificationDestination
import com.rajamohan.mindmingle.core.push.PendingDestination
import com.rajamohan.mindmingle.core.update.AppUpdatePrompt
import com.rajamohan.mindmingle.core.update.InAppUpdateManager
import com.rajamohan.mindmingle.data.remote.source.GoogleAuthLauncher
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

/**
 * Razorpay hands its checkout result to the activity that opened it, so MainActivity
 * implements the listener and forwards it to RazorpayBridge, where the shared
 * PaymentPlatform coroutine is waiting for it.
 *
 * It is also the one place the Google Play in-app update check runs from: once per foreground,
 * never per screen. [InAppUpdateManager] owns all the state, this activity only draws the two
 * prompts Play does not draw itself.
 */
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var updateDialog: AlertDialog? = null

    /** What the visible dialog is for, so a repeated prompt of the same kind never stacks. */
    private var shownPrompt: AppUpdatePrompt? = null

    /** The "downloading" toast is shown once per download, not once per progress callback. */
    private var downloadToastShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContext.set(this)
        readNotificationDestination(intent)

        // Re-attaching after a recreation is intentional and cheap: the manager keeps its
        // session state, so a rotation cannot trigger a second check or a second dialog.
        InAppUpdateManager.attach(this) { prompt -> renderUpdatePrompt(prompt) }

        setContent {
            App()
        }
    }

    /**
     * singleTask means a tap on a notification while the app is already running delivers here
     * rather than through onCreate, so both paths have to park the destination.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationDestination(intent)
    }

    /**
     * Parks where the tapped notification wants to go. It is read, not acted on: at cold start
     * there is no signed-in UI yet to navigate, so the shell picks it up once it is composed.
     */
    private fun readNotificationDestination(intent: Intent?) {
        if (intent == null) return

        // Two shapes arrive here. A notification this app drew carries our own key. One drawn by
        // the system — which is what happens to a server push while the app is backgrounded, and
        // the only path that exists once the Cloud Functions are deployed — carries the FCM data
        // payload flattened into the extras instead, so `type` and `conversationId` are read
        // straight off the intent.
        val raw = intent.getStringExtra(NotificationDestination.EXTRA_KEY)
        if (raw != null) {
            PendingDestination.set(NotificationDestination.fromRaw(raw))
            return
        }

        val data = buildMap {
            intent.getStringExtra("type")?.let { put("type", it) }
            intent.getStringExtra("conversationId")?.let { put("conversationId", it) }
        }
        if (data.isNotEmpty()) {
            PendingDestination.set(NotificationDestination.fromData(data))
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers cold start, returning from the Play update screen, and coming back from the
        // background with a download that finished while the app was away.
        InAppUpdateManager.onResume()
    }

    override fun onDestroy() {
        dismissUpdateDialog()
        InAppUpdateManager.detach(this)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GoogleAuthLauncher.handleActivityResult(requestCode, resultCode, data)
        ImagePicker.handleActivityResult(requestCode, resultCode, data)
        InAppUpdateManager.handleActivityResult(requestCode, resultCode)
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayBridge.handlePaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        RazorpayBridge.handlePaymentError(errorCode, response, paymentData)
    }

    // ---------------------------------------------------------------------------------
    // Update prompts
    // ---------------------------------------------------------------------------------

    private fun renderUpdatePrompt(prompt: AppUpdatePrompt) {
        if (isFinishing || isDestroyed) return
        if (prompt == shownPrompt) return

        when (prompt) {
            is AppUpdatePrompt.None -> {
                downloadToastShown = false
                dismissUpdateDialog()
            }

            is AppUpdatePrompt.Downloading -> {
                // The app stays fully usable during a flexible download; Play also posts its
                // own system notification, so a single toast is all the app should add.
                if (!downloadToastShown) {
                    downloadToastShown = true
                    Toast.makeText(this, R.string.update_downloading, Toast.LENGTH_SHORT).show()
                }
            }

            is AppUpdatePrompt.ReadyToInstall -> showReadyToInstallDialog()

            is AppUpdatePrompt.UpdateRequired -> showUpdateRequiredDialog(prompt.canUseInAppUpdate)
        }
        shownPrompt = prompt
    }

    private fun showReadyToInstallDialog() {
        dismissUpdateDialog()
        updateDialog = AlertDialog.Builder(this)
            .setTitle(R.string.update_ready_title)
            .setMessage(R.string.update_ready_message)
            .setCancelable(true)
            .setPositiveButton(R.string.update_ready_action) { _, _ ->
                InAppUpdateManager.completeFlexibleUpdate()
            }
            .setNegativeButton(R.string.update_later_action) { dialog, _ ->
                // The download stays on the device; Play installs it on the next natural restart.
                dialog.dismiss()
            }
            .setOnDismissListener { shownPrompt = null }
            .show()
    }

    /**
     * The wall for a mandatory update the user dismissed. Not cancelable and with no "Later" —
     * the only ways out are updating or leaving the app.
     */
    private fun showUpdateRequiredDialog(canUseInAppUpdate: Boolean) {
        dismissUpdateDialog()
        val message = if (canUseInAppUpdate) {
            R.string.update_required_message
        } else {
            R.string.update_required_store_message
        }
        updateDialog = AlertDialog.Builder(this)
            .setTitle(R.string.update_required_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.update_required_action) { _, _ ->
                shownPrompt = null
                if (canUseInAppUpdate) {
                    InAppUpdateManager.startImmediateUpdate()
                } else {
                    InAppUpdateManager.openPlayStoreListing()
                }
            }
            .setNegativeButton(R.string.update_exit_action) { _, _ -> finish() }
            .show()
    }

    private fun dismissUpdateDialog() {
        updateDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
        }
        updateDialog = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
