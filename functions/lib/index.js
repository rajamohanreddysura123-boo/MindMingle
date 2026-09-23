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
exports.verifyEmailOtp = exports.requestEmailOtp = exports.subscriptionReminders = exports.onSupportMessageCreated = exports.onMatchCreated = exports.onLikeReceived = exports.onChatMessageCreated = exports.adminSubscriberStats = exports.adminListSubscribers = exports.adminCancelSubscription = exports.adminSetSubscription = exports.recordPaymentFailure = exports.getBillingHistory = exports.razorpayWebhook = exports.verifyRazorpayPayment = exports.createPaymentLink = exports.createRazorpayOrder = void 0;
// First, and deliberately so: it pins the region for every function defined below, including
// the ones re-exported from ./razorpay and ./notifications.
require("./options");
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
admin.initializeApp();
const db = admin.firestore();
var razorpay_1 = require("./razorpay");
Object.defineProperty(exports, "createRazorpayOrder", { enumerable: true, get: function () { return razorpay_1.createRazorpayOrder; } });
Object.defineProperty(exports, "createPaymentLink", { enumerable: true, get: function () { return razorpay_1.createPaymentLink; } });
Object.defineProperty(exports, "verifyRazorpayPayment", { enumerable: true, get: function () { return razorpay_1.verifyRazorpayPayment; } });
Object.defineProperty(exports, "razorpayWebhook", { enumerable: true, get: function () { return razorpay_1.razorpayWebhook; } });
Object.defineProperty(exports, "getBillingHistory", { enumerable: true, get: function () { return razorpay_1.getBillingHistory; } });
Object.defineProperty(exports, "recordPaymentFailure", { enumerable: true, get: function () { return razorpay_1.recordPaymentFailure; } });
Object.defineProperty(exports, "adminSetSubscription", { enumerable: true, get: function () { return razorpay_1.adminSetSubscription; } });
Object.defineProperty(exports, "adminCancelSubscription", { enumerable: true, get: function () { return razorpay_1.adminCancelSubscription; } });
var subscribers_1 = require("./subscribers");
Object.defineProperty(exports, "adminListSubscribers", { enumerable: true, get: function () { return subscribers_1.adminListSubscribers; } });
Object.defineProperty(exports, "adminSubscriberStats", { enumerable: true, get: function () { return subscribers_1.adminSubscriberStats; } });
var notifications_1 = require("./notifications");
Object.defineProperty(exports, "onChatMessageCreated", { enumerable: true, get: function () { return notifications_1.onChatMessageCreated; } });
Object.defineProperty(exports, "onLikeReceived", { enumerable: true, get: function () { return notifications_1.onLikeReceived; } });
Object.defineProperty(exports, "onMatchCreated", { enumerable: true, get: function () { return notifications_1.onMatchCreated; } });
Object.defineProperty(exports, "onSupportMessageCreated", { enumerable: true, get: function () { return notifications_1.onSupportMessageCreated; } });
Object.defineProperty(exports, "subscriptionReminders", { enumerable: true, get: function () { return notifications_1.subscriptionReminders; } });
/**
 * No address is special here. Admin rights are `users/{uid}.userType == "admin"`, set by hand in
 * the Firebase Console — sign-in never promotes anyone. See firestore.rules isAdmin().
 */
/**
 * Who `verifyEmailOtp` runs as.
 *
 * Minting a custom token means signing a JWT, and a deployed function holds no private key — it
 * asks the IAM Credentials API to sign for it (`iam.serviceAccounts.signBlob`). The default
 * compute service account this project's functions otherwise run as does not have that, so every
 * verify died with `auth/insufficient-permission` and surfaced to the client as a bare INTERNAL.
 *
 * The Firebase Admin SDK service account already holds `roles/iam.serviceAccountTokenCreator`
 * project-wide, so pinning this one function to it makes signing work without depending on an
 * IAM grant that has to be maintained by hand. Only this function needs it; everything else stays
 * on the default account.
 */
