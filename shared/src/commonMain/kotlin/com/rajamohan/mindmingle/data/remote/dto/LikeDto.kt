package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LikeDto(
    val liked: Boolean = true
)
