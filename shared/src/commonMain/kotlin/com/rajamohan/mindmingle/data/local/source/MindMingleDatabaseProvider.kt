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