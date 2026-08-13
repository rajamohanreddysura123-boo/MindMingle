package com.rajamohan.mindmingle.core.push

enum class PushPlatformKind {
    ANDROID,
    IOS,
    DESKTOP
}

/**
 * The one push surface the shared code knows about. Firebase Cloud Messaging ships separate
 * SDKs per platform — Android links `firebase-messaging` directly, iOS goes through the Swift
 * bridge in iosApp, and desktop has no push at all — so the registration flow talks to this
 * expect object and each platform binds its own SDK behind it.
 */
expect object PushPlatform {

    val kind: PushPlatformKind

    val isSupported: Boolean

    /** Android 13+ and iOS both gate notifications behind a runtime prompt. */
    suspend fun requestPermission(): Boolean

    /** The current FCM registration token, or null when push is unavailable or denied. */
    suspend fun currentToken(): String?

    /** Fires whenever FCM rotates the token; the app re-registers the new one. */
    fun setTokenRefreshListener(listener: (String) -> Unit)
}
