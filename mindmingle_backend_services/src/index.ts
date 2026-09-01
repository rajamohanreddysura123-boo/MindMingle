import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";

/**
 * Run next to the database (asia-southeast1) rather than in the us-central1 default. This
 * function pages through `users` a document at a time, so every cross-region round trip is
 * paid many times over inside a single deck load.
 */
setGlobalOptions({ region: "asia-southeast1" });

admin.initializeApp();
const db = admin.firestore();

interface FilterDiscoverProfilesRequest {
  minAge?: number;
  maxAge?: number;
  interests?: string[];
  countries?: string[];
  districts?: string[];
  lookingFor?: string[];
  occupations?: string[];
  maxDistanceKm?: number | null;
  genders?: string[];
  experienceLevels?: string[];
  languages?: string[];
  detailFilters?: Record<string, string[]>;
  /** Document id to resume the scan after — the `cursor` from the previous response. */
  cursor?: string;
  /** How many profiles to return; clamped to [1, MAX_DECK_SIZE]. */
  limit?: number;
}

/** Profiles handed back per call. The client asks again with the returned cursor as the deck runs low. */
const DEFAULT_DECK_SIZE = 30;
const MAX_DECK_SIZE = 50;

/** Documents read per page while scanning for candidates. */
const SCAN_PAGE_SIZE = 100;

/**
 * Hard ceiling on documents examined in one call. This is what keeps the cost of a deck load
 * independent of how many users the app has: worst case is SCAN_CAP reads, not one per user.
 * A very narrow filter may return a short deck rather than scanning the whole collection.
 */
const SCAN_CAP = 500;

function clamp(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) return min;
  return Math.min(Math.max(Math.trunc(value), min), max);
}

/**
 * A random point in the auto-id keyspace, used as the starting cursor when the caller has none.
 * Firestore ids are ordered lexicographically, so a random id lands at a random position in the
 * collection — which is what stops every user's deck from starting with the same profiles.
 */
function randomDocumentId(): string {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let id = "";
  for (let i = 0; i < 20; i++) {
    id += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
  }
  return id;
}

/** Great-circle distance between two lat/long points, in kilometers. Mirrors haversineKm in HomeContract.kt. */
function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const earthRadiusKm = 6371.0;
  const dLat = ((lat2 - lat1) * Math.PI) / 180.0;
  const dLng = ((lng2 - lng1) * Math.PI) / 180.0;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180.0) *
      Math.cos((lat2 * Math.PI) / 180.0) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
}

function asStringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}

/**
 * Lifestyle/intent filters arrive keyed by profile_options.json field key. Entries that aren't
 * string arrays, and keys narrowed to nothing, are dropped so they can't match-all or match-none
 * by accident.
 */
function asFilterMap(value: unknown): Record<string, string[]> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) return {};
  const result: Record<string, string[]> = {};
  for (const [key, entry] of Object.entries(value as Record<string, unknown>)) {
    const values = asStringArray(entry);
    if (values.length > 0) result[key] = values;
  }
  return result;
}

/**
 * Mirrors Subscription.isActiveAt in the Kotlin model: a cancelled plan still counts until the
 * period already paid for runs out, a revoked one stops immediately. `subscriptions/{uid}` is
 * server-written only (firestore.rules), so this cannot be faked client-side.
 */
async function isCallerPremium(uid: string): Promise<boolean> {
  const snapshot = await db.collection("subscriptions").doc(uid).get();
  const subscription = snapshot.data();
  if (!subscription) return false;
  const currentPeriodEnd = typeof subscription.currentPeriodEnd === "number" ? subscription.currentPeriodEnd : 0;
  return currentPeriodEnd > Date.now() && subscription.status !== "revoked";
}

/**
 * True when `requested` is empty (filter off) or the profile's answer for `key`
 * intersects it. Profile-setup answers land in two shapes: single-select fields
 * in `details` (string) and multi-select fields in `selections` (string[]), so
 * both are read through here.
 */
