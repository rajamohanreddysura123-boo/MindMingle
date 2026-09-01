// First, and deliberately so: it pins the region for every function defined below, including
// the ones re-exported from ./razorpay and ./notifications.
import "./options";
import * as admin from "firebase-admin";
import { HttpsError, onCall } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

export {
  createRazorpayOrder,
  verifyRazorpayPayment,
  razorpayWebhook,
  getBillingHistory,
  recordPaymentFailure,
  adminSetSubscription,
  adminCancelSubscription,
} from "./razorpay";

export { adminListSubscribers, adminSubscriberStats } from "./subscribers";

export {
  onChatMessageCreated,
  onLikeReceived,
  onMatchCreated,
  onSupportMessageCreated,
  subscriptionReminders,
} from "./notifications";

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
const OTP_SIGNER_SERVICE_ACCOUNT =
  "firebase-adminsdk-fbsvc@tech-connect-44987.iam.gserviceaccount.com";

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
 * that email (creating one if this is a brand-new sign-in) and returns a custom
 * token so the client can sign in as that uid. Grants nothing — every account
 * that comes through here is an ordinary user until userType says otherwise.
 */
export const verifyEmailOtp = onCall({ serviceAccount: OTP_SIGNER_SERVICE_ACCOUNT }, async (request) => {
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

  const normalized = normalizeEmail(email);
  let userRecord: admin.auth.UserRecord;
  try {
    userRecord = await admin.auth().getUserByEmail(normalized);
  } catch {
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
