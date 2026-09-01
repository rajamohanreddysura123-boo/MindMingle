"use strict";
/**
 * Sales tax on MindMingle+ invoices.
 *
 * MindMingle sells to consumers in 60+ countries, and each one decides for itself whether a
 * foreign seller owes tax on a digital subscription. There is no single correct rate to
 * hardcode, and the answer changes with turnover: the EU applies a €10,000 aggregate threshold
 * across all member states before non-EU sellers must register for OSS, Australia A$75,000,
 * Canada CAD 30,000 over a rolling 12 months, while the UK has no threshold at all for sellers
 * with no UK establishment.
 *
 * So nothing here decides anything. Tax is driven entirely by `appConfig/tax`, and the default
 * is to charge none — which is the correct behaviour for a seller who is not registered
 * anywhere. Turn a country on only once you are actually registered to collect there.
 *
 * Two rules are worth understanding before editing that document:
 *
 *   Prices are tax-inclusive. `appConfig/plans` holds what the customer is charged, and a
 *   subscriber in Germany paying €2.99 pays €2.99 — the tax comes out of that, it is not added
 *   on top. Switching `pricesIncludeTax` off means the displayed price stops matching the
 *   amount charged, which is a pricing decision, not an invoicing one.
 *
 *   Selling from India to a customer abroad is an export of services and is zero-rated when the
 *   prescribed conditions are met (payment in convertible foreign exchange, recipient outside
 *   India, and a Letter of Undertaking on file to supply without paying IGST up front). That is
 *   what `lutOnFile` records — it does not make the sale zero-rated, it records that you have
 *   met the condition that allows it.
 *
 * None of this is tax advice. The numbers in this document are yours to set with an accountant.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_TAX_CONFIG = void 0;
exports.computeTax = computeTax;
exports.formatRate = formatRate;
exports.DEFAULT_TAX_CONFIG = {
    // Off until a real registration exists. An unregistered seller charging tax is a worse
    // problem than a registered one failing to.
    enabled: false,
    homeCountry: "IN",
    sellerLegalName: "",
    sellerAddress: "",
    sellerTaxId: "",
    pricesIncludeTax: true,
    lutOnFile: false,
    exportNote: "Export of services — zero rated. Supplied under LUT without payment of IGST.",
    countries: {},
};
/**
 * Splits the charged amount into net and tax.
 *
 * The charged amount never changes. Whether tax is carved out of it or was added on top only
 * decides how the invoice describes it — a customer who paid ₹99 is shown ₹99 either way.
 */
function computeTax(args) {
    const { amount, country, config } = args;
    const none = (note, isExport) => ({
        subtotal: amount,
        taxAmount: 0,
        total: amount,
        taxLabel: "",
        taxRate: 0,
        components: [],
        placeOfSupply: country,
        isExport,
        note,
    });
    if (!config.enabled)
        return none("", false);
    const row = config.countries[country];
    const isExport = country !== config.homeCountry;
    // Not registered to collect here. For a sale out of the home country that is an export and
    // says so on the invoice; otherwise the seller is simply below the threshold.
    if (!row?.registered || row.rate <= 0) {
        if (isExport && config.homeCountry && config.lutOnFile) {
            return none(config.exportNote, true);
        }
        return none("", isExport);
    }
    // Tax-inclusive: the charged amount already contains the tax, so it is carved out rather
    // than added. amount * rate / (10000 + rate) is the tax inside a gross figure.
    const taxAmount = config.pricesIncludeTax
        ? Math.round((amount * row.rate) / (10000 + row.rate))
        : Math.round((amount * row.rate) / 10000);
    const subtotal = config.pricesIncludeTax ? amount - taxAmount : amount;
    const total = config.pricesIncludeTax ? amount : amount + taxAmount;
    const components = [];
    if (row.split) {
        // Halves must add back to the whole: the second component takes the odd minor unit so a
        // rounded half can never leave the invoice a paisa short of what was charged.
        const first = Math.floor(taxAmount / 2);
        const [labelA, labelB] = row.splitLabels ?? ["CGST", "SGST"];
        components.push({ label: labelA, rate: Math.floor(row.rate / 2), amount: first });
        components.push({ label: labelB, rate: row.rate - Math.floor(row.rate / 2), amount: taxAmount - first });
    }
    else {
        components.push({ label: row.label, rate: row.rate, amount: taxAmount });
    }
    return {
        subtotal,
        taxAmount,
        total,
        taxLabel: row.label,
        taxRate: row.rate,
        components,
        placeOfSupply: country,
        isExport,
        note: "",
    };
}
/** A rate as it is printed: 1800 -> "18%", 250 -> "2.5%". */
function formatRate(rate) {
    const percent = rate / 100;
    return `${Number.isInteger(percent) ? percent : percent.toFixed(2)}%`;
}
//# sourceMappingURL=tax.js.map