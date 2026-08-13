package com.rajamohan.mindmingle.core.media

/** Lets the user pick up to [maxCount] photos from their device. Returns raw image bytes, empty list if cancelled. */
expect object ImagePicker {
    suspend fun pickImages(maxCount: Int): List<ByteArray>
}
