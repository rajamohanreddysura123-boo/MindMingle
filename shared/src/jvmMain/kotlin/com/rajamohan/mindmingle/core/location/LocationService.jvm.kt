package com.rajamohan.mindmingle.core.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal actual suspend fun fetchLocationJson(url: String): String? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
