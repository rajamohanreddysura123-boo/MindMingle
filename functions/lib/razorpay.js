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
exports.razorpayWebhook = exports.adminCancelSubscription = exports.adminSetSubscription = exports.recordPaymentFailure = exports.getBillingHistory = exports.verifyRazorpayPayment = exports.createRazorpayOrder = void 0;
const admin = __importStar(require("firebase-admin"));
const crypto = __importStar(require("node:crypto"));
const https_1 = require("firebase-functions/v2/https");
const params_1 = require("firebase-functions/params");
const logger = __importStar(require("firebase-functions/logger"));
const notifications_1 = require("./notifications");
const pricing_1 = require("./pricing");
/**
 * Razorpay checkout for MindMingle+ (see PremiumScreen / PremiumViewModel on the client).
 *
 * Nothing about money is trusted from the client: the client asks for a plan id,
 * the server decides the amount, creates the order, and — after checkout — verifies
 * the payment signature with the key secret before granting the plan. The plan itself
 * lives in `subscriptions/{uid}`, which no client can write (firestore.rules).
 *
 * Secrets (set once per project, never committed):
 *   firebase functions:secrets:set RAZORPAY_KEY_ID
 *   firebase functions:secrets:set RAZORPAY_KEY_SECRET
 *   firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET
 */
const razorpayKeyId = (0, params_1.defineSecret)("RAZORPAY_KEY_ID");
const razorpayKeySecret = (0, params_1.defineSecret)("RAZORPAY_KEY_SECRET");
const razorpayWebhookSecret = (0, params_1.defineSecret)("RAZORPAY_WEBHOOK_SECRET");
const RAZORPAY_API = "https://api.razorpay.com/v1";
const db = () => admin.firestore();
const PLANS_DOC = db().collection("appConfig").doc("plans");
/**
 * The live price list. `appConfig/plans` is editable by an admin from the in-app Plan
 * Pricing screen; a fresh project falls back to the seed table and writes it back so the
 * admin screen has something to edit.
 */
async function loadCatalog() {
    const snap = await PLANS_DOC.get();
    const data = snap.data();
    if (!data?.countries || Object.keys(data.countries).length === 0) {
        await PLANS_DOC.set(pricing_1.DEFAULT_PLAN_CATALOG, { merge: true });
        return pricing_1.DEFAULT_PLAN_CATALOG;
    }
    return {
        enabled: data.enabled !== false,
        defaultCountry: data.defaultCountry || pricing_1.DEFAULT_PLAN_CATALOG.defaultCountry,
        countries: data.countries,
    };
}
/**
 * Which market this user is billed in. Their stored phone number decides it — never the
 * client, or anyone could pick the cheapest country from a patched app. The client hint is
 * only consulted for accounts with no phone on file (email sign-in).
 */
