package com.rajamohan.mindmingle.core.media

/** Minimal raw-bytes fetch for a photo URL — this project has no image-loading library, so thumbnails decode manually. */
expect suspend fun fetchImageBytes(url: String): ByteArray?
