package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.core.location.DeviceLocation
import com.rajamohan.mindmingle.core.location.DeviceLocationResult
import com.rajamohan.mindmingle.core.location.LocationService
import com.rajamohan.mindmingle.core.location.PlaceNames
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.GeoDistance
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import io.github.aakira.napier.Napier

/**
 * Keeps the signed-in user's own coordinates current enough for the deck to mean something.
 *
 * ## When it writes
 * Called when Discover loads, and it writes only when the stored fix is [STALE_AFTER_MILLIS] old
 * or the device has moved more than [MOVED_THRESHOLD_KM] from it. A dating app needs to know
 * roughly where someone is when they open the deck and nothing more, so there is no subscription,
 * no background job and no listener — one fix, on a screen the user deliberately opened.
 *
 * The two conditions cover the two ways a stored position goes wrong: it ages (someone travelled
 * while the app was closed), or it is simply somewhere else now. Neither alone is enough — a
 * commuter who opens the app twice an hour from two cities would keep the first fix all day on
 * age alone, and someone who never moves would never refresh a year-old fix on distance alone.
 *
 * ## What it falls back to
 * A refused permission, a disabled GPS or a timeout falls through to [LocationService], the IP
 * lookup that was the only source before this existed. It is far less accurate — an IP resolves to
 * the ISP's egress point — but a rough position keeps the deck working, and it is exactly what
 * every existing profile already has.
 */
class RefreshMyLocationUseCase(
    private val repository: MindMingleRemoteRepository
) {
    private companion object {
        const val TAG = "RefreshMyLocation"

        /** A fix older than half a day is a place the user may well have left. */
        const val STALE_AFTER_MILLIS = 12 * 60 * 60 * 1000L

        /** Below this, rewriting the document would not change a single displayed distance. */
        const val MOVED_THRESHOLD_KM = 2.0
    }

    /**
     * @param me the profile as last read, so this costs no extra read of the user's own document.
     * @return the coordinates the deck should use — the new ones when a write happened, the stored
     *   ones otherwise, or null when neither exists.
     */
    suspend operator fun invoke(me: User?): Pair<Double, Double>? {
        val uid = me?.uid ?: return null
        val stored = me.latitude?.let { lat -> me.longitude?.let { lng -> lat to lng } }

        if (!needsRefresh(me)) return stored

        val reading = readDeviceLocation() ?: return stored
        val fresh = reading.first
        if (stored != null && !hasMovedFrom(stored, fresh)) {
            // Same place, and the stored fix is only stale by the clock. Nothing displayed would
            // change, so the write is skipped and the age simply carries on — the next real move
            // is what updates it.
            return stored
        }

        val place = reading.second
        repository.updateLocation(
            uid = uid,
            latitude = fresh.first,
            longitude = fresh.second,
            updatedAt = nowMillis(),
            countryCode = place.countryCode,
            region = place.region,
            district = place.district
        )
        return fresh
    }

    private fun needsRefresh(me: User): Boolean {
        if (me.latitude == null || me.longitude == null) return true
        return nowMillis() - me.locationUpdatedAt >= STALE_AFTER_MILLIS
    }

    private fun hasMovedFrom(stored: Pair<Double, Double>, fresh: Pair<Double, Double>): Boolean {
        val moved = GeoDistance.haversineKm(stored.first, stored.second, fresh.first, fresh.second)
        return moved >= MOVED_THRESHOLD_KM
    }

    /**
     * The device first, the IP lookup second. Null when neither can answer.
     *
     * The IP path carries a country name and a city but no district — that level simply is not
     * knowable from an IP address — so a profile that never gets a real fix can match a country
     * filter and never a district one. That is the honest result, not a bug to paper over.
     */
    private suspend fun readDeviceLocation(): Pair<Pair<Double, Double>, PlaceNames>? {
        if (DeviceLocation.isSupported) {
            when (val result = DeviceLocation.current()) {
                is DeviceLocationResult.Located ->
                    return (result.latitude to result.longitude) to result.place
                else -> Napier.d(tag = TAG) { "device location unavailable: $result — falling back to IP" }
            }
        }

        val byIp = runCatching { LocationService.getCurrentLocation() }.getOrNull() ?: return null
        val lat = byIp.latitude ?: return null
        val lng = byIp.longitude ?: return null
        val place = PlaceNames(
            countryCode = CountryCodeRepository.codeForName(byIp.country),
            region = byIp.region,
            city = byIp.city
        )
        return (lat to lng) to place
    }
}