const OTP_SIGNER_SERVICE_ACCOUNT = "firebase-adminsdk-fbsvc@tech-connect-44987.iam.gserviceaccount.com";
const OTP_TTL_MS = 5 * 60 * 1000;
const MAX_VERIFY_ATTEMPTS = 5;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
function normalizeEmail(email) {
    return email.trim().toLowerCase();
}
/** Firestore doc IDs can't contain "/"; emails don't either, so this is safe as-is. */
function otpDocId(email) {
    return normalizeEmail(email);
}
/**
 * Desktop's primary sign-in — mails a 6-digit code to `email`. The code and its
 * expiry live in `emailOtps/{email}` (Cloud-Functions-only, see firestore.rules);
 * the actual send happens via the "Trigger Email" Firestore extension watching
 * the `mail` collection.
 */
exports.requestEmailOtp = (0, https_1.onCall)(async (request) => {
    const email = String(request.data?.email ?? "").trim();
    if (!email || !EMAIL_REGEX.test(email)) {
        throw new https_1.HttpsError("invalid-argument", "A valid email is required");
    }
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    await db.collection("emailOtps").doc(otpDocId(email)).set({
        email: normalizeEmail(email),
        code,
        expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + OTP_TTL_MS),
        attempts: 0,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    await db.collection("mail").add({
        to: email,
        message: {
            subject: "Your MindMingle verification code",
            text: `Your MindMingle verification code is ${code}. It expires in 5 minutes. If you didn't request this, you can ignore this email.`,
            html: `<p>Your MindMingle verification code is <b>${code}</b>.</p><p>It expires in 5 minutes. If you didn't request this, you can ignore this email.</p>`,
        },
    });
    return { success: true };
});
/**
 * Verifies the mailed code. On success: ensures a Firebase Auth user exists for
 * that email (creating one if this is a brand-new sign-in) and returns a custom
 * token so the client can sign in as that uid. Grants nothing — every account
 * that comes through here is an ordinary user until userType says otherwise.
 */
exports.verifyEmailOtp = (0, https_1.onCall)({ serviceAccount: OTP_SIGNER_SERVICE_ACCOUNT }, async (request) => {
    const email = String(request.data?.email ?? "").trim();
    const code = String(request.data?.code ?? "").trim();
    if (!email || !code) {
        throw new https_1.HttpsError("invalid-argument", "email and code are required");
    }
    const docRef = db.collection("emailOtps").doc(otpDocId(email));
    const snap = await docRef.get();
    if (!snap.exists) {
        throw new https_1.HttpsError("not-found", "No verification code was requested for this email");
    }
    const data = snap.data();
    const now = Date.now();
    const expiresAtMs = data.expiresAt.toMillis();
    if (now > expiresAtMs) {
        await docRef.delete();
        throw new https_1.HttpsError("deadline-exceeded", "Code expired — request a new one");
    }
    if ((data.attempts ?? 0) >= MAX_VERIFY_ATTEMPTS) {
        await docRef.delete();
        throw new https_1.HttpsError("resource-exhausted", "Too many attempts — request a new code");
    }
    if (data.code !== code) {
        await docRef.update({ attempts: admin.firestore.FieldValue.increment(1) });
        throw new https_1.HttpsError("permission-denied", "Incorrect code");
    }
    const normalized = normalizeEmail(email);
    let userRecord;
    try {
        userRecord = await admin.auth().getUserByEmail(normalized);
    }
    catch {
        userRecord = await admin.auth().createUser({ email: normalized, emailVerified: true });
    }
    const customToken = await admin.auth().createCustomToken(userRecord.uid);
    // Deleted only once the token exists. Deleting first meant any failure past this point — the
    // signing permission error this project actually hit — burned a code the user had typed
    // correctly, forcing a new email for every retry. The replay window is the few milliseconds
    // between minting and deleting, and the code is single-use from the next request onward.
    await docRef.delete();
    return { customToken, uid: userRecord.uid };
});
//# sourceMappingURL=index.js.map