async function resolveCountry(uid, catalog, hint) {
    const userSnap = await db().collection("users").doc(uid).get();
    const phoneNumber = String(userSnap.data()?.phoneNumber ?? "");
    const fromPhone = phoneNumber ? (0, pricing_1.countryFromPhone)(catalog, phoneNumber) : undefined;
    if (fromPhone)
        return fromPhone;
    const normalizedHint = hint.trim().toUpperCase();
    if (normalizedHint && catalog.countries[normalizedHint])
        return normalizedHint;
    return catalog.defaultCountry;
}
async function isAdmin(uid) {
    return (await db().collection("admins").doc(uid).get()).exists;
}
function authHeader() {
    const pair = `${razorpayKeyId.value()}:${razorpayKeySecret.value()}`;
    return `Basic ${Buffer.from(pair).toString("base64")}`;
}
async function razorpayFetch(path, init) {
    const response = await fetch(`${RAZORPAY_API}${path}`, {
        method: init.method,
        headers: {
            Authorization: authHeader(),
            "Content-Type": "application/json",
        },
        body: init.body === undefined ? undefined : JSON.stringify(init.body),
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        const message = payload?.error?.description ?? `Razorpay ${path} failed (${response.status})`;
        logger.error("razorpay api error", { path, status: response.status, payload });
        throw new https_1.HttpsError("internal", message);
    }
    return payload;
}
/**
 * Extends the plan from whichever is later — now, or the end of the period already
 * paid for — so a renewal bought early never eats the remaining days.
 */
function nextPeriodEnd(currentEnd, days) {
    const base = currentEnd && currentEnd > Date.now() ? currentEnd : Date.now();
    return base + days * 24 * 60 * 60 * 1000;
}
/**
 * Grants (or extends) the plan for `uid`. Idempotent per paymentId: the payment doc is
 * created inside the transaction, so a webhook and a client verify racing on the same
 * payment can only grant once.
 */
async function grantPlan(args) {
    const days = pricing_1.PLAN_DAYS[args.planId];
    if (!days) {
        throw new https_1.HttpsError("invalid-argument", `Unknown plan ${args.planId}`);
    }
    const subscriptionRef = db().collection("subscriptions").doc(args.uid);
    const paymentRef = subscriptionRef.collection("payments").doc(args.paymentId);
    const result = await db().runTransaction(async (tx) => {
        const [subscriptionSnap, paymentSnap] = await Promise.all([
            tx.get(subscriptionRef),
            tx.get(paymentRef),
        ]);
        const current = subscriptionSnap.data();
        if (paymentSnap.exists) {
            return {
                planId: current?.planId ?? args.planId,
                currentPeriodEnd: current?.currentPeriodEnd ?? 0,
                granted: false,
            };
        }
        const currentPeriodEnd = nextPeriodEnd(current?.currentPeriodEnd, days);
        tx.set(subscriptionRef, {
            uid: args.uid,
            provider: "razorpay",
            planId: args.planId,
            status: "active",
            currentPeriodEnd,
            lastPaymentId: args.paymentId,
            lastOrderId: args.orderId,
            billingCountry: args.country,
            billingCurrency: args.currency,
            updatedAt: Date.now(),
        }, { merge: true });
        tx.set(paymentRef, {
            paymentId: args.paymentId,
            orderId: args.orderId,
            planId: args.planId,
            amount: args.amount,
            currency: args.currency,
            country: args.country,
            source: args.source,
            createdAt: Date.now(),
        });
        return { planId: args.planId, currentPeriodEnd, granted: true };
    });
    // Only the write that actually granted notifies — a webhook replaying a payment the client
    // already verified must not push a second "payment successful".
    if (result.granted) {
        await (0, notifications_1.notifyPaymentSucceeded)({
            uid: args.uid,
            planId: result.planId,
            currentPeriodEnd: result.currentPeriodEnd,
            isAdminGrant: args.source === "admin-grant",
        });
    }
    return result;
}
/**
 * Step 1 of checkout — the client sends only a plan id and gets back an order to open
 * the Razorpay SDK with. Amount, currency and country are decided here; `keyId` is the
 * publishable key and the secret never leaves this function.
 */
exports.createRazorpayOrder = (0, https_1.onCall)({ secrets: [razorpayKeyId, razorpayKeySecret] }, async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new https_1.HttpsError("unauthenticated", "Sign in before starting a payment");
    }
    const planId = String(request.data?.planId ?? "");
    if (!pricing_1.PLAN_DAYS[planId]) {
        throw new https_1.HttpsError("invalid-argument", "Unknown plan");
    }
    const catalog = await loadCatalog();
    if (!catalog.enabled) {
        throw new https_1.HttpsError("failed-precondition", "Upgrades are currently unavailable");
    }
    const country = await resolveCountry(uid, catalog, String(request.data?.countryHint ?? ""));
    const pricing = (0, pricing_1.pricingFor)(catalog, country);
    if (!pricing || !(0, pricing_1.isValidPricing)(pricing)) {
        throw new https_1.HttpsError("failed-precondition", "No price is configured for your country");
    }
    const amount = planId === "plus_annual" ? pricing.annual : pricing.monthly;
    const order = await razorpayFetch("/orders", {
        method: "POST",
        body: {
            amount,
            currency: pricing.currency,
            // Razorpay caps receipt at 40 chars; a uid is 28.
            receipt: `oo_${uid}`.slice(0, 40),
            notes: { uid, planId, country },
        },
    });
    return {
        orderId: String(order.id),
        amount: Number(order.amount),
        currency: String(order.currency),
        keyId: razorpayKeyId.value(),
        planId,
        country,
        symbol: pricing.symbol,
        decimals: pricing.decimals,
    };
});
/**
 * Step 2 — the client hands back what the SDK returned. The signature proves Razorpay
 * (not the client) produced this payment for this order; the payment is then re-read
 * from the API so a replayed-but-unpaid signature still cannot buy a plan.
 */
