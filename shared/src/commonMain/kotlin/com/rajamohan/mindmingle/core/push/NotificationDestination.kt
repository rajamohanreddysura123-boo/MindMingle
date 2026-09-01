package com.rajamohan.mindmingle.core.push

/**
 * Where a notification tap should land.
 *
 * Tapping used to open the app at whichever screen it was last on, which is the one place it is
 * certainly not about: a "someone liked you" banner that drops the reader into a half-finished chat
 * has wasted the tap. The value survives as a string in the launch intent's extras, so it has to be
 * small and stable, not a Compose route.
 */
sealed class NotificationDestination {

    /** Open the Likes tab. */
    data object Likes : NotificationDestination()

    /** Open the Chats tab, and the conversation itself when one is named. */
    data class Chat(val conversationId: String) : NotificationDestination()

    /** What goes in the intent extra. */
    val raw: String
        get() = when (this) {
            is Likes -> LIKES
            is Chat -> if (conversationId.isBlank()) CHATS else "$CHATS$SEPARATOR$conversationId"
        }

    companion object {
        const val EXTRA_KEY = "mindmingle_destination"

        private const val LIKES = "likes"
        private const val CHATS = "chats"
        private const val SEPARATOR = ":"

        /** Parses [raw] back, or null for anything unrecognised — an unknown value just opens the app. */
        fun fromRaw(raw: String?): NotificationDestination? {
            if (raw.isNullOrBlank()) return null
            val kind = raw.substringBefore(SEPARATOR)
            val argument = raw.substringAfter(SEPARATOR, "")
            return when (kind) {
                LIKES -> Likes
                CHATS -> Chat(argument)
                else -> null
            }
        }

        /** The same, from a Cloud Function's data payload — see functions/src/notifications.ts. */
        fun fromData(data: Map<String, String>): NotificationDestination? = when (data["type"]) {
            "like" -> Likes
            "chat", "match" -> Chat(data["conversationId"].orEmpty())
            else -> null
        }
    }
}

/**
 * The destination a notification tap asked for, waiting to be consumed once the UI is composed.
 *
 * A tap can arrive before there is anything to navigate — a cold start has to sign in and load
 * before any tab exists — so the intent is parked here and read once the signed-in shell is up.
 */
object PendingDestination {
    private var value: NotificationDestination? = null

    fun set(destination: NotificationDestination?) {
        if (destination != null) value = destination
    }

    /** Returns the pending destination and forgets it, so a rotation cannot re-navigate. */
    fun consume(): NotificationDestination? {
        val pending = value
        value = null
        return pending
    }
}