function matchesProfileAnswer(user: FirebaseFirestore.DocumentData, key: string, requested: string[]): boolean {
  if (requested.length === 0) return true;

  const single = user.details?.[key];
  if (typeof single === "string" && single.length > 0) {
    return requested.includes(single);
  }

  const multi = asStringArray(user.selections?.[key]);
  return multi.some((entry) => requested.includes(entry));
}

/**
 * Discover-feed matching for the home screen's filter sheet, run server-side
 * (Kotlin client: MindMingleFirebaseProvider.getDiscoverProfiles). Excludes the
 * caller, disabled, deactivated and deletion-pending profiles and incomplete
 * profiles, then applies age/distance/
 * interests/lookingFor/occupation/gender/experience/language filters in memory —
 * Firestore can't express a range filter plus multiple array-contains-any filters
 * in a single query, and the user base doesn't yet warrant maintaining composite
 * indexes for it.
 */
/**
 * A profile's ISO country code.
 *
 * Profiles written since the country filter existed carry `countryCode` directly. Older ones have
 * only the free-text `location` the IP lookup produced — "Hyderabad, Telangana, India" — whose last
 * comma-separated part is the country name. Reading that as a fallback is what lets the filter work
 * on the existing user base with no backfill and no migration; it costs one string split on
 * profiles that would otherwise simply never match.
 */
function countryCodeOf(user: FirebaseFirestore.DocumentData): string {
  const stored = String(user.countryCode ?? "").trim().toUpperCase();
  if (stored) return stored;

  const tail = String(user.location ?? "").split(",").pop()?.trim().toLowerCase();
  if (!tail) return "";
  return COUNTRY_NAME_TO_CODE[tail] ?? "";
}

/**
 * Names only for the countries a legacy `location` string is likely to end with. This is a
 * fallback for documents written before `countryCode` existed, not a country list — the client
 * reads all 242 from the bundled CountryCodes.json, which is the single source for the filter UI.
 */
const COUNTRY_NAME_TO_CODE: Record<string, string> = {
  india: "IN",
  "united states": "US",
  "united states of america": "US",
  usa: "US",
  "united kingdom": "GB",
  uk: "GB",
  canada: "CA",
  australia: "AU",
  germany: "DE",
  france: "FR",
  singapore: "SG",
  "united arab emirates": "AE",
  uae: "AE",
  netherlands: "NL",
  ireland: "IE",
  "new zealand": "NZ",
};

/**
 * Replaces a candidate's coordinates with the distance to the caller.
 *
 * This is a dating app: handing every client the exact latitude and longitude of twenty strangers
 * — which is what returning the raw user document did — is a location leak whatever the UI chooses
 * to draw with it. The client only ever needs the number on the card, so only the number is sent.
 *
 * The figure is rounded before it leaves: one decimal while it is still walkable, whole kilometres
 * after that. An unrounded distance is a radius, and three of them are a fix.
 */
function stripLocation(
  user: FirebaseFirestore.DocumentData,
  callerLat: number | null,
  callerLng: number | null
): FirebaseFirestore.DocumentData {
  const { latitude, longitude, ...rest } = user;

  const canMeasure =
    callerLat !== null && callerLng !== null &&
    typeof latitude === "number" && typeof longitude === "number";

  if (!canMeasure) return rest;

  const km = haversineKm(callerLat as number, callerLng as number, latitude, longitude);
  return {
    ...rest,
    distanceKm: km < 10 ? Math.round(km * 10) / 10 : Math.round(km),
  };
}

