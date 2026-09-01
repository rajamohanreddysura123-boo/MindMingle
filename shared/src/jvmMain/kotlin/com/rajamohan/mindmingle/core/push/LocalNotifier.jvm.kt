package com.rajamohan.mindmingle.core.push

/** Desktop has no notification surface in this app, matching [PushPlatform]'s own desktop actual. */
actual object LocalNotifier {
    actual val isSupported: Boolean = false
    actual fun show(id: Int, title: String, body: String, destination: NotificationDestination?) = Unit
}