exports.verifyRazorpayPayment = (0, https_1.onCall)({ secrets: [razorpayKeyId, razorpayKeySecret] }, async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new https_1.HttpsError("unauthenticated", "Sign in before verifying a payment");
    }
    const orderId = String(request.data?.orderId ?? "");
    const paymentId = String(request.data?.paymentId ?? "");
    const signature = String(request.data?.signature ?? "");
    if (!orderId || !paymentId || !signature) {
        throw new https_1.HttpsError("invalid-argument", "orderId, paymentId and signature are required");
    }
    const expected = crypto
        .createHmac("sha256", razorpayKeySecret.value())
        .update(`${orderId}|${paymentId}`)
        .digest("hex");
    const expectedBuffer = Buffer.from(expected, "utf8");
    const actualBuffer = Buffer.from(signature, "utf8");
    const signatureOk = expectedBuffer.length === actualBuffer.length &&
        crypto.timingSafeEqual(expectedBuffer, actualBuffer);
    if (!signatureOk) {
        logger.warn("razorpay signature mismatch", { uid, orderId, paymentId });
        throw new https_1.HttpsError("permission-denied", "Payment signature check failed");
    }
    // The order Razorpay holds is the authority on what was owed — not the catalog, which
    // an admin may have edited between checkout opening and this call.
    const order = await razorpayFetch(`/orders/${orderId}`, { method: "GET" });
    const planId = String(order?.notes?.planId ?? "");
    if (!pricing_1.PLAN_DAYS[planId]) {
        throw new https_1.HttpsError("failed-precondition", "Order is not for a known plan");
    }
    if (String(order?.notes?.uid ?? "") !== uid) {
        throw new https_1.HttpsError("permission-denied", "Order belongs to another account");
    }
    const orderAmount = Number(order.amount);
    const orderCurrency = String(order.currency);
    const country = String(order?.notes?.country ?? "");
    let payment = await razorpayFetch(`/payments/${paymentId}`, { method: "GET" });
    if (String(payment.order_id) !== orderId) {
        throw new https_1.HttpsError("permission-denied", "Payment does not belong to this order");
    }
    if (Number(payment.amount) !== orderAmount || String(payment.currency) !== orderCurrency) {
        throw new https_1.HttpsError("permission-denied", "Payment amount does not match the order");
    }
    // Auto-capture is the default on Razorpay accounts, but an "authorized" payment
    // would silently expire in 5 days, so capture it here rather than trusting config.
    if (payment.status === "authorized") {
        payment = await razorpayFetch(`/payments/${paymentId}/capture`, {
            method: "POST",
            body: { amount: orderAmount, currency: orderCurrency },
        });
    }
    if (payment.status !== "captured") {
        throw new https_1.HttpsError("failed-precondition", `Payment is ${payment.status}, not captured`);
    }
    const granted = await grantPlan({
        uid,
        planId,
        paymentId,
        orderId,
        amount: orderAmount,
        currency: orderCurrency,
        country,
        source: "client-verify",
    });
    return { planId: granted.planId, currentPeriodEnd: granted.currentPeriodEnd, status: "active" };
});
/**
 * Order history. A user sees their own; an admin can pass any uid to see someone else's,
 * which is what backs the subscription panel in the admin user detail screen.
 */
