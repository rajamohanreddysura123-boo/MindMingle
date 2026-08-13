package com.rajamohan.mindmingle.domain.model

import kotlinx.serialization.json.Json
import mindmingle.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Occupation list bundled from https://github.com/johnlsheridan/occupations (occupations.csv),
 * supplemented with a handful of modern tech-era titles the ~2015 UK source list predates.
 */
object OccupationRepository {
    private var cachedList: List<String>? = null
    private val json = Json { ignoreUnknownKeys = true }

    /** Shown by default before the user types a search query. */
    val defaultShortlist = listOf(
        "Doctor", "Software Engineer", "Lawyer", "Scientist", "AI / ML Engineer",
        "Accountant", "Teacher", "Nurse", "Designer", "Entrepreneur"
    )

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getOccupations(): List<String> {
        cachedList?.let { return it }

        return try {
            val bytes = Res.readBytes("files/occupations.json")
            val jsonString = bytes.decodeToString()
            val parsed = json.decodeFromString<List<String>>(jsonString)
            cachedList = parsed
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackList()
        }
    }

    fun fallbackList(): List<String> = defaultShortlist
}
