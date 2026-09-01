package com.rajamohan.mindmingle.core.update

/**
 * The only four things the app itself ever has to draw for an update.
 *
 * Everything else — "an update is available, install it?", the download UI, the immediate
 * update's blocking full-screen progress — is drawn by Google Play itself, which is why there
 * is no custom "New version available / Update Now / Later" dialog anywhere in this codebase.
 * Play's own sheet already asks exactly that, in the user's language, and is the officially
 * supported experience.
 *
 * What Play does *not* draw:
 *  - [ReadyToInstall]: after a flexible download finishes, the restart prompt is the app's job.
 *  - [UpdateRequired]: the wall shown when the user backed out of a mandatory update.
 */
sealed interface AppUpdatePrompt {

    /** Nothing to show; dismiss whatever is on screen. */
    data object None : AppUpdatePrompt

    /** A flexible update is downloading in the background. The app stays fully usable. */
    data class Downloading(val percent: Int) : AppUpdatePrompt

    /** Flexible download finished. The app must ask the user before restarting into it. */
    data object ReadyToInstall : AppUpdatePrompt

    /**
     * The installed build is below the supported floor and the user dismissed the Play flow.
     *
     * @param canUseInAppUpdate true when Play can still run an immediate update here (the
     *   button retries it). False when Play has no update to serve on this device — an
     *   unmanaged install, a stale Play cache, or a rollout that has not reached this user —
     *   in which case the only honest action left is to open the store listing.
     */
    data class UpdateRequired(val canUseInAppUpdate: Boolean) : AppUpdatePrompt
}
