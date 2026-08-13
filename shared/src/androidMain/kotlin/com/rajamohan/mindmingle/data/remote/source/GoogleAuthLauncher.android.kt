package com.rajamohan.mindmingle.data.remote.source

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.rajamohan.mindmingle.core.AppContext

actual object GoogleAuthLauncher {
    private const val WEB_CLIENT_ID = "532395068238-pi67igb7k1ht578l3i5puncvmn8kl2ab.apps.googleusercontent.com"
    const val RC_SIGN_IN = 9001

    private var pendingOnSuccess: ((email: String, name: String) -> Unit)? = null
    private var pendingOnError: ((message: String) -> Unit)? = null

    actual fun launchGoogleSignIn(
        onSuccess: (email: String, name: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val context = AppContext.get()
        val activity = context as? Activity
        if (activity == null) {
            onError("Android Activity context is required for Google Sign-In")
            return
        }

        pendingOnSuccess = onSuccess
        pendingOnError = onError

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        // Sign out first to ensure the device account chooser always shows all accounts
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != RC_SIGN_IN) return false

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            pendingOnSuccess?.invoke(
                                account.email ?: "",
                                account.displayName ?: account.email ?: "User"
                            )
                        } else {
                            pendingOnError?.invoke(
                                authTask.exception?.localizedMessage ?: "Firebase Sign-In failed"
                            )
                        }
                        clearCallbacks()
                    }
            } else {
                pendingOnError?.invoke("Failed to retrieve Google ID Token")
                clearCallbacks()
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) { // 12501 is user canceled
                pendingOnError?.invoke(e.localizedMessage ?: "Google Sign-In failed (${e.statusCode})")
            }
            clearCallbacks()
        }
        return true
    }

    private fun clearCallbacks() {
        pendingOnSuccess = null
        pendingOnError = null
    }
}
