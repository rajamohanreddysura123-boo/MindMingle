"use strict";
/**
 * MindMingle+ price list, one entry per market.
 *
 * This table is the *seed*: `appConfig/plans` in Firestore is what actually gets charged,
 * and an admin can retune any row from the in-app Plan Pricing screen without a release.
 * When that doc is missing (fresh project) these values are used and written back.
 *
 * Amounts are in the currency's smallest unit, which is what Razorpay expects:
 *   decimals 2 -> paise/cents  (INR 99.00 = 9900)
 *   decimals 0 -> whole units  (JPY 399 = 399)
 *   decimals 3 -> fils         (KWD 0.999 = 999)
 *
 * `dialCode` is how a user's country is resolved server-side: profiles store the phone as
 * "+91 9876543210", so the longest matching dial code wins. Nothing about pricing is taken
 * from the client.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_PLAN_CATALOG = exports.PLAN_DAYS = void 0;
exports.pricingFor = pricingFor;
exports.countryFromPhone = countryFromPhone;
exports.isValidPricing = isValidPricing;
/** Days of access each plan id buys. The ids themselves are fixed; only prices are tunable. */
exports.PLAN_DAYS = {
    plus_monthly: 30,
    plus_annual: 365,
};
const inr = (monthly, annual) => ({
    currency: "INR",
    symbol: "₹",
    decimals: 2,
    dialCode: "+91",
    monthly,
    annual,
});
const eur = (dialCode) => ({
    currency: "EUR",
    symbol: "€",
    decimals: 2,
    dialCode,
    monthly: 299,
    annual: 2999,
});
exports.DEFAULT_PLAN_CATALOG = {
    enabled: true,
    // Anything not listed below is charged the US row, in USD.
    defaultCountry: "US",
    countries: {
        IN: inr(9900, 99900),
        US: { currency: "USD", symbol: "$", decimals: 2, dialCode: "+1", monthly: 299, annual: 2999 },
        CA: { currency: "CAD", symbol: "CA$", decimals: 2, dialCode: "+1", monthly: 399, annual: 3999 },
        GB: { currency: "GBP", symbol: "£", decimals: 2, dialCode: "+44", monthly: 249, annual: 2499 },
        CH: { currency: "CHF", symbol: "CHF", decimals: 2, dialCode: "+41", monthly: 299, annual: 2999 },
        DE: eur("+49"),
        FR: eur("+33"),
        IT: eur("+39"),
        ES: eur("+34"),
        NL: eur("+31"),
        BE: eur("+32"),
        AT: eur("+43"),
        IE: eur("+353"),
        PT: eur("+351"),
        FI: eur("+358"),
        GR: eur("+30"),
        SE: { currency: "SEK", symbol: "kr", decimals: 2, dialCode: "+46", monthly: 3499, annual: 34999 },
        NO: { currency: "NOK", symbol: "kr", decimals: 2, dialCode: "+47", monthly: 3499, annual: 34999 },
        DK: { currency: "DKK", symbol: "kr", decimals: 2, dialCode: "+45", monthly: 2499, annual: 24999 },
        PL: { currency: "PLN", symbol: "zł", decimals: 2, dialCode: "+48", monthly: 1299, annual: 12999 },
        CZ: { currency: "CZK", symbol: "Kč", decimals: 2, dialCode: "+420", monthly: 6900, annual: 69000 },
        HU: { currency: "HUF", symbol: "Ft", decimals: 2, dialCode: "+36", monthly: 99900, annual: 999000 },
        RO: { currency: "RON", symbol: "lei", decimals: 2, dialCode: "+40", monthly: 1399, annual: 13999 },
        TR: { currency: "TRY", symbol: "₺", decimals: 2, dialCode: "+90", monthly: 9900, annual: 99900 },
        AU: { currency: "AUD", symbol: "A$", decimals: 2, dialCode: "+61", monthly: 449, annual: 4499 },
        NZ: { currency: "NZD", symbol: "NZ$", decimals: 2, dialCode: "+64", monthly: 499, annual: 4999 },
        AE: { currency: "AED", symbol: "AED", decimals: 2, dialCode: "+971", monthly: 1199, annual: 11999 },
        SA: { currency: "SAR", symbol: "SAR", decimals: 2, dialCode: "+966", monthly: 1199, annual: 11999 },
        QA: { currency: "QAR", symbol: "QAR", decimals: 2, dialCode: "+974", monthly: 1099, annual: 10999 },
        KW: { currency: "KWD", symbol: "KWD", decimals: 3, dialCode: "+965", monthly: 999, annual: 9990 },
        BH: { currency: "BHD", symbol: "BHD", decimals: 3, dialCode: "+973", monthly: 1199, annual: 11990 },
        OM: { currency: "OMR", symbol: "OMR", decimals: 3, dialCode: "+968", monthly: 1199, annual: 11990 },
        IL: { currency: "ILS", symbol: "₪", decimals: 2, dialCode: "+972", monthly: 1190, annual: 11900 },
        EG: { currency: "EGP", symbol: "E£", decimals: 2, dialCode: "+20", monthly: 9900, annual: 99900 },
        SG: { currency: "SGD", symbol: "S$", decimals: 2, dialCode: "+65", monthly: 399, annual: 3999 },
        MY: { currency: "MYR", symbol: "RM", decimals: 2, dialCode: "+60", monthly: 1299, annual: 12999 },
        TH: { currency: "THB", symbol: "฿", decimals: 2, dialCode: "+66", monthly: 9900, annual: 99900 },
        ID: { currency: "IDR", symbol: "Rp", decimals: 2, dialCode: "+62", monthly: 3900000, annual: 39000000 },
        PH: { currency: "PHP", symbol: "₱", decimals: 2, dialCode: "+63", monthly: 14900, annual: 149000 },
        VN: { currency: "VND", symbol: "₫", decimals: 0, dialCode: "+84", monthly: 69000, annual: 690000 },
        JP: { currency: "JPY", symbol: "¥", decimals: 0, dialCode: "+81", monthly: 399, annual: 3990 },
        KR: { currency: "KRW", symbol: "₩", decimals: 0, dialCode: "+82", monthly: 3900, annual: 39000 },
        CN: { currency: "CNY", symbol: "CN¥", decimals: 2, dialCode: "+86", monthly: 1990, annual: 19900 },
        HK: { currency: "HKD", symbol: "HK$", decimals: 2, dialCode: "+852", monthly: 2500, annual: 24900 },
        TW: { currency: "TWD", symbol: "NT$", decimals: 2, dialCode: "+886", monthly: 9900, annual: 99000 },
        LK: { currency: "LKR", symbol: "Rs", decimals: 2, dialCode: "+94", monthly: 89900, annual: 899000 },
        NP: { currency: "NPR", symbol: "Rs", decimals: 2, dialCode: "+977", monthly: 39900, annual: 399900 },
        BD: { currency: "BDT", symbol: "৳", decimals: 2, dialCode: "+880", monthly: 29900, annual: 299900 },
        PK: { currency: "PKR", symbol: "Rs", decimals: 2, dialCode: "+92", monthly: 79900, annual: 799900 },
        MU: { currency: "MUR", symbol: "Rs", decimals: 2, dialCode: "+230", monthly: 13900, annual: 139000 },
        ZA: { currency: "ZAR", symbol: "R", decimals: 2, dialCode: "+27", monthly: 4900, annual: 49900 },
        NG: { currency: "NGN", symbol: "₦", decimals: 2, dialCode: "+234", monthly: 250000, annual: 2499900 },
        KE: { currency: "KES", symbol: "KSh", decimals: 2, dialCode: "+254", monthly: 39900, annual: 399900 },
        GH: { currency: "GHS", symbol: "GH₵", decimals: 2, dialCode: "+233", monthly: 3900, annual: 39900 },
        MX: { currency: "MXN", symbol: "MX$", decimals: 2, dialCode: "+52", monthly: 5900, annual: 59000 },
        BR: { currency: "BRL", symbol: "R$", decimals: 2, dialCode: "+55", monthly: 1490, annual: 14900 },
        AR: { currency: "ARS", symbol: "AR$", decimals: 2, dialCode: "+54", monthly: 290000, annual: 2900000 },
        CL: { currency: "CLP", symbol: "CLP$", decimals: 0, dialCode: "+56", monthly: 2900, annual: 29000 },
        CO: { currency: "COP", symbol: "COL$", decimals: 2, dialCode: "+57", monthly: 1190000, annual: 11900000 },
        PE: { currency: "PEN", symbol: "S/", decimals: 2, dialCode: "+51", monthly: 1190, annual: 11900 },
    },
};
/** Picks the plan row for a country, falling back to the catalog's default market. */
function pricingFor(catalog, countryCode) {
    return catalog.countries[countryCode] ?? catalog.countries[catalog.defaultCountry];
}
/**
 * Resolves a country from a stored profile phone number ("+91 9876543210"). Longest dial
 * code wins, so +1 (US) never shadows +1 area-specific rows and +971 beats +97.
 */
function countryFromPhone(catalog, phoneNumber) {
    const digits = phoneNumber.replace(/[^\d+]/g, "");
    if (!digits.startsWith("+"))
        return undefined;
    let best;
    for (const [code, pricing] of Object.entries(catalog.countries)) {
        const dial = pricing.dialCode.replace(/[^\d+]/g, "");
        if (dial.length > 1 && digits.startsWith(dial)) {
            if (!best || dial.length > best.length) {
                best = { code, length: dial.length };
            }
        }
    }
    return best?.code;
}
/** Sanity check for admin-supplied rows before they reach Firestore or an order. */
function isValidPricing(value) {
    const row = value;
    return (!!row &&
        typeof row.currency === "string" &&
        /^[A-Z]{3}$/.test(row.currency) &&
        typeof row.decimals === "number" &&
        row.decimals >= 0 &&
        row.decimals <= 3 &&
        typeof row.monthly === "number" &&
        row.monthly > 0 &&
        typeof row.annual === "number" &&
        row.annual > 0);
}
//# sourceMappingURL=pricing.js.map