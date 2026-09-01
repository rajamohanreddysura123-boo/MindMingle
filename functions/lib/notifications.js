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
exports.subscriptionReminders = exports.onSupportMessageCreated = exports.onMatchCreated = exports.onLikeReceived = exports.onChatMessageCreated = void 0;
exports.sendToUser = sendToUser;
exports.notifyPaymentSucceeded = notifyPaymentSucceeded;
exports.notifyPaymentFailed = notifyPaymentFailed;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-functions/v2/firestore");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const logger = __importStar(require("firebase-functions/logger"));
const pricing_1 = require("./pricing");
/**
 * Push notifications (FCM).
 *
 * Everything a user should hear about is a Firestore write somebody else made, so each
 * notification is a trigger on that write rather than something the sending client fires —
 * a client cannot be trusted to notify someone it just messaged, and it is offline half the
 * time anyway.
 *
 * Devices:      users/{uid}/devices/{token}   — written by the app on sign-in, cleaned up here
 *                                               whenever FCM reports a token as dead
 * Preferences:  notificationPrefs/{uid}       — per-category switches, owner-writable
 *
 * Anonymous chat is deliberately excluded: those rooms are built so nothing survives them,
 * and a push containing the message (or even its existence) would leak exactly what the
 * feature promises not to keep.
 */
const db = () => admin.firestore();
/** Categories default to on: a user who never opened the settings still gets their messages. */
async function isCategoryEnabled(uid, category) {
    const snap = await db().collection("notificationPrefs").doc(uid).get();
    if (!snap.exists)
        return true;
    return snap.data()?.[category] !== false;
}
/**
 * A deactivated user is meant to hear nothing at all while their break runs, and a deleted one
 * never again — neither should be pinged by anything still in flight.
 */