exports.getBillingHistory = (0, https_1.onCall)(async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid) {
        throw new https_1.HttpsError("unauthenticated", "Sign in first");
    }
    const requestedUid = String(request.data?.uid ?? "").trim();
    const uid = requestedUid || callerUid;
    if (uid !== callerUid && !(await isAdmin(callerUid))) {
        throw new https_1.HttpsError("permission-denied", "Admins only");
    }
    const subscriptionSnap = await db().collection("subscriptions").doc(uid).get();
    const subscription = subscriptionSnap.data();
    const paymentsSnap = await db()
        .collection("subscriptions")
        .doc(uid)
        .collection("payments")
        .orderBy("createdAt", "desc")
        .limit(50)
        .get();
    return {
        uid,
        planId: String(subscription?.planId ?? ""),
        status: String(subscription?.status ?? ""),
        currentPeriodEnd: Number(subscription?.currentPeriodEnd ?? 0),
        billingCountry: String(subscription?.billingCountry ?? ""),
        billingCurrency: String(subscription?.billingCurrency ?? ""),
        payments: paymentsSnap.docs.map((doc) => {
            const data = doc.data();
            return {
                paymentId: String(data.paymentId ?? doc.id),
                orderId: String(data.orderId ?? ""),
                planId: String(data.planId ?? ""),
                amount: Number(data.amount ?? 0),
                currency: String(data.currency ?? ""),
                country: String(data.country ?? ""),
                source: String(data.source ?? ""),
                createdAt: Number(data.createdAt ?? 0),
            };
        }),
    };
});
/**
 * Records a checkout that failed on the device, and tells the user no money was taken.
 *
 * Nothing is granted here and nothing is trusted: the client reports only its own order id, the
 * plan it was buying and a reason string, and the write lands under the caller's own uid. A client
 * that lies about this achieves nothing beyond a wrong row in its own history.
 *
 * It exists because a decline that leaves no trace is indistinguishable from a bug when the user
 * writes in: the attempt row is what support reads, and `firestore.rules` makes it owner-readable
 * and Functions-only writable for exactly that reason.
 *
 * Reconstructed 2026-09-01 after the original was lost to a bad `git checkout`. The contract comes
 * from its caller (MindMingleFirebaseProvider.recordPaymentFailure) and from the collection the
 * rules already documented; the shape it writes matches the payment rows beside it.
 */
exports.recordPaymentFailure = (0, https_1.onCall)(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new https_1.HttpsError("unauthenticated", "Sign in required");
    }
    const orderId = String(request.data?.orderId ?? "").trim();
    const planId = String(request.data?.planId ?? "").trim();
    const reason = String(request.data?.reason ?? "").trim().slice(0, 500);
    if (!orderId) {
        throw new https_1.HttpsError("invalid-argument", "orderId is required");
    }
    await db()
        .collection("subscriptions")
        .doc(uid)
        .collection("paymentAttempts")
        .doc(orderId)
        .set({
        uid,
        orderId,
        planId,
        reason,
        status: "failed",
        createdAt: Date.now(),
    }, 
    // merge: a retry of the same order updates the row rather than stacking a second one.
    { merge: true });
    // Best-effort: the user has just watched a payment fail, and a notification that itself fails
    // must not turn into an error on top of it.
    try {
        await (0, notifications_1.notifyPaymentFailed)({ uid, reason });
    }
    catch (error) {
        logger.warn("payment failure notification not sent", { uid, orderId, error });
    }
    logger.info("payment failure recorded", { uid, orderId, planId });
    return { recorded: true };
});
/**
 * Admin-granted plan — comps, support gestures, refund make-goods. No money moves, so it is
 * recorded as its own zero-amount entry in the same history the user sees.
 */
exports.adminSetSubscription = (0, https_1.onCall)(async (request) => {
    const adminUid = request.auth?.uid;
    if (!adminUid || !(await isAdmin(adminUid))) {
        throw new https_1.HttpsError("permission-denied", "Admins only");
    }
    const uid = String(request.data?.uid ?? "");
    const planId = String(request.data?.planId ?? "");
    if (!uid || !pricing_1.PLAN_DAYS[planId]) {
        throw new https_1.HttpsError("invalid-argument", "uid and a known planId are required");
    }
    const days = Number(request.data?.days ?? pricing_1.PLAN_DAYS[planId]);
    if (!Number.isFinite(days) || days <= 0 || days > 3650) {
        throw new https_1.HttpsError("invalid-argument", "days must be between 1 and 3650");
    }
    const subscriptionRef = db().collection("subscriptions").doc(uid);
    const current = (await subscriptionRef.get()).data();
    const currentPeriodEnd = nextPeriodEnd(current?.currentPeriodEnd, days);
    const grantId = `admin_${Date.now()}`;
    await subscriptionRef.set({
        uid,
        provider: "admin",
        planId,
        status: "active",
        currentPeriodEnd,
        lastPaymentId: grantId,
        updatedAt: Date.now(),
        grantedBy: adminUid,
    }, { merge: true });
    await subscriptionRef.collection("payments").doc(grantId).set({
        paymentId: grantId,
        orderId: "",
        planId,
        amount: 0,
        currency: "",
        country: "",
        source: "admin-grant",
        grantedBy: adminUid,
        createdAt: Date.now(),
    });
    await (0, notifications_1.notifyPaymentSucceeded)({ uid, planId, currentPeriodEnd, isAdminGrant: true });
    logger.info("admin granted subscription", { uid, planId, days, adminUid });
    return { planId, currentPeriodEnd, status: "active" };
});
/**
 * Cancels a plan. By default access runs to the end of the period already paid for;
 * `immediate: true` ends it now, which also switches ads back on for that user.
 */
