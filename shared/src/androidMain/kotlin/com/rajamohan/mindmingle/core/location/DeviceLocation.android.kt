package com.rajamohan.mindmingle.core.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rajamohan.mindmingle.core.AppContext
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "DeviceLocation"
private const val PERMISSION_REQUEST_CODE = 4712

/** A fix older than this is a place the user has probably left. */
private const val MAX_FIX_AGE_MILLIS = 10 * 60 * 1000L

/** How long to wait for a fresh fix before giving up and letting the IP lookup answer. */
private const val FIX_TIMEOUT_MILLIS = 8_000L

/** Names are a bonus on top of the fix; they never get to hold the deck up for long. */
private const val GEOCODE_TIMEOUT_MILLIS = 5_000L

/**
 * Uses the framework LocationManager rather than Play Services' fused provider.
 *
 * The fused client is better at this — it fuses sensors and it is smarter about power — but it is
 * another dependency for a number this app renders as "2.4 km away" and buckets below a kilometre.
 * The framework API is in the OS, needs nothing added, and is accurate to well inside the precision
 * that is actually displayed.
 */
actual object DeviceLocation {

    actual val isSupported: Boolean = true

    actual suspend fun current(): DeviceLocationResult {
        val context = AppContext.get() as? Context ?: return DeviceLocationResult.Unavailable

        val permission = ensurePermission(context)
        if (permission != null) return permission

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return DeviceLocationResult.Unavailable

        if (!manager.isLocationEnabled()) return DeviceLocationResult.LocationDisabled

        return try {
            // A recent cached fix is worth more than a fresh one here: it costs no radio time and
            // no wait, and "where you were ten minutes ago" is the same answer at this precision.
            recentCachedFix(manager)?.let { return it.toResult() }

            requestSingleFix(manager)?.toResult() ?: DeviceLocationResult.Unavailable
        } catch (e: SecurityException) {
            // Permission revoked between the check above and the call — treat as refused.
            Napier.w(throwable = e, tag = TAG) { "location read denied" }
            DeviceLocationResult.PermissionDenied
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "location read failed" }
            DeviceLocationResult.Unavailable
        }
    }

    /** Null when permission is held; otherwise the result the caller should get back. */
    private fun ensurePermission(context: Context): DeviceLocationResult? {
        if (hasPermission(context)) return null

        val activity = context as? Activity ?: return DeviceLocationResult.PermissionDenied

        // "Don't ask again" and "never asked" look identical to checkSelfPermission; the rationale
        // flag is what separates them, and it is only false-after-a-refusal in the permanent case.
        val refusedPermanently = !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) && hasAskedBefore

        if (refusedPermanently) return DeviceLocationResult.PermissionPermanentlyDenied

        hasAskedBefore = true
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            PERMISSION_REQUEST_CODE
        )

        // The dialog is answered asynchronously and this call is not going to wait for it. The
        // fix simply fails this once; the next Home open — after the user has answered — succeeds.
        return if (hasPermission(context)) null else DeviceLocationResult.PermissionDenied
    }

    private var hasAskedBefore = false

    private fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun LocationManager.isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLocationEnabled
        } else {
            isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun recentCachedFix(manager: LocationManager): Location? {
        val now = System.currentTimeMillis()
        return manager.allProviders
            .mapNotNull { provider ->
                @Suppress("MissingPermission")
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { now - it.time <= MAX_FIX_AGE_MILLIS }
            .maxByOrNull { it.time }
    }

    private suspend fun requestSingleFix(manager: LocationManager): Location? {
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        // Network first, GPS second: a city-block-accurate fix that arrives in a second beats a
        // metre-accurate one that needs a clear view of the sky and thirty seconds of radio.
        return withTimeoutOrNull(FIX_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }

                @Suppress("MissingPermission")
                manager.getCurrentLocation(
                    provider,
                    signal,
                    { runnable -> runnable.run() }
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }
        }
    }

    private suspend fun Location.toResult(): DeviceLocationResult =
        DeviceLocationResult.Located(
            latitude = latitude,
            longitude = longitude,
            place = reverseGeocode(latitude, longitude)
        )

    /**
     * Names for a coordinate, via the framework `Geocoder`.
     *
     * Best-effort by design: it needs a network, it can return nothing, and on API 33+ the blocking
     * overload is deprecated in favour of a callback. Both paths end at the same place, and a
     * failure returns empty names rather than failing the fix — a profile with coordinates and no
     * district is still perfectly usable, it just will not match a district filter.
     */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceNames {
        val context = AppContext.get() as? Context ?: return PlaceNames()
        if (!Geocoder.isPresent()) return PlaceNames()

        val geocoder = Geocoder(context)

        return try {
            withTimeoutOrNull(GEOCODE_TIMEOUT_MILLIS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull().toPlaceNames())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull().toPlaceNames()
                }
            } ?: PlaceNames()
        } catch (e: Exception) {
            Napier.d(throwable = e, tag = TAG) { "reverse geocode failed — coordinates kept, names dropped" }
            PlaceNames()
        }
    }

    private fun Address?.toPlaceNames(): PlaceNames {
        if (this == null) return PlaceNames()
        return PlaceNames(
            countryCode = countryCode.orEmpty().uppercase(),
            region = adminArea.orEmpty(),
            district = subAdminArea.orEmpty(),
            city = locality ?: subLocality ?: subAdminArea.orEmpty()
        )
    }
}
