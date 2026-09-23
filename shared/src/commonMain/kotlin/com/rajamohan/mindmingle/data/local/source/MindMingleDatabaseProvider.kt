package com.rajamohan.mindmingle.data.local.source

import com.rajamohan.mindmingle.core.LMPreferences
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.User
import kotlinx.serialization.json.Json

internal class MindMingleDatabaseProvider(
    private val preferences: LMPreferences
) {
    private companion object {
        const val DISCOVER_FILTERS_KEY = "discover_filters"
        const val LAST_LIKE_ALERT_PREFIX = "last_like_alert_"
        const val LAST_MESSAGE_ALERT_PREFIX = "last_message_alert_"
        const val LAST_LOCATION_ATTEMPT_PREFIX = "last_location_attempt_"
    }

    /** Tolerates criteria fields added in a later release rather than throwing away the whole stored set. */
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveUserSession(user: User) {
        preferences.update("user_uid", user.uid, String::class)
        preferences.update("user_phone", user.phoneNumber, String::class)
    }

    suspend fun getUserSession(): User? {
        val uid = preferences.get("user_uid", "", String::class)
        val phone = preferences.get("user_phone", "", String::class)
        return if (uid.isNotEmpty()) User(uid = uid, phoneNumber = phone) else null
    }

    suspend fun clearSession() {
        preferences.delete("user_uid")
        preferences.delete("user_phone")
        // Filters are personal — the next account to sign in on this device must not inherit them.
        preferences.delete(DISCOVER_FILTERS_KEY)
    }

    /**
     * High-water marks for the notification watchers, per account: the newest like and the newest
     * incoming message this device has already announced. Kept per uid because two people signing
     * into the same phone must not inherit each other's marks, and stored as text because
     * [LMPreferences] has no Long type.
     */
    suspend fun saveLastLikeAlertAt(uid: String, millis: Long) {
        preferences.update("$LAST_LIKE_ALERT_PREFIX$uid", millis.toString(), String::class)
    }

    /** 0 means this device has never announced a like for [uid] — the backlog is not news. */
    suspend fun getLastLikeAlertAt(uid: String): Long {
        return preferences.get("$LAST_LIKE_ALERT_PREFIX$uid", "", String::class).toLongOrNull() ?: 0L
    }

    suspend fun saveLastMessageAlertAt(uid: String, seconds: Long) {
        preferences.update("$LAST_MESSAGE_ALERT_PREFIX$uid", seconds.toString(), String::class)
    }

    /** In seconds, matching the conversation row's own timestamp. */
    suspend fun getLastMessageAlertAt(uid: String): Long {
        return preferences.get("$LAST_MESSAGE_ALERT_PREFIX$uid", "", String::class).toLongOrNull() ?: 0L
    }

    /**
     * When a location refresh was last *attempted* — success or failure — as opposed to
     * `locationUpdatedAt` on the profile, which only moves on success. This is what stops a
     * run of failed lookups (a rate-limited IP-geolocation provider, most often) from being
     * retried on every single Discover open: see RefreshMyLocationUseCase.
     */
    suspend fun saveLastLocationAttemptAt(uid: String, millis: Long) {
        preferences.update("$LAST_LOCATION_ATTEMPT_PREFIX$uid", millis.toString(), String::class)
    }

    suspend fun getLastLocationAttemptAt(uid: String): Long {
        return preferences.get("$LAST_LOCATION_ATTEMPT_PREFIX$uid", "", String::class).toLongOrNull() ?: 0L
    }

    suspend fun saveDiscoverFilters(criteria: DiscoverFilterCriteria) {
        preferences.update(DISCOVER_FILTERS_KEY, json.encodeToString(criteria), String::class)
    }

    /** Null when nothing was ever applied on this device — that's the signal to seed from the user's own preferences. */
    suspend fun getDiscoverFilters(): DiscoverFilterCriteria? {
        val stored = preferences.get(DISCOVER_FILTERS_KEY, "", String::class)
        if (stored.isEmpty()) return null
        return json.decodeFromString<DiscoverFilterCriteria>(stored)
    }
}