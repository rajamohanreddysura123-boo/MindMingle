package com.rajamohan.mindmingle.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.core.push.LocalNotifier
import com.rajamohan.mindmingle.core.push.NotificationDestination
import com.rajamohan.mindmingle.core.push.OpenConversation
import com.rajamohan.mindmingle.domain.model.IncomingLike
import com.rajamohan.mindmingle.domain.model.MessageAlert
import com.rajamohan.mindmingle.domain.model.NotificationCategory
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.usecase.GetNotificationPrefsUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveIncomingLikeAlertsUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveMessageAlertsUseCase
import com.rajamohan.mindmingle.domain.usecase.RegisterPushDeviceUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Announces likes and incoming messages from the recipient's own device.
 *
 * ## Why the client does this at all
 * The Cloud Functions that would push these (`functions/src/notifications.ts`) are not part of how
 * this app runs, so the arrival has to be noticed by the person it is for. Their app is already
 * signed in and already streaming their like mailbox and their conversation list; this watches the
 * same two streams and raises a local notification for anything new.
 *
 * ## What it cannot do
 * Only a trusted sender may push through FCM, and no such credential can ship inside an app, so a
 * sender's phone cannot make another phone buzz. This fires while the recipient's app is alive —
 * foreground, or backgrounded and not yet killed. A fully closed app shows nothing until it is
 * next opened, and nothing is lost either way: both a like and a message are Firestore documents,
 * and the Likes and Chats tabs show them regardless.
 *
 * ## Announcing each thing once
 * A high-water mark per account per kind lives on the device. The first run adopts the mark
 * silently — otherwise signing in would fire one notification per like and per conversation ever
 * received.
 *
 * Anonymous chat is deliberately absent. Those rooms are built so that nothing survives them, and
 * a notification saying a message exists would leak exactly what the feature promises not to keep.
 */
