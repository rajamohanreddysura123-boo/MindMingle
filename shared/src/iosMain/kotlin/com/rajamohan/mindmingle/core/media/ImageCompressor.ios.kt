package com.rajamohan.mindmingle.core.media

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max

private const val TAG = "ImageCompressor"

@OptIn(ExperimentalForeignApi::class)
actual object ImageCompressor {

    actual suspend fun compress(bytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        if (bytes.size <= SKIP_BELOW_BYTES) return@withContext bytes

        try {
            val image = UIImage(data = bytes.toNSData()) ?: return@withContext bytes

            // UIImage carries the camera's orientation as metadata; drawing it into a context
            // resolves that into pixels, which is what keeps a portrait photo upright once the
            // metadata is gone.
            val scaled = image.scaledToFit(MAX_DIMENSION.toDouble()) ?: return@withContext bytes

            val jpeg = UIImageJPEGRepresentation(scaled, JPEG_QUALITY / 100.0)
                ?: return@withContext bytes
            val result = jpeg.toByteArray()

            if (result.size >= bytes.size) {
                Napier.d(tag = TAG) { "compression made it larger; keeping the original" }
                return@withContext bytes
            }

            Napier.d(tag = TAG) { "photo ${bytes.size / 1024}KB -> ${result.size / 1024}KB" }
            result
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "compression failed; uploading the original" }
            bytes
        }
    }

    private fun UIImage.scaledToFit(maxDimension: Double): UIImage? {
        val width = size.useContents { width }
        val height = size.useContents { height }
        if (width <= 0.0 || height <= 0.0) return null

        val longEdge = max(width, height)
        val ratio = if (longEdge > maxDimension) maxDimension / longEdge else 1.0
        val targetWidth = width * ratio
        val targetHeight = height * ratio

        // scale = 1.0: the context is in pixels, not points, so this is the real output size
        // rather than that times the device's density.
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
        drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) {
        out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return out
}
