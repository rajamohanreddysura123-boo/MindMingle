import UIKit
import UserNotifications
import Shared

#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

/**
 Wires Firebase Cloud Messaging into the shared Kotlin push layer.

 Same arrangement as AdMobBridge and GoogleAuthBridge: the shared module never links
 FirebaseMessaging, `IosPushHost` (shared/src/iosMain/.../core/push/IosPushHost.kt) declares the
 closures, and this file fills them in.

 Setup (once):
   1. Xcode > the Firebase package > add the `FirebaseMessaging` product to the iosApp target.
   2. Signing & Capabilities > + Capability > Push Notifications.
   3. Signing & Capabilities > + Capability > Background Modes > Remote notifications.
   4. Apple Developer > Keys > create an APNs key, then upload it in Firebase Console >
      Project Settings > Cloud Messaging > Apple app configuration. **Without this step Firebase
      has no way to reach the device and every push is silently dropped**, even though everything
      in the app looks correctly wired.

 Until step 1 is done, `canImport(FirebaseMessaging)` is false, the bridge is never installed, and
 the app asks for no notification permission — which also means the in-app alert watchers cannot
 show anything, since they need the same permission.
 */
final class PushBridge: NSObject {

    static let shared = PushBridge()

    private var permissionCompletion: ((Bool) -> Void)?

    /// Called once at app start from iOSApp.swift.
    static func install() {
        #if canImport(FirebaseMessaging)
        UNUserNotificationCenter.current().delegate = shared
        Messaging.messaging().delegate = shared

        IosPushHost.shared.install(
            requestPermission: { onResult in
                shared.requestPermission { granted in
                    onResult(KotlinBoolean(bool: granted))
                }
            },
            fetchToken: { onResult in
                Messaging.messaging().token { token, error in
                    if let error {
                        print("PushBridge: token fetch failed — \(error.localizedDescription)")
                    }
                    onResult(token)
                }
            }
        )
        #endif
    }

    /**
     Asks for notification permission, then registers with APNs.

     Both halves are needed and in this order: the OS prompt decides whether anything can be
     shown, and `registerForRemoteNotifications` is what produces the APNs token FCM exchanges for
     its own. Registering without permission yields a token that can never deliver anything.
     */
    private func requestPermission(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error {
                print("PushBridge: authorization failed — \(error.localizedDescription)")
            }
            DispatchQueue.main.async {
                if granted {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                completion(granted)
            }
        }
    }
}

#if canImport(FirebaseMessaging)
extension PushBridge: MessagingDelegate {

    /// FCM rotates tokens on reinstall and on restore to a new device; the shared layer
    /// re-registers whatever arrives here under the signed-in user.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        IosPushHost.shared.onTokenRefreshed(token: fcmToken)
    }
}
#endif

extension PushBridge: UNUserNotificationCenterDelegate {

    /// Without this, a notification arriving while the app is open is delivered silently. The
    /// in-app watchers post their own notifications through the same centre, so this is what
    /// makes those visible too.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}
