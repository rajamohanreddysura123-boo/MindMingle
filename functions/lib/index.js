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
exports.verifyEmailOtp = exports.requestEmailOtp = void 0;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
admin.initializeApp();
const db = admin.firestore();
/**
 * The one hardcoded admin address for OO. Whoever verifies this email via
 * requestEmailOtp/verifyEmailOtp gets admins/{uid} set for their Firebase Auth
 * account. Firebase Auth resolves accounts by email regardless of sign-in
 * method, so once this is set, that same person's Google sign-in on mobile
 * (see AuthViewModel.admitIfAllowed / OOAdminRepository.isCurrentUserAdmin)
 * is recognized as admin automatically — no separate mobile-side check needed.
 */
const ADMIN_EMAIL = "rajamohanreddysura123@gmail.com";
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
            subject: "Your OO verification code",
            text: `Your OO verification code is ${code}. It expires in 5 minutes. If you didn't request this, you can ignore this email.`,
            html: `<p>Your OO verification code is <b>${code}</b>.</p><p>It expires in 5 minutes. If you didn't request this, you can ignore this email.</p>`,
        },
    });
    return { success: true };
});
/**
 * Verifies the mailed code. On success: ensures a Firebase Auth user exists for
 * that email (creating one if this is a brand-new sign-in), flips admins/{uid}
 * when the email is the reserved admin address, and returns a custom token so
 * the client can sign in as that uid.
 */
exports.verifyEmailOtp = (0, https_1.onCall)(async (request) => {
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
    await docRef.delete();
    const normalized = normalizeEmail(email);
    let userRecord;
    try {
        userRecord = await admin.auth().getUserByEmail(normalized);
    }
    catch {
        userRecord = await admin.auth().createUser({ email: normalized, emailVerified: true });
    }
    if (normalized === ADMIN_EMAIL.toLowerCase()) {
        await db.collection("admins").doc(userRecord.uid).set({
            email: normalized,
            grantedAt: admin.firestore.FieldValue.serverTimestamp(),
            grantedVia: "emailOtp",
        }, { merge: true });
    }
    const customToken = await admin.auth().createCustomToken(userRecord.uid);
    return { customToken, uid: userRecord.uid };
});
//# sourceMappingURL=index.js.map