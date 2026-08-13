import * as admin from "firebase-admin";
import { HttpsError, onCall } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

export {
  createRazorpayOrder,
  verifyRazorpayPayment,
  razorpayWebhook,
  getPaymentDetails,
  getBillingHistory,
  adminSetSubscription,
  adminCancelSubscription,
  getPlanPricing,
  savePlanPricing,
  resetPlanPricing,
} from "./razorpay";

export { deleteMyAccount, adminDeleteUser } from "./account";

export {
  onChatMessageCreated,
  onLikeReceived,
  onMatchCreated,
  onSupportMessageCreated,
  subscriptionReminders,
} from "./notifications";

/**
 * The one hardcoded admin address for MindMingle. Whoever verifies this email via
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

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

/** Firestore doc IDs can't contain "/"; emails don't either, so this is safe as-is. */
function otpDocId(email: string): string {
  return normalizeEmail(email);
}

/**
 * Desktop's primary sign-in — mails a 6-digit code to `email`. The code and its
 * expiry live in `emailOtps/{email}` (Cloud-Functions-only, see firestore.rules);
 * the actual send happens via the "Trigger Email" Firestore extension watching
 * the `mail` collection.
 */
export const requestEmailOtp = onCall(async (request) => {
  const email = String(request.data?.email ?? "").trim();
  if (!email || !EMAIL_REGEX.test(email)) {
    throw new HttpsError("invalid-argument", "A valid email is required");
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
 * that email (creating one if this is a brand-new sign-in), flips admins/{uid}
 * when the email is the reserved admin address, and returns a custom token so
 * the client can sign in as that uid.
 */
export const verifyEmailOtp = onCall(async (request) => {
  const email = String(request.data?.email ?? "").trim();
  const code = String(request.data?.code ?? "").trim();
  if (!email || !code) {
    throw new HttpsError("invalid-argument", "email and code are required");
  }

  const docRef = db.collection("emailOtps").doc(otpDocId(email));
  const snap = await docRef.get();

  if (!snap.exists) {
    throw new HttpsError("not-found", "No verification code was requested for this email");
  }

  const data = snap.data()!;
  const now = Date.now();
  const expiresAtMs = (data.expiresAt as admin.firestore.Timestamp).toMillis();

  if (now > expiresAtMs) {
    await docRef.delete();
    throw new HttpsError("deadline-exceeded", "Code expired — request a new one");
  }

  if ((data.attempts ?? 0) >= MAX_VERIFY_ATTEMPTS) {
    await docRef.delete();
    throw new HttpsError("resource-exhausted", "Too many attempts — request a new code");
  }

  if (data.code !== code) {
    await docRef.update({ attempts: admin.firestore.FieldValue.increment(1) });
    throw new HttpsError("permission-denied", "Incorrect code");
  }

  await docRef.delete();

  const normalized = normalizeEmail(email);
  let userRecord: admin.auth.UserRecord;
  try {
    userRecord = await admin.auth().getUserByEmail(normalized);
  } catch {
    userRecord = await admin.auth().createUser({ email: normalized, emailVerified: true });
  }

  if (normalized === ADMIN_EMAIL.toLowerCase()) {
    await db.collection("admins").doc(userRecord.uid).set(
      {
        email: normalized,
        grantedAt: admin.firestore.FieldValue.serverTimestamp(),
        grantedVia: "emailOtp",
      },
      { merge: true }
    );
  }

  const customToken = await admin.auth().createCustomToken(userRecord.uid);
  return { customToken, uid: userRecord.uid };
});
