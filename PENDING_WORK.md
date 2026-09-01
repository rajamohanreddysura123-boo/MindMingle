# Pending work

Everything known to be unfinished, in the order it makes sense to do it. Written 2026-09-01 from a
read of the code, not from a spec — if something here contradicts your intent, your intent wins.

Each item says who does it. **You** means it cannot be done from this repo: an Xcode target, a
Firebase Console setting, a deploy, or a device test. **Me** means I can do it here and verify it
with `./gradlew :shared:compileKotlinJvm :androidApp:compileDebugKotlin
:shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug`.

---

## Phase 0 — unblock what is already broken

Nothing below this line is worth starting until these are done. Both are yours.

### 0.1 Restore `functions/src/razorpay.ts` — **You**
I ran `git checkout` on that file to undo an edit of mine, not realising it held uncommitted work.
The repo has one commit, so it reverted to that, and `recordPaymentFailure` went with it; `npx tsc`
fails on `index.ts:15`. Android Studio → right-click the file → Local History → restore a revision
from before 12:58 on 2026-09-01. `functions/lib/razorpay.js` (compiled 26 Aug) still contains the
logic if Local History has nothing.

Then **commit**. One commit for the whole project is why a single mistaken checkout could cost work.

### 0.2 Deploy rules and indexes — **Done 2026-09-01**
Deployed: rules compiled and released, indexes applied. The `conversations` block and the corrected
chat-list index are live. (The CLI noted one index defined in the project but not in the file; left
alone rather than pruned with `--force`.)

### 0.2b — original note
```
firebase deploy --only firestore:rules,firestore:indexes
```
Two things are waiting on this, both of which break features today:
- `conversations/*` had no rule at all, so every chat read and write was denied by default. The
  block exists now but is inert until deployed.
- The composite index for the chat list (`users array-contains` + `lastMessageAt desc`) was declared
  against the retired `matches` collection. Corrected, also inert until deployed.

---

## Phase 1 — iOS is not shippable

Three separate holes, all of which need Xcode. I write the Kotlin side; you do the Xcode side.

### 1.1 Google sign-in — **You + me**
`GoogleAuthLauncher.ios.kt` returns `onError("… requires GIDSignIn configured in Xcode")`. Mobile
sign-in is Google-only, so **iOS cannot sign in at all** — nothing downstream of this can be tested
on a device until it works.

- You: add the `GoogleSignIn-iOS` Swift package to the `iosApp` target, add the `REVERSED_CLIENT_ID`
  from `GoogleService-Info.plist` as a URL scheme in `Info.plist`, and write `GoogleAuthBridge.swift`
  presenting `GIDSignIn.sharedInstance.signIn(withPresenting:)`.
- Me: an `IosGoogleAuthHost` object shaped exactly like the existing `IosAdHost` / `IosPaymentHost`
  bridges, and the actual that calls it. I can write the Swift file too if you'd rather review than
  author — it still has to be added to the target in Xcode by you.

### 1.2 Photo picking — **Done 2026-09-01**
`ImagePicker.ios.kt` now presents `PHPickerViewController`. PhotosUI is part of the OS, so it needed
no package and no Swift bridge. `selectionLimit` enforces the five-photo cap in the system UI, the
picker runs out of process so the app never needs photo-library permission, and each item is loaded
as raw data for `public.image` rather than as a decoded `UIImage` — the bytes go straight to
Storage, so a decode-and-re-encode would cost a generation of quality for nothing.

**Untested.** It compiles for both simulator and device targets, but nothing has run it: with 1.1
outstanding there is no way to sign in on iOS and reach profile setup.

### 1.3 Push — **You + me**
`IosPushHost.install()` is never called: `iosApp` has `AdMobBridge.swift` and `RazorpayBridge.swift`
but no `PushBridge.swift`.

- You: add `FirebaseMessaging` to the target's Firebase package products, enable the Push
  Notifications capability, upload an APNs key in the Firebase Console, and add `PushBridge.swift`
  calling `IosPushHost.install(...)` from `iOSApp.init()` beside the other two.
- Me: the Swift file's contents and any Kotlin-side adjustment.

Note the local notifications added on 2026-09-01 work on iOS without any of this, because
`UNUserNotificationCenter` is part of the OS — but nothing asks for permission until the bridge
exists, so they will not appear.

