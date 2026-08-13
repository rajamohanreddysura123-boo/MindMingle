This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Google AdMob in the Discover deck

Every 5th swiped profile, the Discover card slot is taken over by an ad and Pass/Connect are
disabled until it finishes. Every 3rd ad slot is a full-screen interstitial instead of the in-card
banner. All of it is driven from Firestore, so frequency and ad units change without a release.

There is no cross-platform Google Ads SDK, so the shared code talks to `core/ads/AdsPlatform`
(`expect`/`actual`):

| Target  | Implementation |
|---------|----------------|
| Android | `play-services-ads` directly (`AdsPlatform.android.kt`) |
| iOS     | `IosAdHost` + [`iosApp/iosApp/AdMobBridge.swift`](./iosApp/iosApp/AdMobBridge.swift), so Gradle never has to link the ObjC framework |
| Desktop | No-op — Google ships no desktop SDK, and the ad gate never arms there |

**Firestore config — `appConfig/ads`** (read: any signed-in user, write: admin only; see
`firestore.rules`). Create it in the Firebase console with these fields; any missing field falls
back to the defaults in `domain/model/AdConfig.kt`:

| Field | Type | Default | Meaning |
|-------|------|---------|---------|
| `enabled` | bool | `true` | Master switch |
| `everyNProfiles` | number | `5` | Ad slot after this many swipes |
| `interstitialEveryNAdSlots` | number | `3` | Every Nth ad slot is full-screen |
| `minSecondsOnAdCard` | number | `5` | Swipe lock duration on a banner slot |
| `androidBannerUnitId` / `androidInterstitialUnitId` | string | test ids | Android ad units |
| `iosBannerUnitId` / `iosInterstitialUnitId` | string | test ids | iOS ad units |

Deploy the rules that guard it with `firebase deploy --only firestore:rules`.

**Before shipping live ads**

1. Replace the AdMob *app id* in `androidApp/src/main/AndroidManifest.xml` — it currently holds
   Google's public test app id, and the SDK crashes at launch if it is missing entirely.
2. Put your real *ad unit ids* in `appConfig/ads` (they are a different thing from the app id).
3. Publish `app-ads.txt` on the developer-website domain listed in your Play/App Store entry.
4. Add a consent flow (Google UMP SDK) before serving to EEA/UK users.
5. Keep using the test ids on debug builds — clicking live ads on your own device is invalid
   traffic and can get the AdMob account disabled.

**iOS**: ads stay off until the GoogleMobileAds Swift package is added to the `iosApp` target and
`GADApplicationIdentifier` is set in `Info.plist`. Step-by-step in the header comment of
[`AdMobBridge.swift`](./iosApp/iosApp/AdMobBridge.swift).

### MindMingle+ subscriptions (Razorpay)

Upgrading from Profile → Upgrade opens Razorpay Checkout. An active plan turns ads off
everywhere (`HomeViewModel` refuses to arm an ad slot while `isPremium`) and swaps the profile
upgrade banner for an active-plan card.

Nothing about money is decided on the device:

1. `createRazorpayOrder` (Cloud Function) picks the amount and currency for the user's market
   and creates the order. The client only sends a plan id.
2. Razorpay Checkout runs — Android via `com.razorpay:checkout`, iOS via
   [`RazorpayBridge.swift`](./iosApp/iosApp/RazorpayBridge.swift), desktop not at all.
3. `verifyRazorpayPayment` checks the HMAC signature with the key secret, re-reads the payment
   from Razorpay's API, then writes `subscriptions/{uid}`. That doc is **read-only to every
   client** (`firestore.rules`), so premium — and therefore ad removal — cannot be self-granted.
4. `razorpayWebhook` grants the same plan if the app dies before step 3.

**Secrets** (never committed):

```
firebase functions:secrets:set RAZORPAY_KEY_ID
firebase functions:secrets:set RAZORPAY_KEY_SECRET
firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET
```

Then point a Razorpay webhook for `payment.captured` at the deployed `razorpayWebhook` URL.

#### Checking a payment afterwards

`getPaymentDetails({ paymentId })` reads the payment back from Razorpay — status, amount,
currency, method, timestamp — for the payer or an admin. It doubles as a repair path: if
Razorpay says *captured* but no plan was ever recorded (app killed mid-checkout, webhook never
fired), it grants the plan while answering.

`getBillingHistory({ uid? })` returns the subscription summary plus every payment, newest first.
Users see their own (Profile → **Orders & Billing**); admins can pass any uid, which is what
fills the subscription panel in the admin user detail screen.

#### Admin subscription controls

Admin → user detail now has a **Subscription** panel: current plan, status, end date, full order
history, and actions — *Grant 1 month*, *Grant 1 year* (`adminSetSubscription`, recorded as a
zero-amount `admin-grant` row in the same history the user sees), *Cancel at period end* and
*End now* (`adminCancelSubscription`).

Cancelling at period end keeps access — and keeps ads off — until the time already paid for runs
out (`status: "cancelled"`). *End now* writes `status: "revoked"` with the period ending
immediately, so ads come back on that user's next Discover load.

#### Deleting an account

Both delete paths run server-side, because a client SDK cannot delete a Firebase Auth user,
Storage objects, or the other half of someone else's like:

- **User** — Profile → *Delete My Account* → `deleteMyAccount`. Erases the profile, photos,
  likes (both directions), matches and their messages, anonymous queue/rooms/audit entries,
  subscription and payment records, OTP and mail rows, and finally the Auth account. No
  `bannedUids` tombstone is left, so the same person can sign up fresh later.
- **Admin** — user detail → *Delete Account* → `adminDeleteUser`, same purge plus a `bannedUids`
  tombstone so the uid stays locked out.

The Auth account is deleted **last**: if a data step fails, the user can still sign in and retry
instead of being locked out of an account that still has records behind it.

#### Per-country pricing

`appConfig/plans` holds one row per market — currency, symbol, decimals, dial code, monthly and
annual amount — seeded from [`functions/src/pricing.ts`](./functions/src/pricing.ts) on first use
(India ₹99/month and ₹999/year; ~50 markets in their own currency; anything unlisted falls back to
the US row in USD). Amounts are stored in the currency's smallest unit, which is what Razorpay
charges in: ₹99 is `9900`, ¥399 is `399` (JPY has no decimals), KWD 0.999 is `999`.

A super admin edits the list in-app: **Admin → Dashboard → Plan Pricing** (whole units there —
type `99`, not `9900`), with *Restore defaults* to re-seed. Saving goes through `savePlanPricing`,
which re-validates every row, so a typo can never become a live charge.

The billed country is resolved **server-side** from the phone number on the user's profile, not
from anything the client sends — otherwise a patched app could shop for the cheapest market.
Accounts with no phone on file fall back to `defaultCountry`.

**Before charging real money**: Razorpay only accepts non-INR currencies once *International
Payments* is enabled on the account, and each currency must be on your account's supported list —
settlement still lands in your INR account at Razorpay's conversion rate. Check the enabled list
in the Razorpay dashboard and trim `appConfig/plans` to match, or those markets will fail at
checkout.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…