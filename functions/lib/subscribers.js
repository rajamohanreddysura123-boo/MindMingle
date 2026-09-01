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
exports.adminSubscriberStats = exports.adminListSubscribers = void 0;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
const pricing_1 = require("./pricing");
/**
 * The admin subscriber list — everyone who ever paid, filterable and paged.
 *
 * This has to run server-side because the answer lives in two places: plan state is in
 * `subscriptions/{uid}` and the person's name and email are in `users/{uid}`. Doing the join on
 * the client would mean a profile read per row, and searching by name would mean reading every
 * subscription in the project to find the one row an admin was looking for.
 *
 * Paging works the way the Discover deck's does: scan `subscriptions` in document-id order from
 * a cursor, apply the filters in memory, and stop as soon as the page is full or SCAN_CAP
 * documents have been examined. Nothing here needs a composite index, which matters because the
 * filter combinations are chosen by whoever is looking at the screen and there is no useful set
 * of indexes to pre-declare for that.
 */
const db = () => admin.firestore();
/** How many subscription documents one call may read, regardless of how many pass the filters. */
const SCAN_CAP = 500;
const SCAN_PAGE_SIZE = 100;
const DEFAULT_PAGE_SIZE = 25;
const MAX_PAGE_SIZE = 100;
function clamp(value, min, max) {
    if (!Number.isFinite(value))
        return min;
    return Math.min(Math.max(Math.trunc(value), min), max);
}
async function isAdmin(uid) {
    const doc = await db().collection("users").doc(uid).get();
    return doc.get("userType") === "admin";
}
/**
 * Whether a subscription counts as live right now. A cancelled plan is still active until the
 * period it was paid for runs out — that is what "cancel at period end" means — while a revoked
 * one ended the moment an admin ended it.
 */
function isActiveNow(data, now) {
    if (data.status === "revoked")
        return false;
    return Number(data.currentPeriodEnd ?? 0) > now;
}
function matchesStatus(data, filter, now) {
    const active = isActiveNow(data, now);
    switch (filter) {
        case "active":
            return active;
        case "expired":
            return !active && data.status !== "revoked";
        case "cancelled":
            return data.status === "cancelled";
        case "revoked":
            return data.status === "revoked";
        default:
            return true;
    }
}
exports.adminListSubscribers = (0, https_1.onCall)(async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid || !(await isAdmin(callerUid))) {
        throw new https_1.HttpsError("permission-denied", "Admins only");
    }
    const data = request.data ?? {};
    const status = String(data.status ?? "all") || "all";
    const planId = String(data.planId ?? "").trim();
    const country = String(data.country ?? "").trim().toUpperCase();
    const query = String(data.query ?? "").trim().toLowerCase();
    const pageSize = clamp(Number(data.pageSize ?? DEFAULT_PAGE_SIZE), 1, MAX_PAGE_SIZE);
    const cursor = String(data.cursor ?? "");
    if (planId && !pricing_1.PLAN_DAYS[planId]) {
        throw new https_1.HttpsError("invalid-argument", `Unknown plan ${planId}`);
    }
    const now = Date.now();
    const baseQuery = db()
        .collection("subscriptions")
        .orderBy(admin.firestore.FieldPath.documentId());
    const rows = [];
    let scanned = 0;
    let lastSeen = cursor;
    let exhausted = false;
    while (rows.length < pageSize && scanned < SCAN_CAP) {
        let page = baseQuery.limit(SCAN_PAGE_SIZE);
        if (lastSeen)
            page = baseQuery.startAfter(lastSeen).limit(SCAN_PAGE_SIZE);
        const snapshot = await page.get();
        if (snapshot.empty) {
            exhausted = true;
            break;
        }
        scanned += snapshot.size;
        lastSeen = snapshot.docs[snapshot.docs.length - 1].id;
        // Everything that survives the cheap checks, before spending a read on the profile.
        const candidates = snapshot.docs.filter((doc) => {
            const sub = doc.data();
            if (!matchesStatus(sub, status, now))
                return false;
            if (planId && sub.planId !== planId)
                return false;
            if (country && String(sub.billingCountry ?? "").toUpperCase() !== country)
                return false;
            return true;
        });
        // Profiles are fetched one batch per page rather than one per row: getAll is a single
        // round trip, and the name is needed both for display and for the search filter.
        const profiles = candidates.length
            ? await db().getAll(...candidates.map((doc) => db().collection("users").doc(doc.id)))
            : [];
        const profileByUid = new Map(profiles.map((snap) => [snap.id, snap.data() ?? {}]));
        for (const doc of candidates) {
            if (rows.length >= pageSize)
                break;
            const sub = doc.data();
            const user = profileByUid.get(doc.id) ?? {};
            const name = String(user.name ?? "");
            const email = String(user.email ?? "");
            // Substring search across name, email and uid. Firestore cannot do this natively — it
            // has no substring operator — so it happens here, against the page just read.
            if (query) {
                const haystack = `${name} ${email} ${doc.id}`.toLowerCase();
                if (!haystack.includes(query))
                    continue;
            }
            rows.push({
                uid: doc.id,
                name,
                email,
                photoUrl: Array.isArray(user.photoUrls) ? String(user.photoUrls[0] ?? "") : "",
                planId: String(sub.planId ?? ""),
                status: String(sub.status ?? ""),
                currentPeriodEnd: Number(sub.currentPeriodEnd ?? 0),
                billingCountry: String(sub.billingCountry ?? ""),
                billingCurrency: String(sub.billingCurrency ?? ""),
                lastPaymentId: String(sub.lastPaymentId ?? ""),
                lastInvoiceNumber: String(sub.lastInvoiceNumber ?? ""),
                updatedAt: Number(sub.updatedAt ?? 0),
                isActive: isActiveNow(sub, now),
                isPaid: String(sub.provider ?? "") === "razorpay",
            });
        }
        if (snapshot.size < SCAN_PAGE_SIZE) {
            exhausted = true;
            break;
        }
    }
    return {
        subscribers: rows,
        // Blank means there is nothing left to page through. A cursor with a short page still
        // means "ask again" — the filters may simply have rejected everything in that slice.
        cursor: exhausted ? "" : lastSeen,
        scanned,
    };
});
/**
 * Headline counts for the subscriber screen. Deliberately a separate call: it reads the whole
 * collection, so the list can page cheaply while this is fetched once when the screen opens.
 */
exports.adminSubscriberStats = (0, https_1.onCall)(async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid || !(await isAdmin(callerUid))) {
        throw new https_1.HttpsError("permission-denied", "Admins only");
    }
    const now = Date.now();
    const snapshot = await db().collection("subscriptions").get();
    let active = 0;
    let expired = 0;
    let cancelled = 0;
    let revoked = 0;
    snapshot.docs.forEach((doc) => {
        const sub = doc.data();
        if (sub.status === "revoked") {
            revoked += 1;
            return;
        }
        if (sub.status === "cancelled")
            cancelled += 1;
        if (isActiveNow(sub, now))
            active += 1;
        else
            expired += 1;
    });
    return { total: snapshot.size, active, expired, cancelled, revoked };
});
//# sourceMappingURL=subscribers.js.map