package com.rajamohan.mindmingle.core.location

/** Desktop has no location surface; callers fall through to the IP lookup in [LocationService]. */
actual object DeviceLocation {
    actual val isSupported: Boolean = false
    actual suspend fun current(): DeviceLocationResult = DeviceLocationResult.NotSupported
}
