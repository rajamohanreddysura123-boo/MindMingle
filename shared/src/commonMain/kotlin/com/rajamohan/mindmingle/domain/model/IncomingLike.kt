package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.LikeDto

/**
 * One row of a user's "who liked me" mailbox, with the timestamp the list itself does not need.
 *
 * The alert watcher works off this: without [createdAt] there is no way to separate a like that
 * has just landed from the ones that were already sitting there when the stream opened, and the
 * app would announce someone's whole backlog on every launch.
 */
data class IncomingLike(
    val fromUid: String,
    val name: String,
    val createdAt: Long
)

fun LikeDto.toIncomingLike(): IncomingLike = IncomingLike(
    fromUid = fromUid,
    name = name,
    createdAt = createdAt
)
