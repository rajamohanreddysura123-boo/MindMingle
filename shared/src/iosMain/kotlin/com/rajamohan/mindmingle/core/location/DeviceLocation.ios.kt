package com.rajamohan.mindmingle.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject

/** Long enough for a first fix indoors, short enough that the deck is not left waiting. */
private const val FIX_TIMEOUT_MILLIS = 8_000L

/**
 * CoreLocation is part of the OS, so this needs no Swift package and no bridge in `iosApp` — the
 * same reason `ImagePicker.ios.kt` can present a photo picker from Kotlin directly.
 *
 * `requestLocation()` rather than `startUpdatingLocation()`: it delivers one fix and stops on its
 * own, which is exactly the contract [DeviceLocation] promises and costs nothing to keep.
 */
@OptIn(ExperimentalForeignApi::class)
actual object DeviceLocation {

    actual val isSupported: Boolean = true

    /** Held for the duration of the request; ARC would otherwise free the delegate mid-flight. */
    private var activeDelegate: NSObject? = null

    actual suspend fun current(): DeviceLocationResult {
        if (!CLLocationManager.locationServicesEnabled()) return DeviceLocationResult.LocationDisabled

        val manager = CLLocationManager()

        when (manager.authorizationStatus) {
            kCLAuthorizationStatusDenied -> return DeviceLocationResult.PermissionPermanentlyDenied
            kCLAuthorizationStatusRestricted -> return DeviceLocationResult.PermissionPermanentlyDenied
            kCLAuthorizationStatusNotDetermined -> {
                // The prompt is answered asynchronously and this call does not wait for it: the fix
                // fails once, and the next Home open — by then answered — succeeds.
                manager.requestWhenInUseAuthorization()
                return DeviceLocationResult.PermissionDenied
            }
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> Unit
            else -> return DeviceLocationResult.Unavailable
        }

        val fix = CompletableDeferred<DeviceLocationResult>()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                if (location == null) {
                    fix.complete(DeviceLocationResult.Unavailable)
                    return
                }
                location.coordinate.useContents {
                    val lat = latitude
                    val lng = longitude
                    // Names are looked up separately and must not hold the fix hostage: the
                    // completion below fires with whatever the geocoder managed, empty included.
                    CLGeocoder().reverseGeocodeLocation(location) { placemarks, _ ->
                        val placemark = placemarks?.firstOrNull() as? CLPlacemark
                        fix.complete(
                            DeviceLocationResult.Located(
                                latitude = lat,
                                longitude = lng,
                                place = placemark.toPlaceNames()
                            )
                        )
                    }
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                fix.complete(DeviceLocationResult.Unavailable)
            }
        }

        activeDelegate = delegate
        manager.delegate = delegate
        manager.requestLocation()

        val result = withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { fix.await() }
        activeDelegate = null
        manager.delegate = null

        return result ?: DeviceLocationResult.Unavailable
    }

    private val CLLocationManager.authorizationStatus: CLAuthorizationStatus
        get() = CLLocationManager.authorizationStatus()

    /**
     * CLGeocoder is part of CoreLocation, so this needs no key and no Maps SDK. `subAdministrativeArea`
     * is Apple's district/county level — the same ADM2 the filter compares against.
     */
    private fun CLPlacemark?.toPlaceNames(): PlaceNames {
        if (this == null) return PlaceNames()
        return PlaceNames(
            countryCode = ISOcountryCode.orEmpty().uppercase(),
            region = administrativeArea.orEmpty(),
            district = subAdministrativeArea.orEmpty(),
            city = locality ?: subAdministrativeArea.orEmpty()
        )
    }
}
