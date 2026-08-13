package com.rajamohan.mindmingle.domain.model

data class AnonymousSession(
    val sessionId: String,
    val peerAlias: String,
    val peerAccentIndex: Int
)
