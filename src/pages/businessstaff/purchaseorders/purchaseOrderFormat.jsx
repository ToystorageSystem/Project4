import { formatBusinessStatus } from "../../../utils/viStatus";
export const PURCHASE_ORDER_STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "DRAFT", label: "Draft Order" },
    { value: "ORDERED", label: "Ordered" },
    { value: "PARTIALLY_RECEIVED", label: "Partially Received" },
    { value: "COMPLETED", label: "Fully Received" },
    { value: "CANCELLED", label: "Cancelled" },
];

const statusLabels = Object.fromEntries(
    PURCHASE_ORDER_STATUS_OPTIONS.filter((option) => option.value).map((option) => [
        option.value,
        option.label,
    ])
);

const statusStyles = {
    DRAFT: "bg-slate-100 text-slate-700",
    ORDERED: "bg-blue-100 text-blue-800",
    PARTIALLY_RECEIVED: "bg-amber-100 text-amber-800",
    COMPLETED: "bg-emerald-100 text-emerald-800",
    CANCELLED: "bg-rose-100 text-rose-800",
};

export function PurchaseOrderStatusBadge({ status }) {
    return (
        <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold ${statusStyles[status] || "bg-slate-100 text-slate-700"}`}>
            {statusLabels[status] || (status ? formatBusinessStatus(status) : "—")}
        </span>
    );
}

export function formatPurchaseOrderMoney(value) {
    if (value === null || value === undefined || value === "") return "—";
    const amount = Number(value);
    return Number.isFinite(amount)
        ? `${new Intl.NumberFormat("en-GB", { maximumFractionDigits: 2 }).format(amount)} VND`
        : "—";
}

export function formatPurchaseOrderDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-GB");
}

export function formatExpectedDeliveryDate(value) {
    if (!value) return "—";
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    return match ? `${match[3]}/${match[2]}/${match[1]}` : String(value);
}
