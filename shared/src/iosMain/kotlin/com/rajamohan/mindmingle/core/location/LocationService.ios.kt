package com.rajamohan.mindmingle.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.posix.memcpy
import kotlin.coroutines.resume

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

    val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, _ ->
        val json = data?.toKotlinByteArray()?.decodeToString()
        continuation.resume(json)
    }
    task.resume()
    continuation.invokeOnCancellation { task.cancel() }
}
