import * as admin from "firebase-admin";
import * as crypto from "node:crypto";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { notifyPaymentFailed, notifyPaymentSucceeded } from "./notifications";
import {
  allocateInvoiceNumber,
  buildInvoice,
  invoiceRef,
  loadTaxConfig,
  pricingRowFor,
} from "./invoices";
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

  // Read outside the transaction: neither is contended, and a transaction may only read
  // before it writes. Missing either just means a plainer invoice — never a blocked grant.
  const [profileSnap, catalog, taxConfig] = await Promise.all([
    db().collection("users").doc(args.uid).get(),
    loadCatalog(),
    loadTaxConfig(),
  ]);
  const profile = profileSnap.data();
  const customerName = String(profile?.name ?? "").trim();
  const customerEmail = String(profile?.email ?? "").trim();
  const issuedAt = Date.now();

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

    // Claimed inside the transaction, after the replay check above, so a payment that has
    // already been granted (client verify racing the webhook for the same payment) never
    // burns a second number — the sequence stays gapless.
    const invoiceNumber = await allocateInvoiceNumber(tx, issuedAt);
    const invoice = buildInvoice({
      invoiceNumber,
      uid: args.uid,
      customerName,
      customerEmail,
      paymentId: args.paymentId,
      orderId: args.orderId,
      planId: args.planId,
      amount: args.amount,
      currency: args.currency,
      country: args.country,
      pricing: pricingRowFor(catalog, args.country),
      taxConfig,
      issuedAt,
      periodEnd: currentPeriodEnd,
    });
    tx.set(invoiceRef(args.uid, invoiceNumber), invoice);

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
        lastInvoiceNumber: invoiceNumber,
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
      invoiceNumber,
      createdAt: issuedAt,
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
 * Desktop's way in: a Razorpay-hosted payment page at a short URL.
 *
 * Razorpay ships checkout SDKs for Android and iOS and nothing for desktop JVM, so there is no
 * sheet to present there. A payment link is the same purchase through a page Razorpay hosts —
 * cards, netbanking, UPI and wallets — which the desktop app offers as a QR to scan with a phone
 * and as a button that opens the browser.
 *
 * Nothing downstream changes. The link carries the same `notes` an order does, so when it is paid
 * the existing razorpayWebhook recognises the uid and plan and grants through the same grantPlan
 * as an in-app purchase. There is deliberately no second grant path and no "I have paid" button:
 * the client is already streaming subscriptions/{uid} and unlocks itself when the webhook writes.
 */
/** How long a desktop payment link stays payable. */
const LINK_TTL_SECONDS = 30 * 60;

