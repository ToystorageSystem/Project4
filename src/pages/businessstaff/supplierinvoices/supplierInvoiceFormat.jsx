import { formatBusinessStatus } from "../../../utils/viStatus";
export const SUPPLIER_INVOICE_STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "DRAFT", label: "Draft Order" },
    { value: "PENDING_VERIFICATION", label: "Pending Verification" },
    { value: "VERIFIED", label: "Confirmed" },
    { value: "PARTIALLY_PAID", label: "Partially Paid" },
    { value: "PAID", label: "Paid" },
    { value: "OVERDUE", label: "Payment Overdue" },
    { value: "CANCELLED", label: "Cancelled" },
];

const statusLabels = Object.fromEntries(
    SUPPLIER_INVOICE_STATUS_OPTIONS
        .filter(({ value }) => value)
        .map(({ value, label }) => [value, label])
);

const statusStyles = {
    DRAFT: "bg-slate-100 text-slate-700",
    PENDING_VERIFICATION: "bg-amber-100 text-amber-800",
    VERIFIED: "bg-emerald-100 text-emerald-800",
    PARTIALLY_PAID: "bg-blue-100 text-blue-800",
    PAID: "bg-emerald-100 text-emerald-800",
    OVERDUE: "bg-rose-100 text-rose-800",
    CANCELLED: "bg-slate-200 text-slate-700",
};

export function SupplierInvoiceStatusBadge({ status }) {
    return (
        <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold ${statusStyles[status] || "bg-slate-100 text-slate-700"}`}>
            {statusLabels[status] || (status ? formatBusinessStatus(status) : "—")}
        </span>
    );
}

export function formatSupplierInvoiceMoney(value) {
    if (value === null || value === undefined || value === "") return "—";
    const number = Number(value);
    return Number.isFinite(number)
        ? `${new Intl.NumberFormat("en-GB", { maximumFractionDigits: 2 }).format(number)} VND`
        : "—";
}

// LocalDate has no timezone: don't convert YYYY-MM-DD with Date().
export function formatSupplierInvoiceDate(value, includeTime = false) {
    if (!value) return "—";
    const match = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(String(value));
    if (!match) return String(value);
    const date = `${match[3]}/${match[2]}/${match[1]}`;
    return includeTime && match[4] ? `${match[4]}:${match[5]} ${date}` : date;
}

export function SupplierInvoiceReconciliation({ invoice }) {
    if (invoice.purchaseOrderMatched === true) {
        return <span className="font-semibold text-emerald-700">Matches Purchase Order</span>;
    }

    if (invoice.purchaseOrderMatched === false) {
        const difference = Number(invoice.amountDifference);
        const amount = Number.isFinite(difference)
            ? formatSupplierInvoiceMoney(Math.abs(difference))
            : "amount unknown";
        return (
            <div className="space-y-1" title={invoice.comparisonWarning || undefined}>
                <span className="font-semibold text-rose-700">Does Not Match Purchase Order</span>
                <p className="text-xs text-rose-700">Difference {amount} (before tax)</p>
            </div>
        );
    }

    return <span className="text-slate-500">No Result Yet</span>;
}

export function SupplierInvoiceMatchBadge({ matched }) {
    if (matched === true) return <span className="inline-flex rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-800">Matches Purchase Order</span>;
    if (matched === false) return <span className="inline-flex rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">Differs from Purchase Order</span>;
    return <span className="text-xs text-slate-500">No Result Yet</span>;
}

