import * as admin from "firebase-admin";
import * as crypto from "node:crypto";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { notifyPaymentFailed, notifyPaymentSucceeded } from "./notifications";
import {
  CountryPricing,
  DEFAULT_PLAN_CATALOG,
  PLAN_DAYS,
  PlanCatalog,
  countryFromPhone,
  isValidPricing,
  pricingFor,
} from "./pricing";

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

const razorpayKeyId = defineSecret("RAZORPAY_KEY_ID");
const razorpayKeySecret = defineSecret("RAZORPAY_KEY_SECRET");
const razorpayWebhookSecret = defineSecret("RAZORPAY_WEBHOOK_SECRET");

const RAZORPAY_API = "https://api.razorpay.com/v1";

const db = () => admin.firestore();

const PLANS_DOC = db().collection("appConfig").doc("plans");

/**
 * The live price list. `appConfig/plans` is editable by an admin from the in-app Plan
 * Pricing screen; a fresh project falls back to the seed table and writes it back so the
 * admin screen has something to edit.
 */
async function loadCatalog(): Promise<PlanCatalog> {
  const snap = await PLANS_DOC.get();
  const data = snap.data() as PlanCatalog | undefined;

  if (!data?.countries || Object.keys(data.countries).length === 0) {
    await PLANS_DOC.set(DEFAULT_PLAN_CATALOG, { merge: true });
    return DEFAULT_PLAN_CATALOG;
  }

  return {
    enabled: data.enabled !== false,
    defaultCountry: data.defaultCountry || DEFAULT_PLAN_CATALOG.defaultCountry,
    countries: data.countries,
  };
}

/**
 * Which market this user is billed in. Their stored phone number decides it — never the
 * client, or anyone could pick the cheapest country from a patched app. The client hint is
 * only consulted for accounts with no phone on file (email sign-in).
 */
async function resolveCountry(uid: string, catalog: PlanCatalog, hint: string): Promise<string> {
  const userSnap = await db().collection("users").doc(uid).get();
  const phoneNumber = String(userSnap.data()?.phoneNumber ?? "");

  const fromPhone = phoneNumber ? countryFromPhone(catalog, phoneNumber) : undefined;
  if (fromPhone) return fromPhone;

  const normalizedHint = hint.trim().toUpperCase();
  if (normalizedHint && catalog.countries[normalizedHint]) return normalizedHint;

  return catalog.defaultCountry;
}

async function isAdmin(uid: string): Promise<boolean> {
  return (await db().collection("admins").doc(uid).get()).exists;
}

function authHeader(): string {
  const pair = `${razorpayKeyId.value()}:${razorpayKeySecret.value()}`;
  return `Basic ${Buffer.from(pair).toString("base64")}`;
}

async function razorpayFetch(
  path: string,
  init: { method: string; body?: unknown }
): Promise<any> {
  const response = await fetch(`${RAZORPAY_API}${path}`, {
    method: init.method,
    headers: {
      Authorization: authHeader(),
      "Content-Type": "application/json",
    },
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
  });

  const payload: any = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = payload?.error?.description ?? `Razorpay ${path} failed (${response.status})`;
    logger.error("razorpay api error", { path, status: response.status, payload });
    throw new HttpsError("internal", message);
  }
  return payload;
}

/**
 * Extends the plan from whichever is later — now, or the end of the period already
 * paid for — so a renewal bought early never eats the remaining days.
 */
function nextPeriodEnd(currentEnd: number | undefined, days: number): number {
  const base = currentEnd && currentEnd > Date.now() ? currentEnd : Date.now();
  return base + days * 24 * 60 * 60 * 1000;
}

/**
 * Grants (or extends) the plan for `uid`. Idempotent per paymentId: the payment doc is
 * created inside the transaction, so a webhook and a client verify racing on the same
 * payment can only grant once.
 */
async function grantPlan(args: {
  uid: string;
  planId: string;
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  country: string;
  source: string;
}): Promise<{ planId: string; currentPeriodEnd: number; granted: boolean }> {
  const days = PLAN_DAYS[args.planId];
  if (!days) {
    throw new HttpsError("invalid-argument", `Unknown plan ${args.planId}`);
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

    tx.set(
      subscriptionRef,
      {
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
      },
      { merge: true }
    );

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
    await notifyPaymentSucceeded({
      uid: args.uid,
      planId: result.planId,
      currentPeriodEnd: result.currentPeriodEnd,
      isAdminGrant: args.source === "admin-grant",
    });
  }

  return result;
}

