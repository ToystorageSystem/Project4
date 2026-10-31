import { useEffect, useMemo, useState } from "react";
import {
    getBusinessManagerPurchaseOrderDetail,
    listBusinessManagerPurchaseOrders,
} from "../../api/businessmanager/purchaseOrders";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";

const EMPTY_FILTERS = {
    keyword: "",
    supplierId: "",
    status: "",
    createdFrom: "",
    createdTo: "",
};
const PAGE_SIZE = 10;
const inputClass = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
const labelClass = "mb-1.5 block text-xs font-semibold text-slate-600";
const buttonClass = "rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50";

const STATUS_LABELS = {
    DRAFT: "Nháp",
    PENDING: "Chờ xử lý",
    PENDING_APPROVAL: "Chờ duyệt",
    APPROVED: "Đã duyệt",
    ORDERED: "Đã đặt hàng",
    RECEIVING: "Đang nhận hàng",
    PARTIALLY_RECEIVED: "Nhận một phần",
    COMPLETED: "Hoàn thành",
    REJECTED: "Đã từ chối",
    CANCELLED: "Đã hủy",
};

function display(value) {
    return value === null || value === undefined || value === "" ? "—" : String(value);
}

function integer(value) {
    if (value === null || value === undefined || value === "") return "—";
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed.toLocaleString("vi-VN") : String(value);
}

function money(value) {
    if (value === null || value === undefined || value === "") return "—";
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return String(value);
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0,
    }).format(parsed);
}

function time(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleString("vi-VN", {
        day: "2-digit", month: "2-digit", year: "numeric",
        hour: "2-digit", minute: "2-digit",
    });
}

function dateOnly(value) {
    if (!value) return "—";
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
    return match ? `${match[3]}/${match[2]}/${match[1]}` : String(value);
}

function statusName(status) {
    return STATUS_LABELS[status] || (status ? status.replaceAll("_", " ") : "—");
}

function StatusBadge({ value }) {
    const success = ["APPROVED", "COMPLETED"].includes(value);
    const danger = ["CANCELLED", "REJECTED"].includes(value);
    const color = success
        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
        : danger
            ? "border-rose-200 bg-rose-50 text-rose-700"
            : value === "DRAFT"
                ? "border-slate-200 bg-slate-100 text-slate-700"
                : "border-amber-200 bg-amber-50 text-amber-700";
    return <span className={`inline-block rounded-full border px-2.5 py-1 text-xs font-medium ${color}`}>{statusName(value)}</span>;
}

function apiError(error) {
    const status = error?.response?.status;
    if (status === 401) return "Phiên đăng nhập đã hết hạn (401). Vui lòng đăng nhập lại.";
    if (status === 403) return "Tài khoản không được phép xem đơn mua hàng (403).";
    if (status === 404) return "Không tìm thấy đơn mua hàng (404).";
    const responseMessage = error?.response?.data?.message;
    if (typeof responseMessage === "string" && responseMessage.trim()) return responseMessage;
    return error?.message || "Không thể tải dữ liệu. Vui lòng thử lại.";
}

function cancelled(error) {
    return error?.code === "ERR_CANCELED" || error?.name === "CanceledError" || error?.name === "AbortError";
}

function DetailField({ title, children }) {
    return <div className="min-w-0"><p className="mb-1 text-xs font-medium text-slate-500">{title}</p><div className="break-words text-sm font-semibold text-slate-800">{children ?? "—"}</div></div>;
}

