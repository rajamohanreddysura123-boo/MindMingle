package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Firestore `appConfig/appUpdate` — publicly readable, admin-writable (see firestore.rules).
 *
 * Google Play already knows whether a newer build exists, so this doc deliberately does NOT
 * say "is an update available". It only carries the one thing Play cannot tell the client:
 * the *floor* the app is still allowed to run at, decided after a release has shipped.
 * Play's per-release `inAppUpdatePriority` is frozen at publish time and cannot be raised
 * later, so without this doc a broken 1.8.2 can never be turned into a forced update.
 *
 * Version comparison is done on versionCode (a monotonically increasing Int/Long), never on
 * the versionName string — "1.8.10" sorts before "1.8.9" lexicographically.
 *
 * Every field defaults to a value that means "do nothing", so a missing, empty or partially
 * filled doc can never lock users out of the app.
 */
@Serializable
data class AppUpdateConfigDto(
    /** Master switch; false makes the client ignore the whole doc. */
    val enabled: Boolean = true,
    /** Android builds with a versionCode below this are forced to update. 0 = no floor. */
    val androidMinimumVersionCode: Long = 0L,
    /** Newest Android versionCode published; informational only, Play is the real source. */
    val androidLatestVersionCode: Long = 0L,
    /** Newest Android versionName, e.g. "1.8.5". Display only — never compared. */
    val androidLatestVersionName: String = "",
    /** Belt and braces: the floor is only enforced while this is true. */
    val forceUpdate: Boolean = false
)
