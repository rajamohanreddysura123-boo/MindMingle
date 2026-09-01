package com.rajamohan.mindmingle.core.push

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * UserNotifications is part of the OS, not of Firebase, so this needs none of the Swift bridge
 * [IosPushHost] exists for — it works even on a build where FirebaseMessaging was never linked.
 * The permission prompt still comes from that bridge.
 */
actual object LocalNotifier {

    actual val isSupported: Boolean = true

    actual fun show(id: Int, title: String, body: String, destination: NotificationDestination?) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            // Read back in the notification delegate on the Swift side once PushBridge exists;
            // until then a tap opens the app without routing, which is the old behaviour.
            destination?.let { setUserInfo(mapOf(NotificationDestination.EXTRA_KEY to it.raw)) }
        }

        // iOS refuses a zero-second trigger, and a null trigger fires immediately but is only
        // allowed for some request types; the shortest legal delay is used instead.
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 0.1,
            repeats = false
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            request = UNNotificationRequest.requestWithIdentifier(
                identifier = id.toString(),
                content = content,
                trigger = trigger
            ),
            withCompletionHandler = null
        )
    }
}
