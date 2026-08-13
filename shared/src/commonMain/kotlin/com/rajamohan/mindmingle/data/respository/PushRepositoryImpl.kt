package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.core.push.PushPlatform
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.NotificationPrefs
import com.rajamohan.mindmingle.domain.model.toDomain
import com.rajamohan.mindmingle.domain.model.toDto
import com.rajamohan.mindmingle.domain.repository.PushRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class PushRepositoryImpl(
    private val mindMingleFirebaseProvider: MindMingleFirebaseProvider
) : PushRepository {

    private companion object {
        const val TAG = "PushRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun registerCurrentDevice(uid: String): Result<Unit> {
        if (!PushPlatform.isSupported || uid.isBlank()) return Result.success(Unit)

        return try {
            PushPlatform.requestPermission()

            val token = PushPlatform.currentToken()
            if (token.isNullOrBlank()) {
                Napier.w(tag = TAG) { "no FCM token available yet" }
                return Result.success(Unit)
            }

            mindMingleFirebaseProvider.registerDeviceToken(
                uid = uid,
                token = token,
                platform = PushPlatform.kind.name.lowercase()
            )

            // FCM rotates tokens on reinstall and restore; the stale row is dropped server-side
            // when a send fails, so re-registering here is all the client owes.
            PushPlatform.setTokenRefreshListener { refreshed ->
                scope.launch {
                    runCatching {
                        mindMingleFirebaseProvider.registerDeviceToken(
                            uid = uid,
                            token = refreshed,
                            platform = PushPlatform.kind.name.lowercase()
                        )
                    }.onFailure { error ->
                        Napier.w(throwable = error, tag = TAG) { "token refresh registration failed" }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "registerCurrentDevice failed" }
            Result.failure(e)
        }
    }

    override suspend fun unregisterCurrentDevice(uid: String): Result<Unit> {
        if (!PushPlatform.isSupported || uid.isBlank()) return Result.success(Unit)

        return try {
            val token = PushPlatform.currentToken() ?: return Result.success(Unit)
            mindMingleFirebaseProvider.unregisterDeviceToken(uid, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "unregisterCurrentDevice failed" }
            Result.failure(e)
        }
    }

    override suspend fun getPrefs(uid: String): NotificationPrefs {
        return try {
            mindMingleFirebaseProvider.getNotificationPrefs(uid)?.toDomain() ?: NotificationPrefs()
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "getPrefs failed" }
            NotificationPrefs()
        }
    }

    override suspend fun savePrefs(uid: String, prefs: NotificationPrefs): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.saveNotificationPrefs(uid, prefs.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "savePrefs failed" }
            Result.failure(e)
        }
    }
}