/**
 * The price list as the client should display it, plus which market this caller is billed
 * in. Read-only — no secret, no order created.
 */
export const getPlanPricing = onCall(async (request) => {
  const catalog = await loadCatalog();
  const uid = request.auth?.uid;
  const hint = String(request.data?.countryHint ?? "");
  const country = uid ? await resolveCountry(uid, catalog, hint) : catalog.defaultCountry;
  const pricing = pricingFor(catalog, country);

  return {
    country,
    enabled: catalog.enabled,
    defaultCountry: catalog.defaultCountry,
    pricing: pricing ?? null,
    countries: catalog.countries,
  };
});

/**
 * Admin-only write of the whole price list. Rows are validated here as well as by
 * firestore.rules, so a bad amount can never become a live charge.
 */
export const savePlanPricing = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid || !(await isAdmin(uid))) {
    throw new HttpsError("permission-denied", "Admins only");
  }

  const countries = request.data?.countries as Record<string, CountryPricing> | undefined;
  if (!countries || Object.keys(countries).length === 0) {
    throw new HttpsError("invalid-argument", "countries is required");
  }

  for (const [code, row] of Object.entries(countries)) {
    if (!/^[A-Z]{2}$/.test(code) || !isValidPricing(row)) {
      throw new HttpsError("invalid-argument", `Invalid pricing row for ${code}`);
    }
  }

  const defaultCountry = String(request.data?.defaultCountry ?? DEFAULT_PLAN_CATALOG.defaultCountry);
  if (!countries[defaultCountry]) {
    throw new HttpsError("invalid-argument", "defaultCountry must exist in countries");
  }

  const catalog: PlanCatalog = {
    enabled: request.data?.enabled !== false,
    defaultCountry,
    countries,
  };

  await PLANS_DOC.set(catalog);
  return { saved: Object.keys(countries).length };
});

/** Admin-only: restores every row to the shipped seed table. */
export const resetPlanPricing = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid || !(await isAdmin(uid))) {
    throw new HttpsError("permission-denied", "Admins only");
  }

  await PLANS_DOC.set(DEFAULT_PLAN_CATALOG);
  return { saved: Object.keys(DEFAULT_PLAN_CATALOG.countries).length };
});

/**
 * Step 1 of checkout — the client sends only a plan id and gets back an order to open
 * the Razorpay SDK with. Amount, currency and country are decided here; `keyId` is the
 * publishable key and the secret never leaves this function.
 */
export const createRazorpayOrder = onCall(
  { secrets: [razorpayKeyId, razorpayKeySecret] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in before starting a payment");
    }

    const planId = String(request.data?.planId ?? "");
    if (!PLAN_DAYS[planId]) {
      throw new HttpsError("invalid-argument", "Unknown plan");
    }

    const catalog = await loadCatalog();
    if (!catalog.enabled) {
      throw new HttpsError("failed-precondition", "Upgrades are currently unavailable");
    }

    const country = await resolveCountry(uid, catalog, String(request.data?.countryHint ?? ""));
    const pricing = pricingFor(catalog, country);
    if (!pricing || !isValidPricing(pricing)) {
      throw new HttpsError("failed-precondition", "No price is configured for your country");
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
  }
);

/**
 * Step 2 — the client hands back what the SDK returned. The signature proves Razorpay
 * (not the client) produced this payment for this order; the payment is then re-read
 * from the API so a replayed-but-unpaid signature still cannot buy a plan.
 */
