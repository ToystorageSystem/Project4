const STATUS = {
    PENDING: { label: "Chờ duyệt", color: "bg-amber-100 text-amber-800" },
    PENDING_CREATE: { label: "Chờ duyệt tạo", color: "bg-amber-100 text-amber-800" },
    PENDING_UPDATE: { label: "Chờ duyệt sửa", color: "bg-amber-100 text-amber-800" },
    ACTIVE: { label: "Hoạt động", color: "bg-emerald-100 text-emerald-800" },
    INACTIVE: { label: "Đã ẩn", color: "bg-slate-200 text-slate-700" },
    DISCONTINUED: { label: "Ngừng kinh doanh", color: "bg-slate-200 text-slate-700" },
    REJECTED: { label: "Bị từ chối", color: "bg-red-100 text-red-800" },
    APPROVED: { label: "Đã duyệt", color: "bg-emerald-100 text-emerald-800" },
};

export default function ProductStatusBadge({ status }) {
    const item = STATUS[status] || { label: status || "—", color: "bg-slate-100 text-slate-700" };
    return (
        <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold ${item.color}`}>
            {item.label}
        </span>
    );
}

export function formatProductPrice(value) {
    const number = Number(value);
    return Number.isFinite(number) ? new Intl.NumberFormat("vi-VN").format(number) + " ₫" : "—";
}

export function formatProductDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("vi-VN");
}
