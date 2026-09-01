# Distance on profile cards — analysis and plan

Scope is distance only, as asked. Country filter, the location dataset and geohash sharding are
noted at the end as deferred, not planned here.

**Implemented 2026-09-01.** All four changes below are in the working tree; the client compiles on
every target, the 11 new tests pass, `:androidApp:assembleDebug` succeeds and the cloud function
type-checks. Nothing has run on a device, and the function is not deployed — see "Deploy order".

---

## A. Current architecture

Distance is not a missing feature. It is a **built feature standing on a broken input**.

| Piece | Where | State |
| --- | --- | --- |
| Haversine, client | `HomeContract.kt:118` `haversineKm()` — private | Correct |
| Haversine, server | `mindmingle_backend_services/src/index.ts:66` | Correct |
| Distance per card | `HomeUiState.distanceToKm(user)` — computed on device from both parties' coordinates | Works, but see D |
| Display | `HomeScreen.kt:890`, `DesktopHomeScreen.kt:399`, `ProfileDetailScreen.kt:265` — `"${distanceKm.toInt()} km away"` | Crude, see C |
| Distance filter | `DiscoverFilters.maxDistanceKm`, premium-gated in the sheet (`HomeScreen.kt:1288`) and re-checked server-side (`index.ts:178`) | Correct, both layers |
| Own coordinates | `users/{uid}.latitude/.longitude`, written once during profile setup | **Broken input — see B** |
| Other users' coordinates | Returned in full by `filterDiscoverProfiles` | Privacy problem — see D |
| Storage | Firestore, `users/{uid}` | — |
| Code layout | KMP: `shared` (common/android/ios/jvm) + thin `androidApp` | — |

So requirements 1, 2, 3 (enforcement), 11, 12 and 13 are already satisfied by the existing code.
There is no per-card network call, distance rides the existing discovery response, and Google Maps
is not involved anywhere.

## B. The actual defect: coordinates come from an IP lookup, not the device

`LocationService.getCurrentLocation()` calls **ipapi.co**, falling back to **ipwho.is**. Both return
the coordinates of the IP address, which is the ISP's egress point.

- On Wi-Fi that is typically the right city, wrong suburb — 5-15 km out.
- On mobile data it is the carrier gateway, which can be a different city entirely — 50-500 km out.
- Two people sitting in the same room on different carriers can be "180 km" apart.

So "📍 2.4 km away" is currently fiction, and the premium 5 km filter silently filters on that
fiction. Everything else about the feature is sound; this one input is why it cannot be trusted.

Compounding it: the lookup runs **once**, during profile setup (`ProfileSetupViewModel:202`). There
is no refresh and no `locationUpdatedAt`, so someone who moves keeps their old city forever.

Also note `androidApp/src/main/AndroidManifest.xml` declares no location permission at all, which
is consistent — nothing has ever asked the OS where the device is.

## C. Second defect: the displayed number

`"${distanceKm.toInt()} km away"` truncates. Someone 900 m away reads **"0 km away"**. Someone
340 km away reads "340 km away", which is both useless and more precise than is wise.

## D. Third defect: exact coordinates of every candidate are on the device

`filterDiscoverProfiles` returns whole user documents, so `latitude` and `longitude` for every
profile in the deck land on the client, where `distanceToKm` uses them. Anyone reading the response
gets a precise fix on strangers. This is the one privacy requirement the current design fails, and
it is inseparable from distance because moving the calculation server-side is what fixes it.

---

## E. What was built

Four changes, in dependency order.

### E1. Real device coordinates — the change that makes the number true

New `core/location/DeviceLocation.kt`, an expect object beside the existing `LocationService`, with
a sealed result so every failure path is explicit rather than a null:

```
Located(latitude, longitude)   PermissionDenied   PermissionPermanentlyDenied
LocationDisabled               Unavailable        NotSupported
```

- **Android:** the framework `LocationManager` — `getCurrentLocation()` on API 31+, last-known
  fallback below. Deliberately **not** `play-services-location`: the framework API is enough for
  a "how far away" number and adds no dependency, which matters more here than the last few metres
  of accuracy. Manifest gains `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`; the runtime
  request follows the pattern `PushPlatform.requestPermission()` already uses.
- **iOS:** `CLLocationManager` from `iosMain` directly. CoreLocation is part of the OS, so this
  needs no Swift bridge and no package — the same reasoning that let `PHPickerViewController` be
  written in Kotlin.
- **Desktop:** `NotSupported`, falling through to the existing IP lookup.

**IP geolocation stays as the fallback**, not as the primary. Denied permission, disabled GPS or a
timeout all fall back to today's behaviour, so the deck never ends up with no coordinates at all —
it ends up with worse ones, which is what it has now.

### E2. A refresh strategy, and one new field

`User`/`UserDto` gain `locationUpdatedAt: Long`. Nothing else — `latitude`/`longitude` already
exist and are reused, per Requirement 6.

Refresh runs from `HomeViewModel.loadProfiles`, which is where the deck is built, and only when:

- the stored fix is older than **12 hours**, or
- the new fix is more than **2 km** from the stored one.

