package com.rajamohan.mindmingle.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val TAG = "ImageCompressor"

actual object ImageCompressor {

    actual suspend fun compress(bytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        if (bytes.size <= SKIP_BELOW_BYTES) return@withContext bytes

        try {
            // Two passes. The first reads only the header, so the dimensions are known without
            // ever holding a 4000x3000 bitmap in memory — decoding one of those outright is how
            // a photo picker OOMs on a mid-range phone.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            val longEdge = max(bounds.outWidth, bounds.outHeight)
            if (longEdge <= 0) return@withContext bytes

            val options = BitmapFactory.Options().apply {
                // Powers of two only, and it rounds down, so this lands at or above the target
                // and the exact scale is finished off below.
                inSampleSize = sampleSizeFor(longEdge)
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return@withContext bytes

            val scaled = scaleToFit(decoded)
            val upright = applyExifRotation(scaled, bytes)

            val out = ByteArrayOutputStream()
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            val result = out.toByteArray()

            if (upright !== decoded) decoded.recycle()

            // A photo that grows is a photo that was already better encoded than this — a small
            // PNG screenshot, typically. Keep whichever is smaller.
            if (result.size >= bytes.size) {
                Napier.d(tag = TAG) { "compression made it larger; keeping the original" }
                return@withContext bytes
            }

            Napier.d(tag = TAG) { "photo ${bytes.size / 1024}KB -> ${result.size / 1024}KB" }
            result
        } catch (e: Throwable) {
            // OutOfMemoryError included: an uncompressed upload beats a crash in the picker.
            Napier.w(throwable = e, tag = TAG) { "compression failed; uploading the original" }
            bytes
        }
    }

    private fun sampleSizeFor(longEdge: Int): Int {
        var sample = 1
        while (longEdge / (sample * 2) >= MAX_DIMENSION) sample *= 2
        return sample
    }

    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= MAX_DIMENSION) return bitmap

        val ratio = MAX_DIMENSION.toFloat() / longEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            // Filtered: nearest-neighbour on a downscale of this size produces visible aliasing
            // on exactly the thing these photos are of — faces.
            true
        )
    }

    /**
     * Phone cameras write the sensor's orientation into EXIF rather than rotating the pixels, and
     * re-encoding drops that tag — so a photo taken in portrait would upload sideways. This bakes
     * the rotation into the pixels before the tag is lost.
     */
    private fun applyExifRotation(bitmap: Bitmap, original: ByteArray): Bitmap {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(original))
            val degrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Napier.d(throwable = e, tag = TAG) { "no usable EXIF orientation" }
            bitmap
        }
    }
}
