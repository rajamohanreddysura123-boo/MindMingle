package com.rajamohan.mindmingle.core.media

/**
 * How many photos a profile may hold. The picker, the "add" tile and the view model all read this
 * one value — three copies of the number is how a UI ends up offering a sixth slot the view model
 * then refuses.
 */
const val MAX_PROFILE_PHOTOS = 5

/**
 * Lets the user pick at most [maxCount] photos from their device. Returns raw image bytes, or an
 * empty list if they cancelled.
 *
 * Implementations must not return more than [maxCount]: the limit is enforced by the system picker
 * where the platform has one, and by truncation where it does not.
 */
expect object ImagePicker {
    suspend fun pickImages(maxCount: Int): List<ByteArray>
}
