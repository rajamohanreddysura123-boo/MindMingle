package com.rajamohan.mindmingle.core.push

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rajamohan.mindmingle.core.AppContext
import io.github.aakira.napier.Napier

private const val TAG = "LocalNotifier"

actual object LocalNotifier {

    actual val isSupported: Boolean = true

    actual fun show(id: Int, title: String, body: String, destination: NotificationDestination?) {
        val context = AppContext.get() as? Context ?: return

        try {
            // The channel is created by PushPlatform.requestPermission(); creating it is
            // idempotent, so doing it again here covers the case where nothing has asked for
            // permission yet on this launch. Without the channel, Android silently drops the post.
            PushPlatform.ensureNotificationChannel(context)

            val builder = NotificationCompat.Builder(context, MINDMINGLE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Tapping opens the app the same way the launcher does. The shared module cannot name
            // MainActivity — it lives in the app module — so the launch intent is looked up.
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                destination?.let { intent.putExtra(NotificationDestination.EXTRA_KEY, it.raw) }
                builder.setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            // Throws on Android 13+ when POST_NOTIFICATIONS was refused, which is a normal
            // outcome and not something to surface to the user a second time.
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "local notification not shown" }
        }
    }
}
