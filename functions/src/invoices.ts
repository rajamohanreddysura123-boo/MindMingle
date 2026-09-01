import * as admin from "firebase-admin";
import { CountryPricing, PlanCatalog, pricingFor } from "./pricing";
import { DEFAULT_TAX_CONFIG, TaxComponent, TaxConfig, computeTax } from "./tax";

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

export interface InvoiceRecord {
  invoiceNumber: string;
  uid: string;
  customerName: string;
  customerEmail: string;
  paymentId: string;
  orderId: string;
  planId: string;
  planLabel: string;
  description: string;
  /** All amounts are in the currency's smallest unit, exactly as `payments` stores them. */
  subtotal: number;
  taxAmount: number;
  total: number;
  currency: string;
  symbol: string;
  decimals: number;
  country: string;
  issuedAt: number;
  periodStart: number;
  periodEnd: number;
  status: "paid";

  // Tax. Present on every invoice even when nothing was charged, because an invoice is never
  // edited after it is issued — a field added later could not be filled in on the old ones.
  taxLabel: string;
  /** Basis points: 1800 = 18%. */
  taxRate: number;
  /** Two entries where a country splits its tax (India's CGST/SGST), one otherwise, none if untaxed. */
  taxComponents: TaxComponent[];
  placeOfSupply: string;
  isExport: boolean;
  /** Explains an absent tax line — an export note, typically. */
  taxNote: string;

  // The issuer, copied in rather than referenced: an invoice must still read correctly years
  // later, after the address or registration number has changed.
  sellerLegalName: string;
  sellerAddress: string;
  sellerTaxId: string;
}

/** `appConfig/tax`, falling back to charging nothing anywhere. */
export async function loadTaxConfig(): Promise<TaxConfig> {
  const snap = await db().collection("appConfig").doc("tax").get();
  const data = snap.data() as Partial<TaxConfig> | undefined;
  if (!data) return DEFAULT_TAX_CONFIG;

  return {
    ...DEFAULT_TAX_CONFIG,
    ...data,
    countries: data.countries ?? {},
  };
}

const PLAN_LABELS: Record<string, string> = {
  plus_monthly: "MindMingle+ Monthly",
  plus_annual: "MindMingle+ Annual",
};

export function planLabel(planId: string): string {
  return PLAN_LABELS[planId] ?? "MindMingle+";
}

/**
 * Invoice numbers restart each calendar year, which is what most accounting software expects:
 * MM-2026-000001. The year is part of the counter key so January never collides with December.
 */
function invoiceNumberFor(year: number, sequence: number): string {
  return `MM-${year}-${String(sequence).padStart(6, "0")}`;
}

/**
 * Claims the next invoice number. Must be called with the transaction that also writes the
 * invoice, so a rolled-back grant never consumes a number.
 */
export async function allocateInvoiceNumber(
  tx: FirebaseFirestore.Transaction,
  issuedAt: number
): Promise<string> {
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
export function formatAmount(minorUnits: number, symbol: string, decimals: number): string {
  if (decimals <= 0) return `${symbol}${Math.round(minorUnits).toLocaleString("en-US")}`;
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
export function buildInvoice(args: {
  invoiceNumber: string;
  uid: string;
  customerName: string;
  customerEmail: string;
  paymentId: string;
  orderId: string;
  planId: string;
  amount: number;
  currency: string;
  country: string;
  pricing: CountryPricing | undefined;
  taxConfig: TaxConfig;
  issuedAt: number;
  periodEnd: number;
}): InvoiceRecord {
  const label = planLabel(args.planId);
  const tax = computeTax({
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

export function invoiceRef(uid: string, invoiceNumber: string): FirebaseFirestore.DocumentReference {
  return db().collection("subscriptions").doc(uid).collection("invoices").doc(invoiceNumber);
}

/** The pricing row an invoice should be formatted with, or undefined if the country is gone. */
export function pricingRowFor(catalog: PlanCatalog, country: string): CountryPricing | undefined {
  return pricingFor(catalog, country);
}
