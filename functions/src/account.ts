import * as admin from "firebase-admin";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

/**
 * Account deletion — the "leave no record behind" path.
 *
 * A user deleting themselves from Profile → Delete Account, and an admin deleting someone
 * from the admin user detail screen, both land here. The client cannot do this itself: half
 * of it (Firebase Auth accounts, Storage objects, the other side of every like) is out of
 * reach of a client SDK no matter what the rules say.
 *
 * What gets purged for a uid:
 *   users/{uid}                                  profile
 *   likes/{uid}/sentTo/*  + the mirror row in incomingLikes/{other}/from/{uid}
 *   incomingLikes/{uid}/from/* + the mirror row in likes/{other}/sentTo/{uid}
 *   matches/{id} (+ messages) for every match they are in
 *   anonymousQueue/{uid}, anonymousSessions/{id} (+ relay), anonymousAuditLog entries
 *   subscriptions/{uid} (+ payments)
 *   emailOtps/{email}, mail/* addressed to them
 *   Storage users/{uid}/**  (profile photos)
 *   their Firebase Auth account
 */

const db = () => admin.firestore();

async function isAdmin(uid: string): Promise<boolean> {
  return (await db().collection("admins").doc(uid).get()).exists;
}

/** Deletes docs in batches; `recursiveDelete` handles any subcollections hanging off them. */
async function deleteDocs(refs: FirebaseFirestore.DocumentReference[]): Promise<void> {
  for (const ref of refs) {
    await db().recursiveDelete(ref);
  }
}

async function purgeLikes(uid: string): Promise<void> {
  const sent = await db().collection("likes").doc(uid).collection("sentTo").get();
  await deleteDocs(sent.docs.map((doc) => db().collection("incomingLikes").doc(doc.id).collection("from").doc(uid)));

  const received = await db().collection("incomingLikes").doc(uid).collection("from").get();
  await deleteDocs(received.docs.map((doc) => db().collection("likes").doc(doc.id).collection("sentTo").doc(uid)));

  await deleteDocs([db().collection("likes").doc(uid), db().collection("incomingLikes").doc(uid)]);
}

async function purgeMatches(uid: string): Promise<void> {
  const matches = await db().collection("matches").where("users", "array-contains", uid).get();
  await deleteDocs(matches.docs.map((doc) => doc.ref));
}

async function purgeAnonymous(uid: string): Promise<void> {
  await deleteDocs([db().collection("anonymousQueue").doc(uid)]);

  const sessions = await db()
    .collection("anonymousSessions")
    .where("participants", "array-contains", uid)
    .get();
  await deleteDocs(sessions.docs.map((doc) => doc.ref));

  const auditEntries = await db()
    .collection("anonymousAuditLog")
    .where("participants", "array-contains", uid)
    .get();
  await deleteDocs(auditEntries.docs.map((doc) => doc.ref));
}

async function purgeMail(email: string): Promise<void> {
  if (!email) return;

  const normalized = email.trim().toLowerCase();
  await deleteDocs([db().collection("emailOtps").doc(normalized)]);

  const mails = await db().collection("mail").where("to", "==", email).get();
  await deleteDocs(mails.docs.map((doc) => doc.ref));
}

async function purgeStorage(uid: string): Promise<void> {
  try {
    await admin.storage().bucket().deleteFiles({ prefix: `users/${uid}/` });
  } catch (error) {
    // A missing bucket must not strand the rest of the purge — the Firestore side matters more.
    logger.warn("storage purge failed", { uid, error });
  }
}

async function lookupEmail(uid: string): Promise<string> {
  try {
    return (await admin.auth().getUser(uid)).email ?? "";
  } catch {
    return "";
  }
}

async function purgeAuth(uid: string): Promise<void> {
  try {
    await admin.auth().deleteUser(uid);
  } catch (error) {
    logger.warn("auth purge skipped", { uid, error });
  }
}

/**
 * @param ban leaves a bannedUids tombstone so the uid can never sign back in. Used for admin
 *   removals of abusive accounts; a user deleting their own account leaves nothing behind.
 */
async function purgeUser(uid: string, options: { ban: boolean; bannedBy?: string; reason?: string }) {
  // The Auth account goes last: if a data step throws, the user can still sign in and retry
  // rather than being locked out of an account that still has records behind it.
  const email = await lookupEmail(uid);

  await purgeLikes(uid);
  await purgeMatches(uid);
  await purgeAnonymous(uid);
  await deleteDocs([db().collection("subscriptions").doc(uid)]);
  await purgeStorage(uid);
  await purgeMail(email);
  await deleteDocs([db().collection("users").doc(uid)]);
  await purgeAuth(uid);

  if (options.ban) {
    await db().collection("bannedUids").doc(uid).set({
      reason: options.reason ?? "account deleted by admin",
      bannedBy: options.bannedBy ?? "",
      bannedAt: Date.now(),
    });
  } else {
    await deleteDocs([db().collection("bannedUids").doc(uid)]);
  }

  logger.info("account purged", { uid, banned: options.ban });
}

/**
 * Self-service deletion. Irreversible, and deliberately leaves no tombstone: the same person
 * signing up again later starts genuinely fresh.
 */
export const deleteMyAccount = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign in first");
  }

  await purgeUser(uid, { ban: false });
  return { deleted: true };
});

/** Admin removal. `ban: true` (the default) keeps the uid permanently locked out. */
export const adminDeleteUser = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  if (!adminUid || !(await isAdmin(adminUid))) {
    throw new HttpsError("permission-denied", "Admins only");
  }

  const uid = String(request.data?.uid ?? "");
  if (!uid) {
    throw new HttpsError("invalid-argument", "uid is required");
  }
  if (uid === adminUid) {
    throw new HttpsError("failed-precondition", "Use deleteMyAccount to remove your own account");
  }

  const ban = request.data?.ban !== false;
  await purgeUser(uid, {
    ban,
    bannedBy: adminUid,
    reason: String(request.data?.reason ?? "account deleted by admin"),
  });

  return { deleted: true, banned: ban };
});