exports.adminCancelSubscription = (0, https_1.onCall)(async (request) => {
    const adminUid = request.auth?.uid;
    if (!adminUid || !(await isAdmin(adminUid))) {
        throw new https_1.HttpsError("permission-denied", "Admins only");
    }
    const uid = String(request.data?.uid ?? "");
    if (!uid) {
        throw new https_1.HttpsError("invalid-argument", "uid is required");
    }
    const immediate = request.data?.immediate === true;
    const subscriptionRef = db().collection("subscriptions").doc(uid);
    const current = (await subscriptionRef.get()).data();
    if (!current) {
        throw new https_1.HttpsError("not-found", "That account has no subscription");
    }
    const currentPeriodEnd = immediate ? Date.now() : Number(current.currentPeriodEnd ?? 0);
    await subscriptionRef.set({
        status: immediate ? "revoked" : "cancelled",
        currentPeriodEnd,
        updatedAt: Date.now(),
        cancelledBy: adminUid,
    }, { merge: true });
    logger.info("admin cancelled subscription", { uid, immediate, adminUid });
    return { status: immediate ? "revoked" : "cancelled", currentPeriodEnd };
});
/**
 * Safety net for the case where the money moved but the app never got to call
 * verifyRazorpayPayment (killed app, dead network, user closed the sheet mid-redirect).
 * Point a Razorpay webhook at this URL for the `payment.captured` event.
 */
exports.razorpayWebhook = (0, https_1.onRequest)({ secrets: [razorpayWebhookSecret] }, async (request, response) => {
    const signature = String(request.headers["x-razorpay-signature"] ?? "");
    const body = request.rawBody?.toString("utf8") ?? "";
    const expected = crypto
        .createHmac("sha256", razorpayWebhookSecret.value())
        .update(body)
        .digest("hex");
    const expectedBuffer = Buffer.from(expected, "utf8");
    const actualBuffer = Buffer.from(signature, "utf8");
    const signatureOk = expectedBuffer.length === actualBuffer.length &&
        crypto.timingSafeEqual(expectedBuffer, actualBuffer);
    if (!signatureOk) {
        logger.warn("razorpay webhook signature mismatch");
        response.status(401).send("invalid signature");
        return;
    }
    const event = JSON.parse(body || "{}");
    if (event.event === "payment.failed") {
        const failed = event.payload?.payment?.entity ?? {};
        const failedUid = String(failed.notes?.uid ?? "");
        if (failedUid) {
            await (0, notifications_1.notifyPaymentFailed)({
                uid: failedUid,
                reason: String(failed.error_description ?? ""),
            });
        }
        response.status(200).send("ok");
        return;
    }
    if (event.event !== "payment.captured") {
        response.status(200).send("ignored");
        return;
    }
    const payment = event.payload?.payment?.entity ?? {};
    const uid = String(payment.notes?.uid ?? "");
    const planId = String(payment.notes?.planId ?? "");
    if (!uid || !pricing_1.PLAN_DAYS[planId]) {
        logger.warn("razorpay webhook without a usable uid/plan note", { uid, planId });
        response.status(200).send("ignored");
        return;
    }
    // The captured amount is what actually landed in the account, in whatever currency the
    // order was placed in — recorded as-is rather than re-checked against the price list.
    await grantPlan({
        uid,
        planId,
        paymentId: String(payment.id),
        orderId: String(payment.order_id ?? ""),
        amount: Number(payment.amount ?? 0),
        currency: String(payment.currency ?? ""),
        country: String(payment.notes?.country ?? ""),
        source: "webhook",
    });
    response.status(200).send("ok");
});
//# sourceMappingURL=razorpay.js.map