export const verifyRazorpayPayment = onCall(
  { secrets: [razorpayKeyId, razorpayKeySecret] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in before verifying a payment");
    }

    const orderId = String(request.data?.orderId ?? "");
    const paymentId = String(request.data?.paymentId ?? "");
    const signature = String(request.data?.signature ?? "");
    if (!orderId || !paymentId || !signature) {
      throw new HttpsError("invalid-argument", "orderId, paymentId and signature are required");
    }

    const expected = crypto
      .createHmac("sha256", razorpayKeySecret.value())
      .update(`${orderId}|${paymentId}`)
      .digest("hex");

    const expectedBuffer = Buffer.from(expected, "utf8");
    const actualBuffer = Buffer.from(signature, "utf8");
    const signatureOk =
      expectedBuffer.length === actualBuffer.length &&
      crypto.timingSafeEqual(expectedBuffer, actualBuffer);

    if (!signatureOk) {
      logger.warn("razorpay signature mismatch", { uid, orderId, paymentId });
      throw new HttpsError("permission-denied", "Payment signature check failed");
    }

    // The order Razorpay holds is the authority on what was owed — not the catalog, which
    // an admin may have edited between checkout opening and this call.
    const order = await razorpayFetch(`/orders/${orderId}`, { method: "GET" });
    const planId = String(order?.notes?.planId ?? "");
    if (!PLAN_DAYS[planId]) {
      throw new HttpsError("failed-precondition", "Order is not for a known plan");
    }
    if (String(order?.notes?.uid ?? "") !== uid) {
      throw new HttpsError("permission-denied", "Order belongs to another account");
    }

    const orderAmount = Number(order.amount);
    const orderCurrency = String(order.currency);
    const country = String(order?.notes?.country ?? "");

    let payment = await razorpayFetch(`/payments/${paymentId}`, { method: "GET" });

    if (String(payment.order_id) !== orderId) {
      throw new HttpsError("permission-denied", "Payment does not belong to this order");
    }
    if (Number(payment.amount) !== orderAmount || String(payment.currency) !== orderCurrency) {
      throw new HttpsError("permission-denied", "Payment amount does not match the order");
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
      throw new HttpsError("failed-precondition", `Payment is ${payment.status}, not captured`);
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
  }
);

/**
 * Looks a payment up at Razorpay after the fact — "did that actually go through?".
 *
 * Readable by the payer or an admin. If Razorpay says captured but no plan was ever granted
 * (app killed between paying and verifying, and the webhook never fired), this grants it, so
 * opening the billing screen is itself a repair path.
 */
export const getPaymentDetails = onCall(
  { secrets: [razorpayKeyId, razorpayKeySecret] },
  async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid) {
      throw new HttpsError("unauthenticated", "Sign in first");
    }

    const paymentId = String(request.data?.paymentId ?? "");
    if (!paymentId) {
      throw new HttpsError("invalid-argument", "paymentId is required");
    }

    const payment = await razorpayFetch(`/payments/${paymentId}`, { method: "GET" });
    const orderId = String(payment.order_id ?? "");
    const order = orderId ? await razorpayFetch(`/orders/${orderId}`, { method: "GET" }) : undefined;

    const ownerUid = String(order?.notes?.uid ?? payment?.notes?.uid ?? "");
    const callerIsAdmin = await isAdmin(callerUid);
    if (ownerUid !== callerUid && !callerIsAdmin) {
      throw new HttpsError("permission-denied", "That payment belongs to another account");
    }

    const planId = String(order?.notes?.planId ?? payment?.notes?.planId ?? "");
    const country = String(order?.notes?.country ?? payment?.notes?.country ?? "");

    let granted = false;
    if (payment.status === "captured" && ownerUid && PLAN_DAYS[planId]) {
      const existing = await db()
        .collection("subscriptions")
        .doc(ownerUid)
        .collection("payments")
        .doc(paymentId)
        .get();

      if (!existing.exists) {
        await grantPlan({
          uid: ownerUid,
          planId,
          paymentId,
          orderId,
          amount: Number(payment.amount ?? 0),
          currency: String(payment.currency ?? ""),
          country,
          source: "lookup-repair",
        });
        granted = true;
      }
    }

    return {
      paymentId,
      orderId,
      planId,
      country,
      status: String(payment.status ?? ""),
      amount: Number(payment.amount ?? 0),
      currency: String(payment.currency ?? ""),
      method: String(payment.method ?? ""),
      email: String(payment.email ?? ""),
      contact: String(payment.contact ?? ""),
      createdAt: Number(payment.created_at ?? 0) * 1000,
      description: String(payment.description ?? ""),
      grantedNow: granted,
    };
  }
);

/**
 * Order history. A user sees their own; an admin can pass any uid to see someone else's,
 * which is what backs the subscription panel in the admin user detail screen.
 */
