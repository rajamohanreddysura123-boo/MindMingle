package com.rajamohan.mindmingle.data.remote.source

actual object GoogleAuthLauncher {
    actual fun launchGoogleSignIn(
        onSuccess: (email: String, name: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        IosGoogleAuthHost.launch { email, name, error ->
            when {
                error != null -> onError(error)
                email.isBlank() -> onError("Google Sign-In returned no account")
                else -> onSuccess(email, name)
            }
        }
    }
}
