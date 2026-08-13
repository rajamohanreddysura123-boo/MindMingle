package com.rajamohan.mindmingle.data.remote.source

expect object GoogleAuthLauncher {
    fun launchGoogleSignIn(
        onSuccess: (email: String, name: String) -> Unit,
        onError: (message: String) -> Unit
    )
}
