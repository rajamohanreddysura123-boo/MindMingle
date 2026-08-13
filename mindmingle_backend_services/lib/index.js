"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.filterDiscoverProfiles = void 0;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
admin.initializeApp();
const db = admin.firestore();
/** Great-circle distance between two lat/long points, in kilometers. Mirrors haversineKm in HomeContract.kt. */
function haversineKm(lat1, lng1, lat2, lng2) {
    const earthRadiusKm = 6371.0;
    const dLat = ((lat2 - lat1) * Math.PI) / 180.0;
    const dLng = ((lng2 - lng1) * Math.PI) / 180.0;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos((lat1 * Math.PI) / 180.0) *
            Math.cos((lat2 * Math.PI) / 180.0) *
            Math.sin(dLng / 2) *
            Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
}
function asStringArray(value) {
    return Array.isArray(value) ? value.filter((entry) => typeof entry === "string") : [];
}
/**
 * Lifestyle/intent filters arrive keyed by profile_options.json field key. Entries that aren't
 * string arrays, and keys narrowed to nothing, are dropped so they can't match-all or match-none
 * by accident.
 */
function asFilterMap(value) {
    if (typeof value !== "object" || value === null || Array.isArray(value))
        return {};
    const result = {};
    for (const [key, entry] of Object.entries(value)) {
        const values = asStringArray(entry);
        if (values.length > 0)
            result[key] = values;
    }
    return result;
}
/**
 * Mirrors Subscription.isActiveAt in the Kotlin model: a cancelled plan still counts until the
 * period already paid for runs out, a revoked one stops immediately. `subscriptions/{uid}` is
 * server-written only (firestore.rules), so this cannot be faked client-side.
 */
async function isCallerPremium(uid) {
    const snapshot = await db.collection("subscriptions").doc(uid).get();
    const subscription = snapshot.data();
    if (!subscription)
        return false;
    const currentPeriodEnd = typeof subscription.currentPeriodEnd === "number" ? subscription.currentPeriodEnd : 0;
    return currentPeriodEnd > Date.now() && subscription.status !== "revoked";
}
/**
 * True when `requested` is empty (filter off) or the profile's answer for `key`
 * intersects it. Profile-setup answers land in two shapes: single-select fields
 * in `details` (string) and multi-select fields in `selections` (string[]), so
 * both are read through here.
 */
function matchesProfileAnswer(user, key, requested) {
    if (requested.length === 0)
        return true;
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
exports.filterDiscoverProfiles = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Sign in required");
    }
    const callerUid = request.auth.uid;
    const data = (request.data ?? {});
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
    let callerLat = null;
    let callerLng = null;
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
        if (user.uid === callerUid)
            return false;
        if (user.isDisabled)
            return false;
        if (typeof user.age === "number" && user.age > 0 && (user.age < minAge || user.age > maxAge)) {
            return false;
        }
        if (interests.length > 0) {
            const userInterests = asStringArray(user.interests);
            if (!userInterests.some((interest) => interests.includes(interest)))
                return false;
        }
        if (lookingFor.length > 0 && !lookingFor.includes(user.lookingFor))
            return false;
        if (occupations.length > 0 && !occupations.includes(user.occupation))
            return false;
        if (experienceLevels.length > 0 && !experienceLevels.includes(user.experienceLevel))
            return false;
        if (!matchesProfileAnswer(user, "gender", genders))
            return false;
        if (!matchesProfileAnswer(user, "languages", languages))
            return false;
        for (const [key, values] of Object.entries(detailFilters)) {
            if (!matchesProfileAnswer(user, key, values))
                return false;
        }
        if (applyDistance) {
            if (typeof user.latitude !== "number" || typeof user.longitude !== "number")
                return false;
            const distance = haversineKm(callerLat, callerLng, user.latitude, user.longitude);
            if (distance > maxDistanceKm)
                return false;
        }
        return true;
    });
    return { profiles };
});
//# sourceMappingURL=index.js.map