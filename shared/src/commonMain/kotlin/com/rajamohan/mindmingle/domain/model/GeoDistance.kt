package com.rajamohan.mindmingle.domain.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * How far apart two people are, and how to say it.
 *
 * The calculation itself is a great-circle distance — no mapping service is involved in working out
 * that someone is 2 km away, and none should be: it is arithmetic on two pairs of coordinates.
 *
 * The authoritative distance for the deck is computed server-side, where it can be attached to a
 * profile without also handing the client that person's coordinates. This stays as the reference
 * implementation the tests exercise, and as the fallback for a response that carries none.
 */
object GeoDistance {

    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance in kilometres between two lat/long pairs (Haversine). */
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLng = (lng2 - lng1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * The distance as a person should read it.
     *
     * Blunt at both ends on purpose. Below a kilometre nothing is said beyond "close", because a
     * figure that sharpens as someone walks around is a tracking primitive, not a convenience; and
     * above a hundred kilometres the exact number stops meaning anything to a reader deciding
     * whether to swipe. In between, one decimal while it is still walkable and whole kilometres
     * after that.
     */
    fun formatDistance(km: Double): String = when {
        km < 0 -> ""
        km < 1.0 -> "Less than 1 km away"
        km < 10.0 -> "${(round(km * 10) / 10).oneDecimal()} km away"
        km < 100.0 -> "${round(km).toInt()} km away"
        else -> "100+ km away"
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    /** Kotlin/Native has no String.format, so the one decimal place is assembled by hand. */
    private fun Double.oneDecimal(): String {
        val whole = this.toInt()
        val tenths = round((this - whole) * 10).toInt()
        return if (tenths == 0) "$whole" else "$whole.$tenths"
    }
}
