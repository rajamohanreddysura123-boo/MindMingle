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
exports.loadTaxConfig = loadTaxConfig;
exports.planLabel = planLabel;
exports.allocateInvoiceNumber = allocateInvoiceNumber;
exports.formatAmount = formatAmount;
exports.buildInvoice = buildInvoice;
exports.invoiceRef = invoiceRef;
exports.pricingRowFor = pricingRowFor;
const admin = __importStar(require("firebase-admin"));
const pricing_1 = require("./pricing");
const tax_1 = require("./tax");
/**
 * Invoices for MindMingle+ purchases.
 *
 * One invoice per captured payment, stored at `subscriptions/{uid}/invoices/{invoiceNumber}`
 * so it inherits the same owner-or-admin read rule the payment rows already have.
 *
 * The number is allocated inside the same transaction that grants the plan (see grantPlan in
 * razorpay.ts). That matters: the webhook and the client's verify call can both arrive for one
 * payment, and allocating outside the transaction would burn a second number on the replay —
 * leaving a gap in a sequence that is supposed to be gapless.
 */
const db = () => admin.firestore();
/** Where the running sequence lives. Read and written only inside a transaction. */
const COUNTER_DOC = () => db().collection("appConfig").doc("invoiceCounter");
/** `appConfig/tax`, falling back to charging nothing anywhere. */
async function loadTaxConfig() {
    const snap = await db().collection("appConfig").doc("tax").get();
    const data = snap.data();
    if (!data)
        return tax_1.DEFAULT_TAX_CONFIG;
    return {
        ...tax_1.DEFAULT_TAX_CONFIG,
        ...data,
        countries: data.countries ?? {},
    };
}
const PLAN_LABELS = {
    plus_monthly: "MindMingle+ Monthly",
    plus_annual: "MindMingle+ Annual",
};
function planLabel(planId) {
    return PLAN_LABELS[planId] ?? "MindMingle+";
}
/**
 * Invoice numbers restart each calendar year, which is what most accounting software expects:
 * MM-2026-000001. The year is part of the counter key so January never collides with December.
 */
function invoiceNumberFor(year, sequence) {
    return `MM-${year}-${String(sequence).padStart(6, "0")}`;
}
/**
 * Claims the next invoice number. Must be called with the transaction that also writes the
 * invoice, so a rolled-back grant never consumes a number.
 */
async function allocateInvoiceNumber(tx, issuedAt) {
    const year = new Date(issuedAt).getUTCFullYear();
    const counterRef = COUNTER_DOC();
    const snap = await tx.get(counterRef);
    const next = Number(snap.data()?.[String(year)] ?? 0) + 1;
    tx.set(counterRef, { [String(year)]: next }, { merge: true });
    return invoiceNumberFor(year, next);
}
/**
 * Money as a person reads it. Amounts are minor units (paise, cents, fils), and how many of
 * those make a whole unit differs per currency — JPY has none, KWD has three.
 */
function formatAmount(minorUnits, symbol, decimals) {
    if (decimals <= 0)
        return `${symbol}${Math.round(minorUnits).toLocaleString("en-US")}`;
    const divisor = 10 ** decimals;
    const value = minorUnits / divisor;
    return `${symbol}${value.toLocaleString("en-US", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
    })}`;
}
/**
 * Builds the invoice for a payment that has just been granted.
 *
 * The charged amount is the truth here — tax is carved out of it (see computeTax), never added
 * to it. Whatever the customer paid is what `total` says they paid.
 */
function buildInvoice(args) {
    const label = planLabel(args.planId);
    const tax = (0, tax_1.computeTax)({
        amount: args.amount,
        country: args.country,
        config: args.taxConfig,
    });
    return {
        invoiceNumber: args.invoiceNumber,
        uid: args.uid,
        customerName: args.customerName,
        customerEmail: args.customerEmail,
        paymentId: args.paymentId,
        orderId: args.orderId,
        planId: args.planId,
        planLabel: label,
        description: `${label} subscription`,
        subtotal: tax.subtotal,
        taxAmount: tax.taxAmount,
        total: tax.total,
        currency: args.currency,
        symbol: args.pricing?.symbol ?? args.currency,
        decimals: args.pricing?.decimals ?? 2,
        country: args.country,
        issuedAt: args.issuedAt,
        periodStart: args.issuedAt,
        periodEnd: args.periodEnd,
        status: "paid",
        taxLabel: tax.taxLabel,
        taxRate: tax.taxRate,
        taxComponents: tax.components,
        placeOfSupply: tax.placeOfSupply,
        isExport: tax.isExport,
        taxNote: tax.note,
        sellerLegalName: args.taxConfig.sellerLegalName,
        sellerAddress: args.taxConfig.sellerAddress,
        sellerTaxId: args.taxConfig.sellerTaxId,
    };
}
function invoiceRef(uid, invoiceNumber) {
    return db().collection("subscriptions").doc(uid).collection("invoices").doc(invoiceNumber);
}
/** The pricing row an invoice should be formatted with, or undefined if the country is gone. */
function pricingRowFor(catalog, country) {
    return (0, pricing_1.pricingFor)(catalog, country);
}
//# sourceMappingURL=invoices.js.map