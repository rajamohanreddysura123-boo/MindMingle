import * as admin from "firebase-admin";
import { HttpsError, onCall } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

interface FilterDiscoverProfilesRequest {
  minAge?: number;
  maxAge?: number;
  interests?: string[];
  lookingFor?: string[];
  occupations?: string[];
  maxDistanceKm?: number | null;
  genders?: string[];
  experienceLevels?: string[];
  languages?: string[];
  detailFilters?: Record<string, string[]>;
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
 * caller, disabled profiles and incomplete profiles, then applies age/distance/
 * interests/lookingFor/occupation/gender/experience/language filters in memory —
 * Firestore can't express a range filter plus multiple array-contains-any filters
 * in a single query, and the user base doesn't yet warrant maintaining composite
 * indexes for it.
 */
export const filterDiscoverProfiles = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  const callerUid = request.auth.uid;

  const data = (request.data ?? {}) as FilterDiscoverProfilesRequest;
  const minAge = Number.isFinite(data.minAge) ? Number(data.minAge) : 18;
  const maxAge = Number.isFinite(data.maxAge) ? Number(data.maxAge) : 70;
  const interests = asStringArray(data.interests);
  const lookingFor = asStringArray(data.lookingFor);
  const occupations = asStringArray(data.occupations);
  const genders = asStringArray(data.genders);
  const experienceLevels = asStringArray(data.experienceLevels);
  let languages = asStringArray(data.languages);
  let detailFilters = asFilterMap(data.detailFilters);
  let maxDistanceKm = Number.isFinite(data.maxDistanceKm) ? Number(data.maxDistanceKm) : null;

  // Distance, languages and the lifestyle/values set are MindMingle+ only. The sheet hides them
  // from free users, but the check has to live here too — a modified client can send anything.
  // The subscription read is skipped entirely when no premium criteria were sent.
  const usesPremiumFilters = maxDistanceKm !== null || languages.length > 0 || Object.keys(detailFilters).length > 0;
  if (usesPremiumFilters && !(await isCallerPremium(callerUid))) {
    maxDistanceKm = null;
    languages = [];
    detailFilters = {};
  }

  // Own coordinates, needed only when a distance filter is actually set.
  let callerLat: number | null = null;
  let callerLng: number | null = null;
  if (maxDistanceKm !== null) {
    const callerDoc = await db.collection("users").doc(callerUid).get();
    const caller = callerDoc.data();
    if (typeof caller?.latitude === "number" && typeof caller?.longitude === "number") {
      callerLat = caller.latitude;
      callerLng = caller.longitude;
    }
  }
  // A distance filter the caller has no coordinates for would drop every profile —
  // treat it as "any distance" instead of returning an empty deck.
  const applyDistance = maxDistanceKm !== null && callerLat !== null && callerLng !== null;

  const snapshot = await db.collection("users").where("isProfileComplete", "==", true).get();

  const profiles = snapshot.docs
    .map((doc) => doc.data())
    .filter((user) => {
      if (user.uid === callerUid) return false;
      if (user.isDisabled) return false;

      if (typeof user.age === "number" && user.age > 0 && (user.age < minAge || user.age > maxAge)) {
        return false;
      }

      if (interests.length > 0) {
        const userInterests: string[] = asStringArray(user.interests);
        if (!userInterests.some((interest) => interests.includes(interest))) return false;
      }

      if (lookingFor.length > 0 && !lookingFor.includes(user.lookingFor)) return false;
      if (occupations.length > 0 && !occupations.includes(user.occupation)) return false;
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
    });

  return { profiles };
});
