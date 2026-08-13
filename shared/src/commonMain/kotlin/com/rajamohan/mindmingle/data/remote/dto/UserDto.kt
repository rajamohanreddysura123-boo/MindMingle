package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val uid: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val bio: String = "",
    val occupation: String = "",
    val interests: List<String> = emptyList(),
    val experienceLevel: String = "",
    val lookingFor: String = "",
    val githubUrl: String = "",
    val portfolioLinks: List<String> = emptyList(),
    val age: Int = 0,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isProfileComplete: Boolean = false,
    val isDisabled: Boolean = false,
    val createdAt: Long = 0L,
    val details: Map<String, String> = emptyMap(),
    val selections: Map<String, List<String>> = emptyMap()
)