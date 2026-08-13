package com.rajamohan.mindmingle.core.media

// Not wired up on iOS yet — thumbnails for existing (already-uploaded) photos won't render there.
actual suspend fun fetchImageBytes(url: String): ByteArray? = null