### 1.4 Face check — **Decision needed from you**
`FaceDetector.ios.kt` returns `true` unconditionally; ML Kit is Android-only. So the "every photo
must show a face" rule is enforced on Android and not on iOS. Options: accept the asymmetry, use
Apple's `Vision` framework (`VNDetectFaceRectanglesRequest`, in the OS, no package), or move the
check server-side so it holds everywhere. **Vision is the cheap correct answer** unless you want it
server-side for other reasons.

---

## Phase 2 — finish notifications

The client-side watchers work (a like or a message raises a local notification while the app is
alive). What is left is the plumbing around them.

### 2.1 `FirebaseMessagingService` — **Done 2026-09-01**
`MindMingleMessagingService` in the app module, declared in the manifest. It forwards token
rotation to `AndroidPushBridge` (which until now had no caller at all) and draws foreground
messages through `LocalNotifier`, so a notification looks the same and routes the same whether it
came from a server or from the in-app watchers. The app module gained its own `firebase-messaging`
dependency, because a manifest may only name classes from its own application.

### 2.2 Unregister on sign-out — **Done 2026-09-01**
`AuthViewModel` now has a single `signOut()` that drops this device's token before releasing the
session — in that order, because only the owner may delete the row and after sign-out there is no
credential left to do it with. Both sign-out paths use it, including the one that ejects a blocked
account.

### 2.3 Notification settings screen — **Done 2026-09-01**
A Notifications card at the top of Account Settings: one switch per `NotificationCategory`, with
its label and description. The switch moves first and is reverted only if the write fails, since a
toggle that waits on a round trip feels broken. A missing preferences document means everything is
on, so the first toggle is what creates it.

### 2.4 Tap routing — **Done 2026-09-01**
`NotificationDestination` (Likes, or a Chat with an optional conversation id) rides in the launch
intent. `MainActivity` parks it in `PendingDestination` from both `onCreate` and `onNewIntent` —
the activity is `singleTask` now, so a tap while running does not stack a second one — and the
signed-in shell consumes it once, switching tab and opening the named conversation as soon as the
chat list has loaded. Both alert watchers and the FCM service attach destinations.

### 2.5 Deploy the notification functions — **Decided 2026-09-01: deploy**

Background notifications, the way every app in the category does them, need a trusted sender. That
is the one thing a client cannot be, so the three Firestore triggers in
`functions/src/notifications.ts` get deployed — a deliberate exception to the project's standing
"no Cloud Functions" rule, made because there is no alternative rather than for convenience.

Client side is ready: tokens register on sign-in, the channel matches, `POST_NOTIFICATIONS` is
declared, and a tap routes to the right screen from both a self-drawn and a system-drawn
notification.

**Prerequisites, in order.**
1. **0.1 first.** `npm run build` runs before every deploy and the whole codebase fails to compile
   while `razorpay.ts` is missing `recordPaymentFailure`. Nothing deploys until that is restored.
2. **Blaze plan.** Cloud Functions v2 will not deploy on Spark. Notifications for a small user base
   sit inside the free monthly allowance, but the plan itself is required.
3. **First deploy enables APIs** — Cloud Functions, Cloud Build, Artifact Registry, Eventarc. The
   CLI prompts; accept.

**Deploy just the three triggers first:**
```
cd functions && npm run build && cd ..
firebase deploy --only functions:onLikeReceived,functions:onChatMessageCreated,functions:onMatchCreated
```
Deliberately not `--only functions`: that would also push the Razorpay callables and the email-OTP
signer, none of which have been exercised since the plan-pricing move, plus `subscriptionReminders`,
which needs Cloud Scheduler. Add those later, one at a time.

`setGlobalOptions` pins everything to `asia-southeast1`; Firestore triggers are forced into the
database's region regardless, so these three would land there either way.

**Duplicates are handled by construction.** With the app in the foreground both the server push and
the in-app watcher fire for the same event, but they key their notification on the same value — the
liker's uid, or the conversation id — so the second replaces the first row rather than stacking.

**iOS background push still needs 1.3.** APNs key and `PushBridge.swift`, or iOS gets nothing while
backgrounded no matter what is deployed.

---

## Phase 3 — bugs and gaps

