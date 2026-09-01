package com.rajamohan.mindmingle

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rajamohan.mindmingle.core.push.AndroidPushBridge
import com.rajamohan.mindmingle.core.push.LocalNotifier
import com.rajamohan.mindmingle.core.push.NotificationDestination

/**
 * The app's end of Firebase Cloud Messaging.
 *
 * It has to live in this module rather than in `shared`: a manifest may only name classes from the
 * application it belongs to, and the shared module has no manifest of its own. That is why
 * [AndroidPushBridge] exists — the shared push layer cannot see this class, so this class hands it
 * what it needs.
 *
 * Until this existed the bridge had no caller at all, which meant FCM could rotate a device's token
 * (it does on reinstall, on restore to a new phone, and when app data is cleared) and the app would
 * never hear about it. The stale row under `users/{uid}/devices` then received nothing, and the
 * server only learned it was dead the next time a send failed against it.
 */
class MindMingleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // The shared PushRepository is listening; it re-registers the new token under the signed-in
        // user. No-op when nobody is signed in, which is correct — there is no account to file it
        // against, and the next sign-in registers whatever the current token is anyway.
        AndroidPushBridge.onNewToken(token)
    }

    /**
     * Only fires while the app is in the foreground, or for a data-only message. A message carrying
     * a `notification` block is drawn by the system itself when the app is backgrounded, and this
     * is never called for it.
     *
     * Drawing it through [LocalNotifier] rather than letting it pass keeps one code path for how a
     * notification looks and where a tap goes, whether it came from a server or from the in-app
     * watchers.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification
        val title = notification?.title ?: message.data["title"].orEmpty()
        val body = notification?.body ?: message.data["body"].orEmpty()
        if (title.isBlank() && body.isBlank()) return

        LocalNotifier.show(
            // Keyed on whatever the payload is about, so a second message in the same conversation
            // replaces the first row instead of stacking. Falls back to the message id.
            id = (message.data["conversationId"] ?: message.data["fromUid"] ?: message.messageId).hashCode(),
            title = title.ifBlank { "MindMingle" },
            body = body,
            destination = NotificationDestination.fromData(message.data)
        )
    }
}