Otherwise it writes nothing. No continuous tracking, no background updates, no location listener —
a dating app needs to know roughly where you are when you open the deck, and nothing more. The
write is the same single-field `update` pattern `setPremiumFlag` uses.

### E3. Move the calculation server-side, and stop shipping coordinates

In `filterDiscoverProfiles`:

1. Compute `distanceKm` per candidate with the haversine already in that file, whenever both the
   caller and the candidate have coordinates.
2. **Delete `latitude` and `longitude` from each profile before returning it**, and attach the
   rounded `distanceKm` instead.
3. Round server-side to one decimal below 10 km and to a whole number above, so the wire itself
   never carries a precise fix.

Client side: `UserDto`/`User` gain a transient `distanceKm: Double?` and
`HomeUiState.distanceToKm(user)` becomes `user.distanceKm` — it keeps its name and signature, so
the three call sites are untouched by this part.

`myLatitude`/`myLongitude` stay in `HomeUiState`, contrary to the first draft of this plan: they
are the signed-in user's *own* coordinates, which the client legitimately has, and `hasMyLocation`
reads them to decide whether the distance filter is offerable. They are now fed by the refresh
rather than straight from the profile document.

The Kotlin `haversineKm` **stays**, moved out of `HomeContract` into `domain/model/GeoDistance.kt`,
public. It is no longer on the hot path but it is the reference implementation the tests exercise,
and it is what the desktop client would use if a response ever arrived without distances.

### E4. One formatter, three call sites

`GeoDistance.formatDistance(km: Double): String` in the same file:

| Input | Output |
| --- | --- |
| < 1 km | `Less than 1 km away` |
| 1-9.9 km | `2.4 km away` (one decimal) |
| 10-99 km | `18 km away` (whole) |
| ≥ 100 km | `100+ km away` |

Replaces `"${distanceKm.toInt()} km away"` in `HomeScreen`, `DesktopHomeScreen` and
`ProfileDetailScreen`. The bucketing at both ends is a privacy measure as much as a formatting one:
an exact sub-kilometre figure that updates as someone moves is a tracking primitive.

---

## F. Files that change

| File | Change | Risk |
| --- | --- | --- |
| `core/location/DeviceLocation.kt` + 3 actuals | New | Low — new surface, IP fallback preserved |
| `androidApp/src/main/AndroidManifest.xml` | Two location permissions | Low, but it is a new Play Store disclosure |
| `domain/model/GeoDistance.kt` | New; haversine moved out of `HomeContract`, formatter added | Low |
| `domain/model/User.kt`, `dto/UserDto.kt` | `+ locationUpdatedAt`, `+ distanceKm` (transient) | Low |
| `MindMingleFirebaseProvider` | `updateLocation(uid, lat, lng, updatedAt)` | Low |
| `HomeViewModel` | Refresh-on-open, drop `myLatitude`/`myLongitude` | **Medium** — deck load path |
| `HomeContract` | `distanceToKm` and private haversine removed; `hasMyLocation` re-sourced | Medium |
| `HomeScreen`, `DesktopHomeScreen`, `ProfileDetailScreen` | Read `profile.distanceKm`, use the formatter | Low |
| `mindmingle_backend_services/src/index.ts` | Attach distance, strip coordinates | **Medium** — needs a deploy, and old clients keep working only because they fall back to no distance |
| `shared/src/commonTest/.../GeoDistanceTest.kt` | New — first test in the repo | Low |

## G. Tests

`shared/src/commonTest` does not exist yet; this creates it. The dependency is already declared in
`shared/build.gradle.kts`.

- **Haversine** against known pairs at 0, 1, 5, 10, 25, 50 and 100 km, 1% tolerance; the equator/
  antimeridian crossing; identical points returning exactly 0.
- **Formatter** at each boundary: 0.4, 0.99, 1.0, 9.94, 10.0, 99.6, 100.0, 340.
- **Filter arithmetic**: a 25 km filter keeps 24.9 and drops 25.1.

Subscription enforcement for the distance filter already exists in both layers and is unchanged, so
it is covered by reading, not by new tests.

## H. Deploy order and migration

Every existing profile keeps whatever coordinates the IP lookup gave it, and `locationUpdatedAt`
defaults to `0L` — which reads as "older than 12 hours", so the first Home open after the update
refreshes it from the device. No backfill, no migration script, no downtime. A user who refuses the
permission stays exactly where they are today.

**Deploy order: ship the client first, then the function.** They are in the same working tree but
they go out separately, and the order is not optional — the function stripping `latitude`/
`longitude` would blank the distance on any older client, which computes it locally from exactly
those fields. A newer client against the old function is harmless: `distanceKm` is simply absent and
no distance is shown.

```
firebase deploy --only functions:filterDiscoverProfiles
```
(that function lives in the `mindmingle-backend-services` codebase, not `functions`, so it is
unaffected by the `razorpay.ts` breakage.)

## I. Deliberately not in this plan

Country filter, the location dataset, and geohash sharding. On sharding specifically: the current
scan is capped (`SCAN_CAP`) and distance is applied as a post-filter over scanned candidates, which
is fine at this size but means a 5 km filter on a large user base will return a thin deck rather
than a slow one. That is a real scaling limit, and the fix is a geohash prefix query — worth doing
when the user base makes it hurt, not before.
