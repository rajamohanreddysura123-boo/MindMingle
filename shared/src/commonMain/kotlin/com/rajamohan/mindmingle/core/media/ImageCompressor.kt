package com.rajamohan.mindmingle.core.media

/**
 * Shrinks a picked photo before it is uploaded.
 *
 * A photo straight off a modern phone is 3-8 MB and 4000px wide. It is never displayed anywhere
 * near that: the largest surface in the app is the profile detail hero, a few hundred points on a
 * phone and under a thousand on a desktop. So every one of those megabytes is paid for three
 * times — the uploader's data, the storage bill, and every viewer who loads the deck.
 *
 * "Without losing quality" is the whole constraint here, and it is achievable because the original
 * is so far above what is shown. Downscaling to [MAX_DIMENSION] on the long edge and re-encoding at
 * [JPEG_QUALITY] is indistinguishable at display size while typically cutting the file by 90%.
 *
 * What it deliberately does not do: touch a photo that is already small. Re-encoding a JPEG always
 * loses a little, so an image already within budget is passed through untouched rather than put
 * through a second generation of loss for no gain.
 */
expect object ImageCompressor {

    /**
     * Returns the compressed bytes, or the original when it is already small enough or cannot be
     * decoded. Never throws: a photo that fails to compress is still a photo worth uploading.
     */
    suspend fun compress(bytes: ByteArray): ByteArray
}

/**
 * Long-edge limit in pixels.
 *
 * 1440 covers the largest surface the app has — a full-bleed hero on a high-density phone — with
 * enough headroom that the image is still sharp on a desktop window twice that size, because it is
 * downscaled on display rather than stretched.
 */
internal const val MAX_DIMENSION = 1440

/**
 * JPEG quality. 85 is the usual place to stop: above it the file grows fast for differences nobody
 * sees, below it flat areas like skin and sky start to band.
 */
internal const val JPEG_QUALITY = 85

/** Below this, compressing would cost a generation of quality to save almost nothing. */
internal const val SKIP_BELOW_BYTES = 300 * 1024
