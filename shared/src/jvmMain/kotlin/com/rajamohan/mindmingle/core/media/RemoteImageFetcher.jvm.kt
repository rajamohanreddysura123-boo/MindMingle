package com.rajamohan.mindmingle.core.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

actual suspend fun fetchImageBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        URL(url).openStream().use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
