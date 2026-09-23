package com.rajamohan.mindmingle.core.location

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "LocationService"

internal actual suspend fun fetchLocationJson(url: String): String? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        // Every provider here is free-tier and rate-limited per IP, so a 429 is an expected
        // outcome, not a bug — logged at debug rather than warn, and the caller already has
        // two more providers and a cooldown (RefreshMyLocationUseCase) to fall back on.
        Napier.d(throwable = e, tag = TAG) { "fetch failed for $url" }
        null
    }
}
