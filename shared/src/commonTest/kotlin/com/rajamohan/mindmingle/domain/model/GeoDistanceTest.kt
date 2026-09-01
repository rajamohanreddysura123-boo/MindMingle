package com.rajamohan.mindmingle.domain.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The deck's distance is what someone decides on, so the arithmetic behind it is worth pinning.
 *
 * Expected values come from a reference Haversine over the same coordinates; the tolerance is 1%,
 * which is far tighter than the difference between Haversine and a real ellipsoid (~0.3%) matters
 * for "how far away is this person".
 */
class GeoDistanceTest {

    private val hyderabad = 17.3850 to 78.4867

    private fun assertKmWithin(expected: Double, actual: Double, tolerance: Double = 0.01) {
        val allowed = expected * tolerance
        assertTrue(
            abs(actual - expected) <= allowed,
            "expected ${expected}km +/- ${allowed}km but was ${actual}km"
        )
    }

    @Test
    fun identicalPointsAreZero() {
        val (lat, lng) = hyderabad
        assertEquals(0.0, GeoDistance.haversineKm(lat, lng, lat, lng))
    }

    @Test
    fun shortHopAcrossACity() {
        // Hyderabad: Charminar to Hussain Sagar, 6.93 km apart on a great circle.
        assertKmWithin(6.93, GeoDistance.haversineKm(17.3616, 78.4747, 17.4239, 78.4738))
    }

    @Test
    fun oneDegreeOfLatitudeIsAboutOneHundredAndElevenKm() {
        // The cleanest fixed reference there is: a degree of latitude is ~111.19 km anywhere.
        assertKmWithin(111.19, GeoDistance.haversineKm(0.0, 0.0, 1.0, 0.0))
        assertKmWithin(111.19, GeoDistance.haversineKm(45.0, 20.0, 46.0, 20.0))
    }

    @Test
    fun knownCityPairs() {
        // Each pair is a great-circle distance between the two city centres, computed
        // independently rather than quoted — published "distance between cities" figures are
        // usually driving distances and are 10-30% longer.
        assertKmWithin(1255.4, GeoDistance.haversineKm(17.3850, 78.4867, 28.6139, 77.2090)) // Hyderabad-Delhi
        assertKmWithin(343.6, GeoDistance.haversineKm(51.5074, -0.1278, 48.8566, 2.3522)) // London-Paris
        assertKmWithin(4129.1, GeoDistance.haversineKm(40.7128, -74.0060, 37.7749, -122.4194)) // NYC-SF
    }

    @Test
    fun crossesTheAntimeridianWithoutBlowingUp() {
        // Two points 2 degrees apart either side of 180. Naive coordinate subtraction reads this as
        // 358 degrees and gives a nonsense answer; the great circle is about 222 km.
        assertKmWithin(222.4, GeoDistance.haversineKm(0.0, 179.0, 0.0, -179.0))
    }

    @Test
    fun distanceIsSymmetric() {
        val there = GeoDistance.haversineKm(17.3850, 78.4867, 12.9716, 77.5946)
        val back = GeoDistance.haversineKm(12.9716, 77.5946, 17.3850, 78.4867)
        assertEquals(there, back)
    }

    @Test
    fun formatsSubKilometreWithoutANumber() {
        assertEquals("Less than 1 km away", GeoDistance.formatDistance(0.0))
        assertEquals("Less than 1 km away", GeoDistance.formatDistance(0.4))
        assertEquals("Less than 1 km away", GeoDistance.formatDistance(0.99))
    }

    @Test
    fun formatsWalkableDistancesToOneDecimal() {
        assertEquals("1 km away", GeoDistance.formatDistance(1.0))
        assertEquals("2.4 km away", GeoDistance.formatDistance(2.4387291827))
        assertEquals("9.9 km away", GeoDistance.formatDistance(9.94))
    }

    @Test
    fun formatsTensOfKilometresAsWholeNumbers() {
        assertEquals("10 km away", GeoDistance.formatDistance(10.0))
        assertEquals("18 km away", GeoDistance.formatDistance(17.6))
        assertEquals("100 km away", GeoDistance.formatDistance(99.6))
    }

    @Test
    fun capsAtOneHundred() {
        assertEquals("100+ km away", GeoDistance.formatDistance(100.0))
        assertEquals("100+ km away", GeoDistance.formatDistance(340.2))
    }

    @Test
    fun premiumFiltersAreTheOnesStrippedForAFreeUser() {
        // What the client sends when a paying user has set everything, and what must survive
        // downgrading. The server repeats this decision — see filterDiscoverProfiles — because a
        // modified client can send whatever it likes; this pins the client half.
        val paid = DiscoverFilterCriteria(
            minAge = 25,
            maxAge = 30,
            genders = setOf("Female"),
            lookingFor = setOf("Relationship"),
            maxDistanceKm = 25,
            countries = setOf("IN"),
            languages = setOf("Telugu"),
            interests = setOf("Music"),
            occupations = setOf("Designer"),
            experienceLevels = setOf("Senior"),
            detailFilters = mapOf("diet" to setOf("Vegetarian"))
        )

        val free = paid.withoutPremiumFilters()

        // Paid-only groups go.
        assertEquals(null, free.maxDistanceKm)
        assertTrue(free.countries.isEmpty())
        assertTrue(free.languages.isEmpty())
        assertTrue(free.detailFilters.isEmpty())

        // Everything a free user is entitled to keeps working, which is the other half of the
        // requirement: downgrading must not empty the deck.
        assertEquals(25, free.minAge)
        assertEquals(30, free.maxAge)
        assertEquals(setOf("Female"), free.genders)
        assertEquals(setOf("Relationship"), free.lookingFor)
    }

    @Test
    fun distanceFilterBoundaryIsInclusiveOfWhatItSays() {
        // What the 25 km filter has to mean: 24.9 stays, 25.1 goes.
        val limit = 25
        assertTrue(24.9 <= limit)
        assertTrue(25.1 > limit)
    }
}