export const createPaymentLink = onCall(
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

    // Prefills the hosted page. Both are optional to Razorpay; a blank profile just means the
    // user types their own email on the payment page.
    const profile = (await db().collection("users").doc(uid).get()).data() ?? {};
    const customerName = String(profile.name ?? "").trim();
    const customerEmail = String(profile.email ?? "").trim();

    // Half an hour. Long enough to find a phone and open a UPI app, short enough that a link
    // abandoned on a laptop cannot be paid tomorrow by someone who has already bought on mobile.
    const expireBy = Math.floor(Date.now() / 1000) + LINK_TTL_SECONDS;

    const link = await razorpayFetch("/payment_links", {
      method: "POST",
      body: {
        amount,
        currency: pricing.currency,
        description: `MindMingle+ ${planId === "plus_annual" ? "annual" : "monthly"}`,
        expire_by: expireBy,
        reference_id: `mm_${uid}_${Date.now()}`.slice(0, 40),
        customer: {
          name: customerName || undefined,
          email: customerEmail || undefined,
        },
        // Razorpay will email/SMS the link itself if asked; it is not asked. The user is looking
        // at the QR right now, and an unexpected payment email is alarming, not helpful.
        notify: { sms: false, email: false },
        reminder_enable: false,
        notes: { uid, planId, country },
      },
    });

    const linkId = String(link.id);

    // The same trace an abandoned checkout leaves, so support can tell "never paid" from "paid and
    // not granted" without reading the Razorpay dashboard.
    await db()
      .collection("subscriptions")
      .doc(uid)
      .collection("paymentAttempts")
      .doc(linkId)
      .set(
        {
          uid,
          linkId,
          planId,
          country,
          amount,
          currency: pricing.currency,
          status: "link_created",
          source: "desktop-link",
          createdAt: Date.now(),
        },
        { merge: true }
      );

    logger.info("payment link created", { uid, planId, linkId });

    return {
      linkId,
      url: String(link.short_url),
      amount: Number(amount),
      currency: String(pricing.currency),
      planId,
      country,
      symbol: pricing.symbol,
      decimals: pricing.decimals,
      expiresAt: expireBy * 1000,
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

  // Declines, so a failed charge leaves a visible trace instead of vanishing — this collection
  // previously only backed the "payment failed" email, never shown in either the user's own
  // Orders & Billing or the admin payments view. "link_created" attempts are excluded on purpose:
  // that status never flips to "paid" once the QR is actually scanned and paid, so including it
  // would show every successful desktop payment a second time, permanently stuck as "pending".
  const failedAttemptsSnap = await db()
    .collection("subscriptions")
    .doc(uid)
    .collection("paymentAttempts")
    .where("status", "==", "failed")
    .orderBy("createdAt", "desc")
    .limit(50)
    .get();

  // Invoices live in their own subcollection (see invoices.ts) so this never returned them —
  // the client has always been able to render one, in a dialog that opens when a payment row is
  // tapped, but that dialog never had anything to open: every payment showed with `invoice`
  // resolving to null, so the tap did nothing on every platform. Reading them alongside the
  // payments is what makes the row clickable at all.
  const invoicesSnap = await db()
    .collection("subscriptions")
    .doc(uid)
    .collection("invoices")
    .orderBy("issuedAt", "desc")
    .limit(50)
    .get();

  return {
    uid,
    planId: String(subscription?.planId ?? ""),
    status: String(subscription?.status ?? ""),
    currentPeriodEnd: Number(subscription?.currentPeriodEnd ?? 0),
    billingCountry: String(subscription?.billingCountry ?? ""),
    billingCurrency: String(subscription?.billingCurrency ?? ""),
    payments: [
      ...paymentsSnap.docs.map((doc) => {
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
          status: "success",
          reason: "",
        };
      }),
      // No amount/currency on a decline — recordPaymentFailure never captured a charge to read
      // one from. The row still earns its place: a plan and a reason are what admin needs to see.
      ...failedAttemptsSnap.docs.map((doc) => {
        const data = doc.data();
        return {
          paymentId: String(data.orderId ?? doc.id),
          orderId: String(data.orderId ?? doc.id),
          planId: String(data.planId ?? ""),
          amount: 0,
          currency: "",
          country: "",
          source: "razorpay",
          createdAt: Number(data.createdAt ?? 0),
          status: "failed",
          reason: String(data.reason ?? ""),
        };
      }),
    ].sort((a, b) => b.createdAt - a.createdAt),
    // Field names match InvoiceDto (shared/.../BillingDto.kt) one for one — this is a straight
    // passthrough of what buildInvoice already wrote, not a reshaping.
    invoices: invoicesSnap.docs.map((doc) => {
      const data = doc.data();
      return {
        invoiceNumber: String(data.invoiceNumber ?? doc.id),
        paymentId: String(data.paymentId ?? ""),
        planId: String(data.planId ?? ""),
        planLabel: String(data.planLabel ?? ""),
        description: String(data.description ?? ""),
        subtotal: Number(data.subtotal ?? 0),
        taxAmount: Number(data.taxAmount ?? 0),
        total: Number(data.total ?? 0),
        currency: String(data.currency ?? ""),
        symbol: String(data.symbol ?? ""),
        decimals: Number(data.decimals ?? 2),
        country: String(data.country ?? ""),
        issuedAt: Number(data.issuedAt ?? 0),
        periodEnd: Number(data.periodEnd ?? 0),
        status: String(data.status ?? ""),
        taxLabel: String(data.taxLabel ?? ""),
        taxRate: Number(data.taxRate ?? 0),
        taxComponents: Array.isArray(data.taxComponents)
          ? data.taxComponents.map((c: { label?: unknown; rate?: unknown; amount?: unknown }) => ({
              label: String(c.label ?? ""),
              rate: Number(c.rate ?? 0),
              amount: Number(c.amount ?? 0),
            }))
          : [],
        placeOfSupply: String(data.placeOfSupply ?? ""),
        isExport: Boolean(data.isExport ?? false),
        taxNote: String(data.taxNote ?? ""),
        sellerLegalName: String(data.sellerLegalName ?? ""),
        sellerAddress: String(data.sellerAddress ?? ""),
        sellerTaxId: String(data.sellerTaxId ?? ""),
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
export const recordPaymentFailure = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }

  const orderId = String(request.data?.orderId ?? "").trim();
  const planId = String(request.data?.planId ?? "").trim();
  const reason = String(request.data?.reason ?? "").trim().slice(0, 500);

  if (!orderId) {
    throw new HttpsError("invalid-argument", "orderId is required");
  }

  await db()
    .collection("subscriptions")
    .doc(uid)
    .collection("paymentAttempts")
    .doc(orderId)
    .set(
      {
        uid,
        orderId,
        planId,
        reason,
        status: "failed",
        createdAt: Date.now(),
      },
      // merge: a retry of the same order updates the row rather than stacking a second one.
      { merge: true }
    );

  // Best-effort: the user has just watched a payment fail, and a notification that itself fails
  // must not turn into an error on top of it.
  try {
    await notifyPaymentFailed({ uid, reason });
  } catch (error) {
    logger.warn("payment failure notification not sent", { uid, orderId, error });
  }

  logger.info("payment failure recorded", { uid, orderId, planId });
  return { recorded: true };
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
