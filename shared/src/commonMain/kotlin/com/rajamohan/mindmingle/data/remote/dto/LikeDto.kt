package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * A like marker, carrying enough of the liker's profile to draw their card in "who liked me".
 *
 * The copy is deliberate. Reading the profile per like meant one document read per liker every
 * time the screen opened — and the live stream re-read all of them on every change, so a user
 * with 500 likes paid 500 reads per update. These fields make the list cost exactly the number of
 * like documents.
 *
 * They are a snapshot from the moment of the like: a liker who later renames themselves shows
 * their old name until they like someone again. Opening their profile always reads the live doc.
 * Docs written before these fields existed decode with blanks.
 */
@Serializable
data class LikeDto(
    val liked: Boolean = true,
    val fromUid: String = "",
    val name: String = "",
    val occupation: String = "",
    val experienceLevel: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = 0L
)
