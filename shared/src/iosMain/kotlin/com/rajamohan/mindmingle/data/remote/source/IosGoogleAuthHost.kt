package com.rajamohan.mindmingle.data.remote.source

/**
 * The Swift side of Google sign-in on iOS.
 *
 * Same shape as [com.rajamohan.mindmingle.core.ads.IosAdHost] and IosPaymentHost, and for the same
 * reason: GoogleSignIn is an Xcode dependency, so Gradle cannot put it on Kotlin/Native's compile
 * path without dragging CocoaPods into the project. `iosApp` registers the closure at launch (see
 * iosApp/iosApp/GoogleAuthBridge.swift) and the shared code calls it.
 *
 * Until that happens the bridge is not installed and sign-in reports that plainly, which is what
 * it did before this existed — except it now says so through one path instead of a hardcoded
 * error string.
 */
object IosGoogleAuthHost {

    var isBridgeInstalled: Boolean = false
        private set

    private var signInLauncher: (((String, String, String?) -> Unit) -> Unit)? = null

    /**
     * @param launchSignIn presents Google's sheet and reports back exactly once with
     *   `(email, name, error)` — a non-null error meaning it failed or was cancelled.
     */
    fun install(launchSignIn: ((String, String, String?) -> Unit) -> Unit) {
        signInLauncher = launchSignIn
        isBridgeInstalled = true
    }

    internal fun launch(onResult: (email: String, name: String, error: String?) -> Unit) {
        val launcher = signInLauncher
        if (launcher == null) {
            onResult("", "", "Google Sign-In is not available on this build")
            return
        }
        launcher(onResult)
    }
}
