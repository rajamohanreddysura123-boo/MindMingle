package com.rajamohan.mindmingle.core.location

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.posix.memcpy
import kotlin.coroutines.resume

private const val TAG = "LocationService"

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
    }
    return bytes
}

internal actual suspend fun fetchLocationJson(url: String): String? = suspendCancellableCoroutine { continuation ->
    val request = NSMutableURLRequest(uRL = NSURL(string = url))
    request.setHTTPMethod("GET")

    val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
        if (error != null) {
            // Every provider here is free-tier and rate-limited per IP, so a failure is an
            // expected outcome, not a bug — the caller already has more providers and a cooldown
            // (RefreshMyLocationUseCase) to fall back on.
            Napier.d(tag = TAG) { "fetch failed for $url: ${(error as NSError).localizedDescription}" }
        }
        val json = data?.toKotlinByteArray()?.decodeToString()
        continuation.resume(json)
    }
    task.resume()
    continuation.invokeOnCancellation { task.cancel() }
}