internal class AlertsViewModel(
    private val observeIncomingLikeAlertsUseCase: ObserveIncomingLikeAlertsUseCase,
    private val observeMessageAlertsUseCase: ObserveMessageAlertsUseCase,
    private val getNotificationPrefsUseCase: GetNotificationPrefsUseCase,
    private val registerPushDeviceUseCase: RegisterPushDeviceUseCase,
    private val mindMingleLocalRepository: MindMingleLocalRepository
) : ViewModel() {

    private companion object {
        const val TAG = "Alerts"

        /** Beyond this many at once, one summary line reads better than a burst of banners. */
        const val MAX_ALERTS_PER_BATCH = 3
    }

    private var likeJob: Job? = null
    private var messageJob: Job? = null

    fun start(uid: String) {
        if (uid.isBlank() || !LocalNotifier.isSupported) return

        // Two things at once, and both are needed. It asks for the OS notification permission,
        // without which nothing below can be shown at all on Android 13+ or iOS; and it stores
        // this device's FCM token under the user, which is what the notification Cloud Functions
        // send to. Nothing called this before, so `users/{uid}/devices` was empty for everyone —
        // every server-side push had zero addresses to deliver to.
        viewModelScope.launch { registerPushDeviceUseCase(uid) }

        watchLikes(uid)
        watchMessages(uid)
    }

    private fun watchLikes(uid: String) {
        likeJob?.cancel()
        likeJob = viewModelScope.launch {
            var lastAlertAt = mindMingleLocalRepository.getLastLikeAlertAt(uid)

            observeIncomingLikeAlertsUseCase(uid).collect { likes ->
                if (likes.isEmpty()) return@collect

                val newest = likes.maxOf { it.createdAt }

                // First sight of this mailbox on this device: adopt the mark silently.
                if (lastAlertAt == 0L) {
                    lastAlertAt = newest
                    mindMingleLocalRepository.saveLastLikeAlertAt(uid, newest)
                    return@collect
                }

                val fresh = likes.filter { it.createdAt > lastAlertAt }.sortedBy { it.createdAt }
                if (fresh.isEmpty()) return@collect

                // The mark moves before anything is shown: a notification that fails to post is
                // still not worth repeating on the next snapshot, and every snapshot of this
                // stream carries the whole mailbox.
                lastAlertAt = newest
                mindMingleLocalRepository.saveLastLikeAlertAt(uid, newest)

                if (!isEnabled(uid, NotificationCategory.LIKES)) return@collect
                notifyLikes(fresh)
            }
        }
    }

    /**
     * Messages are watched through the conversation rows rather than the messages themselves:
     * `lastMessageFrom` and `lastMessageAt` change on every send, and one listener over the
     * conversation list covers every chat at once — a listener per conversation would multiply
     * reads by however many people someone talks to.
     */
    private fun watchMessages(uid: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            var lastAlertAt = mindMingleLocalRepository.getLastMessageAlertAt(uid)

            observeMessageAlertsUseCase(uid).collect { alerts ->
                if (alerts.isEmpty()) return@collect

                val newest = alerts.maxOf { it.sentAtSeconds }
                if (newest <= 0L) return@collect

                if (lastAlertAt == 0L) {
                    lastAlertAt = newest
                    mindMingleLocalRepository.saveLastMessageAlertAt(uid, newest)
                    return@collect
                }

                val fresh = alerts
                    .filter { it.sentAtSeconds > lastAlertAt }
                    // Not the chat they are reading right now.
                    .filter { it.conversationId != OpenConversation.conversationId }
                    .sortedBy { it.sentAtSeconds }

                lastAlertAt = newest
                mindMingleLocalRepository.saveLastMessageAlertAt(uid, newest)

                if (fresh.isEmpty()) return@collect
                if (!isEnabled(uid, NotificationCategory.MESSAGES)) return@collect
                notifyMessages(fresh)
            }
        }
    }

    private suspend fun isEnabled(uid: String, category: NotificationCategory): Boolean {
        val enabled = getNotificationPrefsUseCase(uid).isEnabled(category)
        if (!enabled) Napier.d(tag = TAG) { "$category alerts muted by preference" }
        return enabled
    }

    /**
     * Several can land in one snapshot after a spell offline. A handful are announced individually
     * and anything beyond that collapses into one line — a burst of notifications reads as a
     * malfunction, and the tab itself is where the full list belongs.
     */
    private fun notifyLikes(fresh: List<IncomingLike>) {
        if (fresh.size > MAX_ALERTS_PER_BATCH) {
            LocalNotifier.show(
                id = fresh.last().createdAt.hashCode(),
                title = "${fresh.size} new likes",
                body = "Open Likes to see who is interested in your profile.",
                destination = NotificationDestination.Likes
            )
            return
        }

        fresh.forEach { like ->
            LocalNotifier.show(
                // Keyed on the liker so a second like from the same person replaces the first row
                // rather than stacking a duplicate.
                id = like.fromUid.hashCode(),
                title = "${like.name.ifBlank { "Someone" }} liked you",
                body = "Open Likes to like them back and start chatting.",
                destination = NotificationDestination.Likes
            )
        }
    }

    /**
     * The body never quotes the message. A conversation row stores who wrote and when, never the
     * text — messages disappear once seen, and a notification carrying a copy would outlive the
     * message it copied.
     */
    private fun notifyMessages(fresh: List<MessageAlert>) {
        if (fresh.size > MAX_ALERTS_PER_BATCH) {
            LocalNotifier.show(
                id = fresh.last().sentAtSeconds.hashCode(),
                title = "${fresh.size} new messages",
                body = "Open Chats to read them.",
                // No single conversation to open when several are waiting — the tab is the answer.
                destination = NotificationDestination.Chat("")
            )
            return
        }

        fresh.forEach { alert ->
            LocalNotifier.show(
                // Keyed on the conversation: a second message in the same chat replaces the row.
                id = alert.conversationId.hashCode(),
                title = alert.fromName.ifBlank { "New message" },
                body = "Sent you a message. Open Chats to read it.",
                destination = NotificationDestination.Chat(alert.conversationId)
            )
        }
    }
}
