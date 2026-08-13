package com.rajamohan.mindmingle.core.push

actual object PushPlatform {

    actual val kind: PushPlatformKind = PushPlatformKind.DESKTOP

    actual val isSupported: Boolean = false

    actual suspend fun requestPermission(): Boolean = false

    actual suspend fun currentToken(): String? = null

    actual fun setTokenRefreshListener(listener: (String) -> Unit) = Unit
}
