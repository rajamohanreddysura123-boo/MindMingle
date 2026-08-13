package com.rajamohan.mindmingle.data.remote.source

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rajamohan.mindmingle.core.AppContext
import java.util.concurrent.TimeUnit

actual object PhoneOtpSender {
    actual fun sendOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val context = AppContext.get()
        val activity = context as? Activity
        if (activity == null) {
            onError("Android Activity context is required for Firebase Phone Auth")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onCodeSent("AUTO_VERIFIED")
                        } else {
                            onError(task.exception?.localizedMessage ?: "Auto verification failed")
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.localizedMessage ?: "SMS verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    actual fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (verificationId == "AUTO_VERIFIED") {
            onSuccess()
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.localizedMessage ?: "Invalid OTP entered")
                }
            }
    }
}
