package com.rajamohan.mindmingle.core.push

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.rajamohan.mindmingle.core.AppContext
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "PushPlatform"
private const val PERMISSION_REQUEST_CODE = 4711

const val MINDMINGLE_NOTIFICATION_CHANNEL_ID = "mindmingle_default"

actual object PushPlatform {

    private var refreshListener: ((String) -> Unit)? = null

    actual val kind: PushPlatformKind = PushPlatformKind.ANDROID

    actual val isSupported: Boolean = true

    actual suspend fun requestPermission(): Boolean {
        val context = AppContext.get() as? Context ?: return false
        ensureChannel(context)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return true

        val activity = context as? Activity ?: return false
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            PERMISSION_REQUEST_CODE
        )

        // The dialog is answered asynchronously; registration proceeds either way and the token
        // simply produces nothing visible until the user allows it.
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (continuation.isActive) continuation.resume(token)
            }
            .addOnFailureListener { error ->
                Napier.w(throwable = error, tag = TAG) { "FCM token fetch failed" }
                if (continuation.isActive) continuation.resume(null)
            }
    }

    actual fun setTokenRefreshListener(listener: (String) -> Unit) {
        refreshListener = listener
    }

    internal fun onTokenRefreshed(token: String) {
        refreshListener?.invoke(token)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(MINDMINGLE_NOTIFICATION_CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                MINDMINGLE_NOTIFICATION_CHANNEL_ID,
                "MindMingle notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Messages, likes, matches and billing updates"
            }
        )
    }
}

/**
 * Bridge for the app module's FirebaseMessagingService — that class has to live in the app
 * module so it can be declared in its manifest, and it hands new tokens back here.
 */
object AndroidPushBridge {
    fun onNewToken(token: String) {
        PushPlatform.onTokenRefreshed(token)
    }
}
