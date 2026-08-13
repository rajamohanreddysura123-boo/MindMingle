import UIKit
import Shared

#if canImport(Razorpay)
import Razorpay
#endif

/**
 Wires the Razorpay iOS SDK into the shared Kotlin payment layer (MindMingle+ upgrade).

 Same shape as AdMobBridge: Gradle never links Razorpay, so the shared framework builds
 with or without the SDK. The Kotlin side declares `IosPaymentHost`
 (shared/src/iosMain/.../core/payments/IosPaymentHost.kt) and this file fills it in.

 Setup (once):
   1. Xcode > File > Add Package Dependencies… > https://github.com/razorpay/razorpay-pod
      (or add `pod 'Razorpay-Customui-Universal'` / the Razorpay SPM package for your account).
   2. Info.plist: add the URL scheme `<your key id>` under CFBundleURLSchemes so UPI intent
      apps can return to MindMingle after payment.
   3. Nothing else — amounts, order ids and verification all come from Cloud Functions
      (functions/src/razorpay.ts). The key secret never reaches this app.

 Until step 1 is done, `canImport(Razorpay)` is false, the bridge is never installed, and
 PremiumScreen shows checkout as unavailable on iOS instead of failing mid-payment.
 */
enum RazorpayBridge {

    /// Called once at app start from iOSApp.swift.
    static func install() {
        #if canImport(Razorpay)
        IosPaymentHost.shared.install { options, onFinished in
            CheckoutPresenter.shared.present(options: options, onFinished: onFinished)
        }
        #endif
    }
}

#if canImport(Razorpay)
/// Holds the delegate alive for the duration of one checkout and maps its callbacks to the map
/// contract `PaymentPlatform.ios.kt` expects: status = success | cancelled | failed.
private final class CheckoutPresenter: NSObject, RazorpayPaymentCompletionProtocolWithData {

    static let shared = CheckoutPresenter()

    private var razorpay: RazorpayCheckout?
    private var onFinished: (([String: String]) -> Void)?

    func present(options: [String: String], onFinished: @escaping ([String: String]) -> Void) {
        self.onFinished = onFinished

        let checkout = RazorpayCheckout.initWithKey(options["keyId"] ?? "", andDelegateWithData: self)
        razorpay = checkout

        var payload: [String: Any] = [
            "order_id": options["orderId"] ?? "",
            "currency": options["currency"] ?? "INR",
            "name": "MindMingle",
            "description": options["description"] ?? "MindMingle+"
        ]
        if let amount = options["amount"], let paise = Int(amount) {
            payload["amount"] = paise
        }
        var prefill: [String: String] = [:]
        if let name = options["name"], !name.isEmpty { prefill["name"] = name }
        if let email = options["email"], !email.isEmpty { prefill["email"] = email }
        if let contact = options["contact"], !contact.isEmpty { prefill["contact"] = contact }
        if !prefill.isEmpty { payload["prefill"] = prefill }

        checkout.open(payload, displayController: topViewController() ?? UIViewController())
    }

    func onPaymentSuccess(_ payment_id: String, andData response: [AnyHashable: Any]?) {
        deliver([
            "status": "success",
            "paymentId": payment_id,
            "orderId": response?["razorpay_order_id"] as? String ?? "",
            "signature": response?["razorpay_signature"] as? String ?? ""
        ])
    }

    func onPaymentError(_ code: Int32, description str: String, andData response: [AnyHashable: Any]?) {
        // 0 is Razorpay's "user cancelled" network/cancel code; everything else is a real failure.
        if code == Int32(RZPError.cancelled.rawValue) {
            deliver(["status": "cancelled"])
        } else {
            deliver(["status": "failed", "message": str])
        }
    }

    private func deliver(_ result: [String: String]) {
        let callback = onFinished
        onFinished = nil
        razorpay = nil
        callback?(result)
    }

    private func topViewController() -> UIViewController? {
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
}
#endif