export const filterDiscoverProfiles = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const callerUid = request.auth.uid;

  const data = (request.data ?? {}) as FilterDiscoverProfilesRequest;
  const minAge = Number.isFinite(data.minAge) ? Number(data.minAge) : 18;
  const maxAge = Number.isFinite(data.maxAge) ? Number(data.maxAge) : 70;
  const lookingFor = asStringArray(data.lookingFor);
  const genders = asStringArray(data.genders);

  // Two tiers of search.
  //
  // Free: age, gender and what someone is looking for — enough to see the right people, and
  // broad enough that a free deck stays full in a small user base.
  //
  // MindMingle+: everything below. Narrowing a deck by trade, skill level, interests, language,
  // lifestyle answers or distance is the paid feature.
  //
  // The sheet hides these from free users, but the decision has to be made here too — a modified
  // client can send whatever it likes. The subscription read is skipped when no paid criteria
  // were sent, so a free user's deck costs no extra reads.
  let interests = asStringArray(data.interests);
  let occupations = asStringArray(data.occupations);
  let experienceLevels = asStringArray(data.experienceLevels);
  let languages = asStringArray(data.languages);
  let detailFilters = asFilterMap(data.detailFilters);
  let countries = asStringArray(data.countries).map((code) => code.trim().toUpperCase()).filter(Boolean);
  let districts = asStringArray(data.districts).map((name) => name.trim().toLowerCase()).filter(Boolean);
  let maxDistanceKm = Number.isFinite(data.maxDistanceKm) ? Number(data.maxDistanceKm) : null;

  const usesPremiumFilters =
    maxDistanceKm !== null ||
    countries.length > 0 ||
    districts.length > 0 ||
    languages.length > 0 ||
    interests.length > 0 ||
    occupations.length > 0 ||
    experienceLevels.length > 0 ||
    Object.keys(detailFilters).length > 0;

  if (usesPremiumFilters && !(await isCallerPremium(callerUid))) {
    maxDistanceKm = null;
    countries = [];
    districts = [];
    languages = [];
    interests = [];
    occupations = [];
    experienceLevels = [];
    detailFilters = {};
  }

  // Own coordinates. Read for every call now, not only when a distance filter is set: the deck
  // shows "N km away" on every card, and that number is computed here so the client never has to
  // be told where anybody actually is (see stripLocation below).
  let callerLat: number | null = null;
  let callerLng: number | null = null;
  {
    const callerDoc = await db.collection("users").doc(callerUid).get();
    const caller = callerDoc.data();
    if (typeof caller?.latitude === "number" && typeof caller?.longitude === "number") {
      callerLat = caller.latitude;
      callerLng = caller.longitude;
    }
  }

  const applyDistance = maxDistanceKm !== null && callerLat !== null && callerLng !== null;
  const deckSize = clamp(Number(data.limit ?? DEFAULT_DECK_SIZE), 1, MAX_DECK_SIZE);
  const requestedCursor = typeof data.cursor === "string" ? data.cursor : "";

  const baseQuery = db
    .collection("users")
    .where("isProfileComplete", "==", true)
    .orderBy(admin.firestore.FieldPath.documentId());

  /**
   * Whether this profile can be shown at all — self, disabled, deleting and paused accounts are
   * never candidates, no matter how empty the deck gets.
   */
  const isShowable = (user: FirebaseFirestore.DocumentData): boolean => {
    if (user.uid === callerUid) return false;
    if (user.isDisabled) return false;
    // Self-service account states: a break in progress, or a filed deletion request.
    if (user.isDeletionRequested) return false;
    if (user.isDeactivated && Date.now() < Number(user.reactivateAt ?? 0)) return false;
    return true;
  };

  /** Every filter the caller actually set. */
  const matchesExactly = (user: FirebaseFirestore.DocumentData): boolean => {
    if (typeof user.age === "number" && user.age > 0 && (user.age < minAge || user.age > maxAge)) {
      return false;
    }

    if (interests.length > 0) {
      const userInterests: string[] = asStringArray(user.interests);
      if (!userInterests.some((interest) => interests.includes(interest))) return false;
    }

    if (lookingFor.length > 0 && !lookingFor.includes(user.lookingFor)) return false;
    if (occupations.length > 0 && !occupations.includes(user.occupation)) return false;
    if (countries.length > 0 && !countries.includes(countryCodeOf(user))) return false;
    // Compared case-insensitively: the names come from the device geocoder on one side and from
    // the geoBoundaries extract on the other, and the two disagree about capitalisation far more
    // often than they disagree about the district.
    if (districts.length > 0 && !districts.includes(String(user.district ?? "").trim().toLowerCase())) {
      return false;
    }
    if (experienceLevels.length > 0 && !experienceLevels.includes(user.experienceLevel)) return false;

    if (!matchesProfileAnswer(user, "gender", genders)) return false;
    if (!matchesProfileAnswer(user, "languages", languages)) return false;

    for (const [key, values] of Object.entries(detailFilters)) {
      if (!matchesProfileAnswer(user, key, values)) return false;
    }

    if (applyDistance) {
      if (typeof user.latitude !== "number" || typeof user.longitude !== "number") return false;
      const distance = haversineKm(callerLat as number, callerLng as number, user.latitude, user.longitude);
      if (distance > (maxDistanceKm as number)) return false;
    }

    return true;
  };

  /**
   * "Close enough": the structural filters still hold (age, gender, who they're looking for) but
   * the softer taste filters — interests, languages, occupation, experience, lifestyle answers,
   * distance — are ignored. These fill the deck when an exact search comes up short.
   */
  const matchesLoosely = (user: FirebaseFirestore.DocumentData): boolean => {
    if (typeof user.age === "number" && user.age > 0 && (user.age < minAge || user.age > maxAge)) {
      return false;
    }
    if (lookingFor.length > 0 && !lookingFor.includes(user.lookingFor)) return false;
    return matchesProfileAnswer(user, "gender", genders);
  };

  // Candidates are sorted into three tiers during a single scan, so the fallbacks cost no extra
  // reads: an exact match is used first, then a close-enough one, then anyone showable. A deck
  // never comes back empty because a filter was too narrow.
  const exact: FirebaseFirestore.DocumentData[] = [];
  const related: FirebaseFirestore.DocumentData[] = [];
  const anyone: FirebaseFirestore.DocumentData[] = [];

  let cursor = requestedCursor || randomDocumentId();
  let scanned = 0;
  let wrapped = requestedCursor.length > 0; // an explicit cursor means "continue", never wrap twice
  let lastSeen = "";

  while (exact.length < deckSize && scanned < SCAN_CAP) {
    // An empty cursor means "from the very beginning" (the wrap case below). It must not be
    // handed to startAfter: the SDK resolves "" against the collection and throws
    // "Only a direct child can be used as a query boundary. Found: users", which surfaced in
    // the client as a bare INTERNAL from the callable.
    const scan = cursor ? baseQuery.startAfter(cursor) : baseQuery;
    const page = await scan.limit(SCAN_PAGE_SIZE).get();

    if (page.empty) {
      if (wrapped) break;
      // Ran off the end of the id range — carry on from the start, once.
      wrapped = true;
      cursor = "";
      continue;
    }

    for (const doc of page.docs) {
      scanned++;
      lastSeen = doc.id;
      const user = doc.data();
      if (!isShowable(user)) continue;

      if (matchesExactly(user)) {
        exact.push(user);
        if (exact.length >= deckSize) break;
      } else if (matchesLoosely(user)) {
        if (related.length < deckSize) related.push(user);
      } else if (anyone.length < deckSize) {
        anyone.push(user);
      }
    }

    cursor = page.docs[page.docs.length - 1].id;
  }

  const collected = [...exact];
  let relaxed = false;
  for (const pool of [related, anyone]) {
    for (const user of pool) {
      if (collected.length >= deckSize) break;
      collected.push(user);
      relaxed = true;
    }
  }

  const profiles = collected.map((user) => stripLocation(user, callerLat, callerLng));

  return {
    profiles,
    // Blank cursor = this scan reached the end; the client starts a fresh random scan, which is
    // also how someone with a tiny user base is shown profiles they have already seen rather
    // than an empty screen.
    cursor: exact.length < deckSize && scanned < SCAN_CAP ? "" : lastSeen,
    // True when the deck had to reach past the caller's filters to fill up.
    relaxed,
  };
});
