import UIKit
import Shared

#if canImport(GoogleSignIn)
import GoogleSignIn
import FirebaseAuth
#endif

/**
 Wires Google Sign-In into the shared Kotlin auth layer.

 The shared module deliberately does not link GoogleSignIn — doing that from Gradle would force a
 CocoaPods integration onto this Xcode project. The Kotlin side declares `IosGoogleAuthHost`
 (shared/src/iosMain/.../data/remote/source/IosGoogleAuthHost.kt) and this file fills it in, so the
 shared framework keeps building whether or not the SDK is present.

 Setup (once):
   1. Xcode > File > Add Package Dependencies… > https://github.com/google/GoogleSignIn-iOS
   2. Info.plist > URL Types > add a URL Scheme equal to `REVERSED_CLIENT_ID` from
      GoogleService-Info.plist. Without it Google's sheet opens and never returns.
   3. Nothing else: the client id is read from GoogleService-Info.plist, which is already here.

 Until step 1 is done, `canImport(GoogleSignIn)` is false, the bridge is never installed, and
 sign-in reports that it is unavailable rather than failing obscurely.

 Note this signs into *Firebase Auth*, not merely into Google: the shared code expects
 `Firebase.auth.currentUser` to exist by the time it is called back, because everything downstream
 — the profile document, the rules, the uid — is keyed on the Firebase user.
 */
enum GoogleAuthBridge {

    /// Called once at app start from iOSApp.swift.
    static func install() {
        #if canImport(GoogleSignIn)
        IosGoogleAuthHost.shared.install { onResult in
            signIn(onResult: onResult)
        }
        #endif
    }

    #if canImport(GoogleSignIn)
    private static func signIn(onResult: @escaping (String, String, String?) -> Void) {
        guard let presenter = topViewController() else {
            onResult("", "", "No window to present sign-in from")
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error {
                // A cancel arrives here too; the shared layer shows it as an error and the user
                // simply tries again, which is the same as the Android behaviour.
                onResult("", "", error.localizedDescription)
                return
            }

            guard
                let user = result?.user,
                let idToken = user.idToken?.tokenString
            else {
                onResult("", "", "Google returned no credential")
                return
            }

            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: user.accessToken.tokenString
            )

            // Exchanged for a Firebase session before the callback fires: the shared code reads
            // Firebase.auth.currentUser immediately afterwards.
            Auth.auth().signIn(with: credential) { authResult, authError in
                if let authError {
                    onResult("", "", authError.localizedDescription)
                    return
                }

                let email = authResult?.user.email ?? user.profile?.email ?? ""
                let name = authResult?.user.displayName ?? user.profile?.name ?? ""
                onResult(email, name, nil)
            }
        }
    }

    private static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
    #endif
}
