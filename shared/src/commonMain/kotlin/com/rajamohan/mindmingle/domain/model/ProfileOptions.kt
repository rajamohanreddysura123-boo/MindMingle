package com.rajamohan.mindmingle.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mindmingle.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
data class ProfileFieldOption(
    val key: String,
    val label: String,
    val type: String,
    val options: List<String> = emptyList(),
    val placeholder: String = "",
    val optional: Boolean = false,
    val maxSelect: Int = 0
)

@Serializable
data class ProfileSection(
    val title: String,
    val subtitle: String = "",
    val fields: List<ProfileFieldOption> = emptyList()
)

@Serializable
private data class ProfileOptionsFile(val sections: List<ProfileSection> = emptyList())

object ProfileOptionsRepository {
    private var cachedSections: List<ProfileSection>? = null
    private val json = Json { ignoreUnknownKeys = true }

    const val INTERESTS_KEY = "interests"
    const val DATE_OF_BIRTH_KEY = "dateOfBirth"

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getSections(): List<ProfileSection> {
        cachedSections?.let { return it }

        return try {
            val bytes = Res.readBytes("files/profile_options.json")
            val parsed = json.decodeFromString<ProfileOptionsFile>(bytes.decodeToString()).sections
            cachedSections = parsed
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun optionsFor(key: String): List<String> =
        getSections().flatMap { it.fields }.firstOrNull { it.key == key }?.options ?: emptyList()

    /**
     * The full field definitions for [keys], in the order given and skipping any that carry no
     * options — a filter row with nothing to pick would just be a dead heading.
     */
    suspend fun fieldsFor(keys: List<String>): List<ProfileFieldOption> {
        val byKey = getSections().flatMap { it.fields }.associateBy { it.key }
        return keys.mapNotNull { key -> byKey[key]?.takeIf { it.options.isNotEmpty() } }
    }
}
