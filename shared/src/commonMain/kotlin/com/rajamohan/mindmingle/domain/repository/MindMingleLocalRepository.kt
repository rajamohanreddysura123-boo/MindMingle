package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.User

interface MindMingleLocalRepository {
    suspend fun saveUserSession(user: User): Boolean
    suspend fun getUserSession(): User?
    suspend fun clearSession(): Boolean

    /** Last-applied Discover filters, so the deck opens where the user left it. */
    /** Newest like already announced on this device for [uid]; 0 when none ever was. */
    suspend fun getLastLikeAlertAt(uid: String): Long

    suspend fun saveLastLikeAlertAt(uid: String, millis: Long)

    /** Newest incoming message already announced on this device for [uid], in seconds; 0 when none. */
    suspend fun getLastMessageAlertAt(uid: String): Long

    suspend fun saveLastMessageAlertAt(uid: String, seconds: Long)

    /** Newest location refresh *attempt* (success or failure) for [uid]; 0 when never tried. */
    suspend fun getLastLocationAttemptAt(uid: String): Long

    suspend fun saveLastLocationAttemptAt(uid: String, millis: Long)

    suspend fun saveDiscoverFilters(criteria: DiscoverFilterCriteria): Boolean

    /** Null when the user has never applied a filter on this device. */
    suspend fun getDiscoverFilters(): DiscoverFilterCriteria?
}