package com.rajamohan.mindmingle.core.push

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object PushPlatform {

    actual val kind: PushPlatformKind = PushPlatformKind.IOS

    actual val isSupported: Boolean
        get() = IosPushHost.isBridgeInstalled

    actual suspend fun requestPermission(): Boolean {
        if (!IosPushHost.isBridgeInstalled) return false

        return suspendCancellableCoroutine { continuation ->
            IosPushHost.requestPermission { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }
    }

    actual suspend fun currentToken(): String? {
        if (!IosPushHost.isBridgeInstalled) return null

        return suspendCancellableCoroutine { continuation ->
            IosPushHost.fetchToken { token ->
                if (continuation.isActive) continuation.resume(token)
            }
        }
    }

    actual fun setTokenRefreshListener(listener: (String) -> Unit) {
        IosPushHost.setRefreshListener(listener)
    }
}
