package com.rajamohan.mindmingle.core.push

/**
 * Which conversation is on screen right now, or blank for none.
 *
 * The message watcher skips notifying about the chat the user is already reading — a banner for a
 * message that just appeared in front of them is noise. It is a plain object rather than shared
 * state in a ViewModel because the two live in different scopes: the watcher is mounted once for
 * the whole signed-in session, the chat screen comes and goes.
 */
object OpenConversation {
    var conversationId: String = ""
        private set

    fun opened(id: String) {
        conversationId = id
    }

    fun closed() {
        conversationId = ""
    }
}
