import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { InvoiceRecord, formatAmount } from "./invoices";
import { formatRate } from "./tax";

/**
 * Transactional email.
 *
 * Nothing here talks to an SMTP server. Every mail is a document in the `mail` collection,
 * which the "Trigger Email" Firestore extension watches and sends — the same channel the
 * desktop sign-in code already uses for OTPs (see requestEmailOtp in index.ts). If the
 * extension is not installed, these documents pile up unsent rather than throwing.
 *
 * Sending must never break a purchase: a user who paid is premium the moment the plan is
 * written, and a mail server having a bad day is not a reason to fail their upgrade. Every
 * function here swallows its errors and logs them.
 */

const db = () => admin.firestore();

const BRAND = "MindMingle";
const SUPPORT_HINT = "Reply to this email or use Help &amp; Support in the app.";

/** Dates on an invoice are read by humans and sometimes by an accountant: 16 Aug 2026. */
function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * Queues one mail document. Callers are already inside a "the payment succeeded" path, so a
 * failure here is logged and dropped rather than propagated.
 */
async function queueMail(args: {
  to: string;
  subject: string;
  text: string;
  html: string;
  context: Record<string, unknown>;
}): Promise<void> {
  if (!args.to) {
    logger.warn("email skipped, no recipient address", args.context);
    return;
  }

  try {
    await db().collection("mail").add({
      to: args.to,
      message: {
        subject: args.subject,
        text: args.text,
        html: args.html,
      },
      createdAt: Date.now(),
    });
    logger.info("email queued", { to: args.to, subject: args.subject, ...args.context });
  } catch (error) {
    logger.error("email could not be queued", { error, ...args.context });
  }
}

/**
 * Who issued the invoice. Only rendered once `appConfig/tax` carries the details — a blank
 * registration line looks like a mistake, and an unregistered seller has none to print.
 */
function sellerBlock(invoice: InvoiceRecord): string {
  const lines = [invoice.sellerLegalName, invoice.sellerAddress, invoice.sellerTaxId]
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (lines.length === 0) return "";
  return `<br/><br/>${lines.map(escapeHtml).join("<br/>")}`;
}

const wrapper = (body: string): string => `
<div style="font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:600px;margin:0 auto;padding:24px;color:#1a1a1a;">
  <h1 style="font-size:20px;margin:0 0 16px;">${BRAND}</h1>
  ${body}
  <p style="font-size:12px;color:#777;margin-top:32px;border-top:1px solid #e5e5e5;padding-top:16px;">
    ${SUPPORT_HINT}
  </p>
</div>`;

/**
 * The one email a successful upgrade sends: confirmation that the plan is live, with the
 * invoice inline. Two separate mails for a single purchase reads as spam and doubles the
 * number of things that can fail, so activation and receipt share one message.
 */
export async function sendInvoiceEmail(invoice: InvoiceRecord): Promise<void> {
  const money = (minor: number) => formatAmount(minor, invoice.symbol, invoice.decimals);
  const amount = money(invoice.total);
  const subtotal = money(invoice.subtotal);
  const activeUntil = formatDate(invoice.periodEnd);
  const issued = formatDate(invoice.issuedAt);
  const name = invoice.customerName || "there";

  // One row per tax component, so a split tax (CGST + SGST) is itemised the way it has to be
  // rather than collapsed into a single figure.
  const taxRows = invoice.taxComponents.map((component) => ({
    label: `${component.label} (${formatRate(component.rate)})`,
    amount: money(component.amount),
  }));

  const subject = `${BRAND}+ is active — invoice ${invoice.invoiceNumber}`;

  const text = [
    `Hi ${name},`,
    "",
    `Your ${invoice.planLabel} subscription is active until ${activeUntil}. Ads are off across the app.`,
    "",
    `Invoice ${invoice.invoiceNumber}`,
    `Date: ${issued}`,
    `Description: ${invoice.description}`,
    `Subtotal: ${subtotal}`,
    ...taxRows.map((row) => `${row.label}: ${row.amount}`),
    `Total paid: ${amount}`,
    `Payment reference: ${invoice.paymentId}`,
    ...(invoice.taxNote ? ["", invoice.taxNote] : []),
    "",
    "Nothing else is needed — the plan is already on your account.",
  ].join("\n");

  const html = wrapper(`
    <p>Hi ${escapeHtml(name)},</p>
    <p>Your <strong>${escapeHtml(invoice.planLabel)}</strong> subscription is active until
       <strong>${activeUntil}</strong>. Ads are off across the app, and nothing else is needed —
       the plan is already on your account.</p>

    <table style="width:100%;border-collapse:collapse;margin:24px 0;font-size:14px;">
      <tr>
        <td colspan="2" style="padding:12px 0;border-bottom:2px solid #1a1a1a;">
          <strong>Invoice ${escapeHtml(invoice.invoiceNumber)}</strong><br/>
          <span style="color:#666;">${issued}</span>
        </td>
      </tr>
      <tr>
        <td style="padding:12px 0;border-bottom:1px solid #e5e5e5;">${escapeHtml(invoice.description)}</td>
        <td style="padding:12px 0;border-bottom:1px solid #e5e5e5;text-align:right;">${subtotal}</td>
      </tr>
      ${taxRows
        .map(
          (row) => `<tr>
        <td style="padding:8px 0;color:#666;">${escapeHtml(row.label)}</td>
        <td style="padding:8px 0;text-align:right;color:#666;">${row.amount}</td>
      </tr>`
        )
        .join("")}
      <tr>
        <td style="padding:12px 0;border-top:2px solid #1a1a1a;"><strong>Total paid</strong></td>
        <td style="padding:12px 0;border-top:2px solid #1a1a1a;text-align:right;"><strong>${amount}</strong></td>
      </tr>
    </table>

    ${invoice.taxNote ? `<p style="font-size:12px;color:#666;">${escapeHtml(invoice.taxNote)}</p>` : ""}

    <p style="font-size:12px;color:#777;">
      Payment reference ${escapeHtml(invoice.paymentId)}<br/>
      Billed in ${escapeHtml(invoice.currency)} (place of supply: ${escapeHtml(invoice.placeOfSupply)})
      ${sellerBlock(invoice)}
    </p>
  `);

  await queueMail({
    to: invoice.customerEmail,
    subject,
    text,
    html,
    context: { uid: invoice.uid, invoiceNumber: invoice.invoiceNumber },
  });
}

/**
 * Sent when a charge did not go through. The important line is that no money was taken —
 * a failed payment that looks like a silent charge is what generates support tickets.
 */
export async function sendPaymentFailedEmail(args: {
  uid: string;
  to: string;
  name: string;
  reason: string;
}): Promise<void> {
  const name = args.name || "there";
  const reason = args.reason || "The payment did not complete.";

  const text = [
    `Hi ${name},`,
    "",
    `Your ${BRAND}+ payment did not go through.`,
    `Reason: ${reason}`,
    "",
    "No money was taken. You can try again any time from the Premium screen in the app.",
  ].join("\n");

  const html = wrapper(`
    <p>Hi ${escapeHtml(name)},</p>
    <p>Your <strong>${BRAND}+</strong> payment did not go through.</p>
    <p style="background:#fdf3f3;border-left:3px solid #d95757;padding:12px;margin:16px 0;">
      ${escapeHtml(reason)}
    </p>
    <p><strong>No money was taken.</strong> You can try again any time from the Premium screen in the app.</p>
  `);

  await queueMail({
    to: args.to,
    subject: `${BRAND}+ payment failed`,
    text,
    html,
    context: { uid: args.uid },
  });
}
