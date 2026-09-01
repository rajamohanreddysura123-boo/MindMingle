"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const v2_1 = require("firebase-functions/v2");
/**
 * Every function in this codebase runs next to the database.
 *
 * Firestore for this project lives in asia-southeast1. The default region for Cloud Functions
 * is us-central1, so without this the callables ran in Iowa and reached across the Pacific for
 * every read — twice per request, and many times over inside anything that pages through a
 * collection. The Firestore triggers were never affected: those are forced into the database's
 * region, which is why they alone were already in Singapore.
 *
 * This must be imported before any function is defined. Module bodies run when they are first
 * imported, and `onCall`/`onRequest` capture the region at definition time — so importing this
 * anywhere but the very top of the entry point would apply it too late to matter.
 */
(0, v2_1.setGlobalOptions)({ region: "asia-southeast1" });
//# sourceMappingURL=options.js.map