### 3.1 Profile "Chats" count caps at 30 — **Done 2026-09-01**
Both stats now come from `GetProfileStatsUseCase`, backed by Firestore `count()` aggregations. The
cap is gone, and the screen went from fetching a thirty-document page plus every like document — in
full, to display two integers — to two aggregation reads.

### 3.2 Admin unban has no UI — **Done 2026-09-01**
A "Lift Ban" action on the admin user detail screen, deleting the `bannedUids/{uid}` tombstone a
purge leaves behind. No confirmation dialog: it is the reversible direction, and re-banning is just
running the purge again.

### 3.3 Orphaned photos after an account purge — **You (Console) or me (Functions)**
Storage rules cannot read Firestore, so `storage.rules` cannot grant an admin delete based on
`users/{uid}.userType`. A purged account's photos stay in the bucket, unreachable but stored. The
fix needs a custom auth claim (`request.auth.token.admin == true`), which only the Admin SDK can
set — so it needs a Cloud Function, or manual Console cleanup.

---

## Phase 4 — verification

**You.** None of the work from 2026-08-31 or 2026-09-01 has run on a device: no emulator or
connected device is available from here. Everything is compile-verified and `assembleDebug`-verified
only. Worth a pass on a real phone:

- the deck's photo carousel and its tap zones versus the swipe gesture (they share the same surface)
- the profile detail screen's spacing and the new fact grid
- the five-photo cap in the Android 13+ system picker
- like and message notifications actually appearing
- the premium tick, which depends on `users/{uid}.isPremium` being mirrored on first launch

---

## Done in this pass (2026-09-01)

1.2, and all of Phase 2 and Phase 3.1-3.2. Every target compiles and `:androidApp:assembleDebug`
passes; none of it has run on a device. What remains below is what needs you.

## Production readiness (2026-09-01, second pass)

Done since the last pass:
- `recordPaymentFailure` reconstructed from its caller's contract and the `paymentAttempts` rules,
  after the compiled `lib/razorpay.js` turned out to predate it too. The functions codebase
  compiles again, and `onLikeReceived` / `onChatMessageCreated` / `onMatchCreated` are **deployed**.
- **UMP consent** (`AdsPlatform.requestConsent`) on Android and through the iOS bridge, called
  before `MobileAds.initialize` — required for EEA/UK users, which a global app has by definition.
- **Test-device registration** hook in `AdsPlatform.registerTestDevices`; add your phone's hashed
  id to `TEST_DEVICE_IDS` after reading it out of the first run's logcat.
- **GoogleAuthBridge.swift** and **PushBridge.swift** written, plus `IosGoogleAuthHost`, and both
  installed from `iOSApp.init()`.
- **Node 22** across both codebases; all 18 deployed functions moved off the deprecated runtime.
  The same deploy removed four dead callables from production — `getPaymentDetails`,
  `getPlanPricing`, `savePlanPricing`, `resetPlanPricing` — and replaced the live
  `recordPaymentFailure` with the reconstruction, on the owner's instruction.

Left, and all of it needs you:

| What | Why only you |
| --- | --- |
| Check `appConfig/ads` holds real ad unit ids | Console. Blank fields mean test ads worldwide and no revenue. |
| Add the three Swift packages in Xcode | GoogleSignIn, FirebaseMessaging, GoogleMobileAds. Target membership cannot be scripted. |
| Push capability + APNs key | Xcode capability and an Apple Developer key uploaded to Firebase. |
| Play data-safety form | Precise location is new. |
| Run it on a device | Nothing here has ever run. |
| ~~Node 20 runtime~~ | **Done 2026-09-01** — both codebases pinned to Node 22 and all 18 functions redeployed. Zero remain on 20. |

## What I need from you, shortest form

1. **Restore `razorpay.ts`** from Local History, then commit the repo.
2. **Run** `firebase deploy --only firestore:rules,firestore:indexes`.
3. **Xcode work** for 1.1 and 1.3 — packages, capabilities, APNs key, and adding the bridge files to
   the target. I can write the Swift; I cannot add it to a target.
4. **Three decisions:** deploy the notification functions or accept foreground-only alerts (2.5);
   Vision versus server-side versus nothing for the iOS face check (1.4); and whether orphaned
   photos are worth a Cloud Function (3.3).
5. **A device test pass** once Phase 1 and 2 land.

Everything else on this page I can do from here. Say which phase to start.