function PurchaseOrderDetail({ order, loading, error, onClose }) {
    useEffect(() => {
        const onKeyDown = (event) => { if (event.key === "Escape") onClose(); };
        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);

    const items = Array.isArray(order?.items) ? order.items : [];

    return <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-3 sm:p-6" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
        <div role="dialog" aria-modal="true" aria-labelledby="po-detail-heading" className="flex max-h-[92vh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
            <div className="flex items-start justify-between gap-3 border-b border-slate-200 px-5 py-4">
                <div>
                    <h2 id="po-detail-heading" className="text-lg font-bold text-slate-900">Chi tiết đơn mua hàng</h2>
                    <p className="mt-1 text-xs text-slate-500">{order?.orderCode || "Đang tải chi tiết..."}</p>
                </div>
                <button type="button" className="rounded-lg px-3 py-2 text-sm text-blue-700 hover:bg-blue-50" onClick={onClose}>Đóng ×</button>
            </div>
            <div className="overflow-y-auto p-5">
                {loading && <div className="py-10 text-center text-sm text-slate-600">Đang tải thông tin đơn mua hàng...</div>}
                {!loading && error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
                {!loading && !error && order && <div className="space-y-5">
                    <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                        <DetailField title="Mã đơn mua">{display(order.orderCode)}</DetailField>
                        <DetailField title="Trạng thái"><StatusBadge value={order.status}/></DetailField>
                        <DetailField title="Nhà cung cấp">{display(order.supplierName)}{order.supplierCode ? <div className="mt-1 text-xs font-normal text-slate-500">{order.supplierCode}</div> : null}</DetailField>
                        <DetailField title="Kho nhập">{display(order.warehouseName)}</DetailField>
                        <DetailField title="Người tạo">{display(order.createdByName || order.createdByCode)}</DetailField>
                        <DetailField title="Thời gian tạo">{time(order.createdAt)}</DetailField>
                        <DetailField title="Ngày dự kiến nhận">{dateOnly(order.expectedDeliveryDate)}</DetailField>
                        <DetailField title="Người duyệt">{display(order.approvedByName || order.approvedByCode)}</DetailField>
                        <DetailField title="Thời gian duyệt">{time(order.approvedAt)}</DetailField>
                        <DetailField title="Cập nhật lần cuối">{time(order.updatedAt)}</DetailField>
                        <DetailField title="Tổng số sản phẩm">{integer(order.totalProducts)}</DetailField>
                        <DetailField title="Tổng số lượng đặt">{integer(order.totalOrderedQuantity)}</DetailField>
                    </div>

                    {(order.note || order.rejectionReason || order.cancelReason) && <div className="space-y-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                        {order.note && <p><strong>Ghi chú:</strong> {order.note}</p>}
                        {order.rejectionReason && <p><strong>Lý do từ chối:</strong> {order.rejectionReason}</p>}
                        {order.cancelReason && <p><strong>Lý do hủy:</strong> {order.cancelReason}</p>}
                    </div>}

                    <div>
                        <h3 className="mb-3 font-bold text-slate-900">Danh sách sản phẩm</h3>
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                            <table className="min-w-[850px] w-full text-left text-sm">
                                <thead className="bg-slate-50 text-xs uppercase text-slate-600"><tr>
                                    <th className="px-4 py-3">Sản phẩm</th>
                                    <th className="px-4 py-3">Đơn vị</th>
                                    <th className="px-4 py-3 text-right">Đặt</th>
                                    <th className="px-4 py-3 text-right">Đã nhận</th>
                                    <th className="px-4 py-3 text-right">Đơn giá</th>
                                    <th className="px-4 py-3 text-right">Thành tiền</th>
                                </tr></thead>
                                <tbody className="divide-y divide-slate-100">
                                    {items.map((item, index) => <tr key={item.id ?? `${item.productId}-${index}`}>
                                        <td className="px-4 py-3"><div className="font-semibold text-slate-800">{display(item.productName)}</div><div className="mt-1 text-xs text-slate-500">{display(item.productCode)}{item.itemCode ? ` · ${item.itemCode}` : ""}</div></td>
                                        <td className="px-4 py-3 text-slate-600">{display(item.baseUnit)}</td>
                                        <td className="px-4 py-3 text-right tabular-nums">{integer(item.orderedQuantity)}</td>
                                        <td className="px-4 py-3 text-right tabular-nums">{integer(item.receivedQuantity)}</td>
                                        <td className="px-4 py-3 text-right tabular-nums">{money(item.unitPrice)}</td>
                                        <td className="px-4 py-3 text-right font-semibold tabular-nums">{money(item.lineTotal)}</td>
                                    </tr>)}
                                    {items.length === 0 && <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-500">Đơn mua chưa có dòng sản phẩm.</td></tr>}
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4">
                        <div className="text-sm text-blue-800">Tổng giá trị đơn mua hàng</div>
                        <div className="text-xl font-bold text-blue-900 tabular-nums">{money(order.totalAmount)}</div>
                    </div>
                </div>}
            </div>
        </div>
    </div>;
}

export default function PurchaseOrdersPage() {
    const [draft, setDraft] = useState(EMPTY_FILTERS);
    const [applied, setApplied] = useState(EMPTY_FILTERS);
    const [records, setRecords] = useState([]);
    const [catalog, setCatalog] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [formError, setFormError] = useState("");
    const [reload, setReload] = useState(0);
    const [page, setPage] = useState(0);
    const [selectedId, setSelectedId] = useState(null);
    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        listBusinessManagerPurchaseOrders({}, controller.signal)
            .then(setCatalog)
            .catch((err) => { if (!cancelled(err)) console.warn("Purchase order filter options:", err); });
        return () => controller.abort();
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listBusinessManagerPurchaseOrders(applied, controller.signal)
            .then((rows) => { if (!controller.signal.aborted) setRecords(rows); })
            .catch((err) => { if (!controller.signal.aborted && !cancelled(err)) { setError(apiError(err)); setRecords([]); } })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [applied, reload]);

    useEffect(() => {
        if (selectedId === null) return undefined;
        const controller = new AbortController();
        setDetail(null);
        setDetailLoading(true);
        setDetailError("");
        getBusinessManagerPurchaseOrderDetail(selectedId, controller.signal)
            .then((result) => { if (!controller.signal.aborted) setDetail(result); })
            .catch((err) => { if (!controller.signal.aborted && !cancelled(err)) setDetailError(apiError(err)); })
            .finally(() => { if (!controller.signal.aborted) setDetailLoading(false); });
        return () => controller.abort();
    }, [selectedId]);

    const supplierOptions = useMemo(() => {
        const map = new Map();
        for (const row of catalog) {
            if (row.supplierId != null && !map.has(String(row.supplierId))) {
                map.set(String(row.supplierId), row.supplierName || row.supplierCode || `Nhà cung cấp #${row.supplierId}`);
            }
        }
        return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1], "vi"));
    }, [catalog]);

    const statusOptions = useMemo(() =>
        [...new Set(catalog.map((row) => row.status).filter(Boolean))].sort(), [catalog]);
    const totalPages = Math.max(1, Math.ceil(records.length / PAGE_SIZE));
    const pageRecords = records.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
    const totalValue = records.reduce((sum, row) => sum + (Number(row.totalAmount) || 0), 0);

    function updateDraft(key, value) {
        setDraft((old) => ({ ...old, [key]: value }));
        setFormError("");
    }

    function applyFilters(event) {
        event.preventDefault();
        if (draft.createdFrom && draft.createdTo && draft.createdFrom > draft.createdTo) {
            setFormError("Ngày bắt đầu không được sau ngày kết thúc.");
            return;
        }
        setFormError("");
        setPage(0);
        setApplied({ ...draft, keyword: draft.keyword.trim() });
    }

    function clearFilters() {
        setDraft({ ...EMPTY_FILTERS });
        setApplied({ ...EMPTY_FILTERS });
        setFormError("");
        setPage(0);
    }

    function closeDetail() {
        setSelectedId(null);
        setDetail(null);
    }

    return <MainLayout title="Business Manager" items={businessManagerMenuItems}>
        <main className="mx-auto w-full max-w-7xl space-y-5 px-4 py-8 sm:px-6 lg:py-10">
            <div>
                <h1 className="text-xl font-bold text-slate-900">Danh sách đơn mua hàng</h1>
                <p className="mt-1 text-sm text-slate-500">Business Manager theo dõi toàn bộ đơn mua hàng trong hệ thống. Chức năng chỉ xem, không chỉnh sửa đơn.</p>
            </div>

            <form onSubmit={applyFilters} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
                    <label><span className={labelClass}>Mã đơn mua hàng</span><input className={inputClass} value={draft.keyword} onChange={(event) => updateDraft("keyword", event.target.value)} placeholder="PO-..." /></label>
                    <label><span className={labelClass}>Nhà cung cấp</span><select className={inputClass} value={draft.supplierId} onChange={(event) => updateDraft("supplierId", event.target.value)}><option value="">Tất cả nhà cung cấp</option>{supplierOptions.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>
                    <label><span className={labelClass}>Trạng thái</span><select className={inputClass} value={draft.status} onChange={(event) => updateDraft("status", event.target.value)}><option value="">Tất cả trạng thái</option>{statusOptions.map((status) => <option key={status} value={status}>{statusName(status)}</option>)}</select></label>
                    <label><span className={labelClass}>Từ ngày tạo</span><input type="date" className={inputClass} value={draft.createdFrom} onChange={(event) => updateDraft("createdFrom", event.target.value)}/></label>
                    <label><span className={labelClass}>Đến ngày tạo</span><input type="date" className={inputClass} value={draft.createdTo} onChange={(event) => updateDraft("createdTo", event.target.value)}/></label>
                </div>
                {formError && <p role="alert" className="mt-3 text-sm text-red-600">{formError}</p>}
                <div className="mt-4 flex flex-wrap items-center gap-2">
                    <button type="submit" className={`${buttonClass} bg-blue-600 text-white hover:bg-blue-700`}>Tìm kiếm / Lọc</button>
                    <button type="button" className={`${buttonClass} border border-slate-200 text-slate-700 hover:bg-slate-50`} onClick={clearFilters}>Xóa bộ lọc</button>
                    <button type="button" className={`${buttonClass} text-blue-700 hover:bg-blue-50`} onClick={() => setReload((n) => n + 1)}>Tải lại</button>
                </div>
            </form>

            <div className="grid gap-3 sm:grid-cols-3">
                <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><div className="text-xs text-slate-500">Số đơn mua hàng</div><div className="mt-1 text-2xl font-bold tabular-nums text-slate-900">{loading ? "..." : integer(records.length)}</div></div>
                <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><div className="text-xs text-slate-500">Tổng số lượng đặt</div><div className="mt-1 text-2xl font-bold tabular-nums text-slate-900">{loading ? "..." : integer(records.reduce((sum, row) => sum + (Number(row.totalOrderedQuantity) || 0), 0))}</div></div>
                <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><div className="text-xs text-slate-500">Tổng giá trị đơn đang hiển thị</div><div className="mt-1 text-xl font-bold tabular-nums text-blue-800">{loading ? "..." : money(totalValue)}</div></div>
            </div>

            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="flex items-center justify-between border-b border-slate-200 px-4 py-4">
                    <h2 className="font-semibold text-slate-900">Danh sách đơn mua hàng</h2>
                    <span className="text-xs text-slate-500">{loading ? "Đang tải..." : `${integer(records.length)} đơn`}</span>
                </div>
                {error && <div role="alert" className="m-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}
                {loading ? <div className="py-12 text-center text-sm text-slate-500">Đang tải danh sách đơn mua hàng...</div> : !error && records.length === 0 ? <div className="py-12 text-center text-sm text-slate-500">Không tìm thấy đơn mua hàng phù hợp.</div> : !error && <div className="overflow-x-auto">
                    <table className="min-w-[1200px] w-full text-left text-sm">
                        <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>
                            <th className="px-4 py-3">Mã đơn</th>
                            <th className="px-4 py-3">Nhà cung cấp</th>
                            <th className="px-4 py-3">Kho nhập</th>
                            <th className="px-4 py-3">Trạng thái</th>
                            <th className="px-4 py-3 text-right">Sản phẩm / Số lượng</th>
                            <th className="px-4 py-3 text-right">Tổng tiền</th>
                            <th className="px-4 py-3">Người tạo</th>
                            <th className="px-4 py-3">Ngày tạo</th>
                            <th className="px-4 py-3 text-right">Thao tác</th>
                        </tr></thead>
                        <tbody className="divide-y divide-slate-100">{pageRecords.map((row) => <tr key={row.id} className="hover:bg-slate-50/60">
                            <td className="px-4 py-3 font-semibold text-slate-900">{display(row.orderCode)}</td>
                            <td className="px-4 py-3">{display(row.supplierName)}</td>
                            <td className="px-4 py-3 text-slate-700">{display(row.warehouseName)}</td>
                            <td className="px-4 py-3"><StatusBadge value={row.status}/></td>
                            <td className="px-4 py-3 text-right tabular-nums">{integer(row.totalProducts)} / {integer(row.totalOrderedQuantity)}</td>
                            <td className="px-4 py-3 text-right font-semibold tabular-nums">{money(row.totalAmount)}</td>
                            <td className="px-4 py-3">{display(row.createdByName || row.createdByCode)}</td>
                            <td className="px-4 py-3 whitespace-nowrap text-slate-600">{time(row.createdAt)}</td>
                            <td className="px-4 py-3 text-right"><button type="button" className="font-semibold text-blue-700 hover:underline" onClick={() => setSelectedId(row.id)}>Chi tiết</button></td>
                        </tr>)}</tbody>
                    </table>
                </div>}
                {!loading && !error && records.length > 0 && <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm text-slate-500">
                    <span>Trang {page + 1} / {totalPages}</span>
                    <div className="flex gap-2"><button type="button" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className={`${buttonClass} border border-slate-200 text-slate-700 disabled:text-slate-400`}>Trước</button><button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} className={`${buttonClass} border border-slate-200 text-slate-700 disabled:text-slate-400`}>Sau</button></div>
                </div>}
            </section>
        </main>
        {selectedId !== null && <PurchaseOrderDetail order={detail} loading={detailLoading} error={detailError} onClose={closeDetail} />}
    </MainLayout>;
}
