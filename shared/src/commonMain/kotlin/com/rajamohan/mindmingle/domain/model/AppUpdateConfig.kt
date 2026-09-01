package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.AppUpdateConfigDto

/**
 * The backend-controlled half of the update policy, mirrored from Firestore `appConfig/appUpdate`.
 *
 * Play Store availability (is there a newer build?) is answered by the Play In-App Update API.
 * This model answers the separate question Play cannot: *is the installed build still supported?*
 * The two are combined in InAppUpdateManager (androidMain) to pick flexible vs immediate.
 *
 * Defaults are the "no opinion" values, so an unreadable or unseeded doc leaves the app running
 * with Play's own priority as the only signal.
 */
data class AppUpdateConfig(
    val enabled: Boolean = true,
    val minimumVersionCode: Long = 0L,
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val forceUpdate: Boolean = false
) {
    /**
     * True only when the backend has explicitly declared [installedVersionCode] out of support.
     * A zero/negative floor, a disabled doc or forceUpdate=false all answer false, which is what
     * keeps a bad config from bricking the app.
     */
    fun isBelowMinimum(installedVersionCode: Long): Boolean =
        enabled &&
            forceUpdate &&
            minimumVersionCode > 0L &&
            installedVersionCode > 0L &&
            installedVersionCode < minimumVersionCode
}

fun AppUpdateConfigDto.toDomain(): AppUpdateConfig = AppUpdateConfig(
    enabled = enabled,
    minimumVersionCode = androidMinimumVersionCode,
    latestVersionCode = androidLatestVersionCode,
    latestVersionName = androidLatestVersionName,
    forceUpdate = forceUpdate
)