export const getBillingHistory = onCall(async (request) => {
  const callerUid = request.auth?.uid;
  if (!callerUid) {
    throw new HttpsError("unauthenticated", "Sign in first");
  }

  const requestedUid = String(request.data?.uid ?? "").trim();
  const uid = requestedUid || callerUid;

  if (uid !== callerUid && !(await isAdmin(callerUid))) {
    throw new HttpsError("permission-denied", "Admins only");
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
 * Admin-granted plan — comps, support gestures, refund make-goods. No money moves, so it is
 * recorded as its own zero-amount entry in the same history the user sees.
 */
export const adminSetSubscription = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  if (!adminUid || !(await isAdmin(adminUid))) {
    throw new HttpsError("permission-denied", "Admins only");
  }

  const uid = String(request.data?.uid ?? "");
  const planId = String(request.data?.planId ?? "");
  if (!uid || !PLAN_DAYS[planId]) {
    throw new HttpsError("invalid-argument", "uid and a known planId are required");
  }

  const days = Number(request.data?.days ?? PLAN_DAYS[planId]);
  if (!Number.isFinite(days) || days <= 0 || days > 3650) {
    throw new HttpsError("invalid-argument", "days must be between 1 and 3650");
  }

  const subscriptionRef = db().collection("subscriptions").doc(uid);
  const current = (await subscriptionRef.get()).data();
  const currentPeriodEnd = nextPeriodEnd(current?.currentPeriodEnd, days);
  const grantId = `admin_${Date.now()}`;

  await subscriptionRef.set(
    {
      uid,
      provider: "admin",
      planId,
      status: "active",
      currentPeriodEnd,
      lastPaymentId: grantId,
      updatedAt: Date.now(),
      grantedBy: adminUid,
    },
    { merge: true }
  );

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

  await notifyPaymentSucceeded({ uid, planId, currentPeriodEnd, isAdminGrant: true });

  logger.info("admin granted subscription", { uid, planId, days, adminUid });
  return { planId, currentPeriodEnd, status: "active" };
});

/**
 * Cancels a plan. By default access runs to the end of the period already paid for;
 * `immediate: true` ends it now, which also switches ads back on for that user.
 */
export const adminCancelSubscription = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  if (!adminUid || !(await isAdmin(adminUid))) {
    throw new HttpsError("permission-denied", "Admins only");
  }

  const uid = String(request.data?.uid ?? "");
  if (!uid) {
    throw new HttpsError("invalid-argument", "uid is required");
  }

  const immediate = request.data?.immediate === true;
  const subscriptionRef = db().collection("subscriptions").doc(uid);
  const current = (await subscriptionRef.get()).data();
  if (!current) {
    throw new HttpsError("not-found", "That account has no subscription");
  }

  const currentPeriodEnd = immediate ? Date.now() : Number(current.currentPeriodEnd ?? 0);

  await subscriptionRef.set(
    {
      status: immediate ? "revoked" : "cancelled",
      currentPeriodEnd,
      updatedAt: Date.now(),
      cancelledBy: adminUid,
    },
    { merge: true }
  );

  logger.info("admin cancelled subscription", { uid, immediate, adminUid });
  return { status: immediate ? "revoked" : "cancelled", currentPeriodEnd };
});

/**
 * Safety net for the case where the money moved but the app never got to call
 * verifyRazorpayPayment (killed app, dead network, user closed the sheet mid-redirect).
 * Point a Razorpay webhook at this URL for the `payment.captured` event.
 */
export const razorpayWebhook = onRequest(
  { secrets: [razorpayWebhookSecret] },
  async (request, response) => {
    const signature = String(request.headers["x-razorpay-signature"] ?? "");
    const body = request.rawBody?.toString("utf8") ?? "";

    const expected = crypto
      .createHmac("sha256", razorpayWebhookSecret.value())
      .update(body)
      .digest("hex");

    const expectedBuffer = Buffer.from(expected, "utf8");
    const actualBuffer = Buffer.from(signature, "utf8");
    const signatureOk =
      expectedBuffer.length === actualBuffer.length &&
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
        await notifyPaymentFailed({
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

    if (!uid || !PLAN_DAYS[planId]) {
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
  }
);
