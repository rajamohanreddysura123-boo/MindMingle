package com.rajamohan.mindmingle.core.push

/**
 * A notification raised by this device about something it just read from Firestore, as opposed to
 * one pushed to it by a server.
 *
 * The distinction matters and is not a detail: FCM will only deliver to a device on behalf of a
 * trusted sender (a service account or server key), and no such credential can live inside a
 * shipped app — anyone could extract it and push to every user. So a client cannot notify another
 * client. What it can do is watch its own mailbox in Firestore and raise the notification itself,
 * which is what this is.
 *
 * The limit that follows: this fires while the app process is alive (foreground, or backgrounded
 * but not yet killed). A fully closed app hears nothing until it is next opened, and closing that
 * gap needs the Cloud Functions in `functions/src/notifications.ts` to actually be deployed.
 */
expect object LocalNotifier {

    /** False on desktop, which has no notification surface wired up. */
    val isSupported: Boolean

    /**
     * Posts a notification now. [id] replaces an earlier notification with the same value, which
     * is how a second like from the same person updates one row instead of stacking a duplicate.
     * [destination] is where a tap should land; null opens the app wherever it was.
     */
    fun show(id: Int, title: String, body: String, destination: NotificationDestination? = null)
}
