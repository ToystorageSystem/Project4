import { formatBusinessStatus } from "../../../utils/viStatus";
const STATUS = {
    PENDING: { label: "Pending Approval", color: "bg-amber-100 text-amber-800" },
    PENDING_CREATE: { label: "Pending Creation Approval", color: "bg-amber-100 text-amber-800" },
    PENDING_UPDATE: { label: "Pending Update Approval", color: "bg-amber-100 text-amber-800" },
    ACTIVE: { label: "Active", color: "bg-emerald-100 text-emerald-800" },
    INACTIVE: { label: "Hidden", color: "bg-slate-200 text-slate-700" },
    DISCONTINUED: { label: "Discontinued", color: "bg-slate-200 text-slate-700" },
    REJECTED: { label: "Rejected", color: "bg-red-100 text-red-800" },
    APPROVED: { label: "Approved", color: "bg-emerald-100 text-emerald-800" },
};

export default function ProductStatusBadge({ status }) {
    const item = STATUS[status] || { label: status ? formatBusinessStatus(status) : "—", color: "bg-slate-100 text-slate-700" };
    return (
        <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold ${item.color}`}>
            {item.label}
        </span>
    );
}

export function formatProductPrice(value) {
    const number = Number(value);
    return Number.isFinite(number) ? new Intl.NumberFormat("en-GB").format(number) + " ₫" : "—";
}

export function formatProductDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-GB");
}

