package com.rajamohan.mindmingle.data.remote.source

actual object GoogleAuthLauncher {
    actual fun launchGoogleSignIn(
        onSuccess: (email: String, name: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        onError("Google Sign-In is only supported on Android and iOS mobile devices")
    }
}