async function isReachable(uid) {
    const snap = await db().collection("users").doc(uid).get();
    const user = snap.data();
    if (!user)
        return false;
    if (user.isDisabled === true || user.isDeletionRequested === true)
        return false;
    const reactivateAt = Number(user.reactivateAt ?? 0);
    return !(user.isDeactivated === true && Date.now() < reactivateAt);
}
async function tokensFor(uid) {
    const snap = await db().collection("users").doc(uid).collection("devices").get();
    return snap.docs.map((doc) => String(doc.data().token ?? doc.id)).filter((token) => token.length > 0);
}
/** FCM tells us when a token is dead (app uninstalled, data cleared); that row is then useless. */
async function dropTokens(uid, tokens) {
    await Promise.all(tokens.map((token) => db().collection("users").doc(uid).collection("devices").doc(token).delete()));
}
async function sendToUser(uid, category, payload) {
    if (!uid)
        return 0;
    if (!(await isCategoryEnabled(uid, category))) {
        logger.debug("notification muted by preference", { uid, category });
        return 0;
    }
    if (!(await isReachable(uid))) {
        logger.debug("notification skipped, account not reachable", { uid, category });
        return 0;
    }
    const tokens = await tokensFor(uid);
    if (tokens.length === 0)
        return 0;
    const response = await admin.messaging().sendEachForMulticast({
        tokens,
        notification: { title: payload.title, body: payload.body },
        data: { category, ...(payload.data ?? {}) },
        android: {
            priority: "high",
            // Must match the channel the app creates in PushPlatform.android.kt. Android drops a
            // notification addressed to a channel that does not exist, without any error the sender
            // can see — this said "oo_default" while the app registers "mindmingle_default".
            notification: { channelId: "mindmingle_default", sound: "default" },
        },
        apns: {
            payload: { aps: { sound: "default", badge: 1 } },
        },
    });
    const dead = [];
    response.responses.forEach((result, index) => {
        const code = result.error?.code ?? "";
        if (code === "messaging/registration-token-not-registered" ||
            code === "messaging/invalid-registration-token" ||
            code === "messaging/invalid-argument") {
            dead.push(tokens[index]);
        }
    });
    if (dead.length > 0)
        await dropTokens(uid, dead);
    logger.info("notification sent", {
        uid,
        category,
        delivered: response.successCount,
        failed: response.failureCount,
    });
    return response.successCount;
}
async function displayName(uid) {
    const snap = await db().collection("users").doc(uid).get();
    const name = String(snap.data()?.name ?? "").trim();
    return name || "Someone";
}
/** Notification bodies are previews, not transcripts — long messages get cut, not wrapped. */
function preview(text, limit = 120) {
    const clean = text.replace(/\s+/g, " ").trim();
    return clean.length <= limit ? clean : `${clean.slice(0, limit - 1)}…`;
}
// ---------------------------------------------------------------------------
// Chat
// ---------------------------------------------------------------------------
// The client writes chat under `conversations`, not `matches` — these triggers watched a
// collection nothing writes to any more, so they never fired.
exports.onChatMessageCreated = (0, firestore_1.onDocumentCreated)("conversations/{conversationId}/messages/{messageId}", async (event) => {
    const message = event.data?.data();
    if (!message)
        return;
    const conversationId = event.params.conversationId;
    const senderId = String(message.senderId ?? "");
    const text = String(message.text ?? "");
    if (!senderId)
        return;
    const conversationSnap = await db().collection("conversations").doc(conversationId).get();
    const users = conversationSnap.data()?.users ?? [];
    const recipient = users.find((uid) => uid !== senderId);
    if (!recipient)
        return;
    await sendToUser(recipient, "messages", {
        title: await displayName(senderId),
        body: preview(text),
        data: { type: "chat", conversationId, senderId },
    });
});
// ---------------------------------------------------------------------------
// Likes and matches
// ---------------------------------------------------------------------------
exports.onLikeReceived = (0, firestore_1.onDocumentCreated)("incomingLikes/{toUid}/from/{fromUid}", async (event) => {
    const { toUid, fromUid } = event.params;
    if (!toUid || !fromUid || toUid === fromUid)
        return;
    // A mutual like opens the conversation in the same breath; that trigger owns the "It's a
    // match" notification, so this one would be a duplicate.
    const conversationId = [toUid, fromUid].sort().join("_");
    if ((await db().collection("conversations").doc(conversationId).get()).exists)
        return;
    await sendToUser(toUid, "likes", {
        title: "Someone liked you",
        body: "Open Discover to see who is interested in your stack.",
        data: { type: "like", fromUid },
    });
});
exports.onMatchCreated = (0, firestore_1.onDocumentCreated)("conversations/{conversationId}", async (event) => {
    const users = event.data?.data()?.users ?? [];
    if (users.length !== 2)
        return;
    const conversationId = event.params.conversationId;
    const [first, second] = users;
    const names = await Promise.all([displayName(first), displayName(second)]);
    await Promise.all([
        sendToUser(first, "likes", {
            title: "It's a match!",
            body: `You and ${names[1]} liked each other. Say hi.`,
            data: { type: "match", conversationId, withUid: second },
        }),
        sendToUser(second, "likes", {
            title: "It's a match!",
            body: `You and ${names[0]} liked each other. Say hi.`,
            data: { type: "match", conversationId, withUid: first },
        }),
    ]);
});
// ---------------------------------------------------------------------------
// Support
// ---------------------------------------------------------------------------
exports.onSupportMessageCreated = (0, firestore_1.onDocumentCreated)("supportChats/{uid}/messages/{messageId}", async (event) => {
    const message = event.data?.data();
    if (!message)
        return;
    const uid = event.params.uid;
    const senderId = String(message.senderId ?? "");
    const text = preview(String(message.text ?? ""));
    if (senderId === "support") {
        await sendToUser(uid, "support", {
            title: "MindMingle Support",
            body: text,
            data: { type: "support", uid },
        });
        return;
    }
    // A user wrote in: ping every admin so the desktop inbox is not a thing someone has to
    // remember to check.
    const admins = await db().collection("users").where("userType", "==", "admin").get();
    const name = await displayName(uid);
    await Promise.all(admins.docs.map((doc) => sendToUser(doc.id, "support", {
        title: `Support: ${name}`,
        body: text,
        data: { type: "support-inbox", uid },
    })));
});
// ---------------------------------------------------------------------------
// Payments — called straight from the Razorpay functions, not from a trigger, because the
// subscription doc is written by the same code that already knows what happened.
// ---------------------------------------------------------------------------
async function notifyPaymentSucceeded(args) {
    const until = new Date(args.currentPeriodEnd).toUTCString().slice(5, 16);
    await sendToUser(args.uid, "payments", {
        title: args.isAdminGrant ? "MindMingle+ unlocked" : "Payment successful",
        body: `Your MindMingle+ plan is active until ${until}. Ads are off.`,
        data: { type: "payment", planId: args.planId },
    });
}
async function notifyPaymentFailed(args) {
    await sendToUser(args.uid, "payments", {
        title: "Payment failed",
        body: args.reason || "Your MindMingle+ payment did not go through. No money was taken.",
        data: { type: "payment-failed" },
    });
}
// ---------------------------------------------------------------------------
// Expiry reminders
// ---------------------------------------------------------------------------
const DAY_MS = 24 * 60 * 60 * 1000;
/**
 * Daily sweep: warns three days out, and tells people the day their plan lapses (which is
 * also the day ads come back, so it should not be a surprise).
 */
exports.subscriptionReminders = (0, scheduler_1.onSchedule)("every day 09:00", async () => {
    const now = Date.now();
    const expiringSoon = await db()
        .collection("subscriptions")
        .where("currentPeriodEnd", ">=", now + 2 * DAY_MS)
        .where("currentPeriodEnd", "<", now + 3 * DAY_MS)
        .get();
    for (const doc of expiringSoon.docs) {
        const data = doc.data();
        if (data.status === "revoked")
            continue;
        const planLabel = pricing_1.PLAN_DAYS[String(data.planId ?? "")] ? "MindMingle+" : "Your plan";
        await sendToUser(doc.id, "payments", {
            title: `${planLabel} renews soon`,
            body: data.status === "cancelled"
                ? "Your plan ends in 3 days. Renew any time to stay ad-free."
                : "Your plan expires in 3 days. Renew to stay ad-free.",
            data: { type: "plan-expiring" },
        });
    }
    const justExpired = await db()
        .collection("subscriptions")
        .where("currentPeriodEnd", ">=", now - DAY_MS)
        .where("currentPeriodEnd", "<", now)
        .get();
    for (const doc of justExpired.docs) {
        await sendToUser(doc.id, "payments", {
            title: "MindMingle+ has ended",
            body: "Your plan expired. Upgrade again from Profile to go ad-free.",
            data: { type: "plan-expired" },
        });
    }
    logger.info("subscription reminders run", {
        expiringSoon: expiringSoon.size,
        justExpired: justExpired.size,
    });
});
//# sourceMappingURL=notifications.js.map