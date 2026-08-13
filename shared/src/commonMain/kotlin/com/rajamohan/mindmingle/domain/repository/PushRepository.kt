package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.NotificationPrefs

interface PushRepository {

    /**
     * Asks for permission if needed, fetches the FCM token and stores it under the user, then
     * keeps listening for token rotation. Safe to call on every sign-in.
     */
    suspend fun registerCurrentDevice(uid: String): Result<Unit>

    /** Drops this device's token so a signed-out phone stops receiving that account's pushes. */
    suspend fun unregisterCurrentDevice(uid: String): Result<Unit>

    suspend fun getPrefs(uid: String): NotificationPrefs

    suspend fun savePrefs(uid: String, prefs: NotificationPrefs): Result<Unit>
}
