import { useEffect, useMemo, useState } from "react";
import {
    getBusinessActivityExcel,
    getBusinessActivityReport,
} from "../../api/businessmanager/businessActivityReports";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";

const TYPES = [
    { id: "imports", label: "Báo cáo nhập hàng", short: "Nhập hàng", file: "bao-cao-nhap-hang.xlsx" },
    { id: "exports", label: "Báo cáo xuất hàng", short: "Xuất hàng", file: "bao-cao-xuat-hang.xlsx" },
    { id: "transfers", label: "Báo cáo điều chuyển", short: "Điều chuyển", file: "bao-cao-dieu-chuyen.xlsx" },
    { id: "inventory", label: "Báo cáo tồn kho", short: "Tồn kho", file: "bao-cao-ton-kho.xlsx" },
];

const METRICS = {
    imports: [
        ["totalReceipts", "Tổng phiếu nhập"],
        ["completedReceipts", "Phiếu hoàn thành"],
        ["totalAcceptedQuantity", "Số lượng chấp nhận"],
    ],
    exports: [
        ["totalExports", "Tổng phiếu xuất"],
        ["shippedExports", "Phiếu đã xuất hàng"],
        ["totalShippedQuantity", "Số lượng đã gửi"],
    ],
    transfers: [
        ["totalTransfers", "Tổng phiếu điều chuyển"],
        ["completedTransfers", "Phiếu hoàn thành"],
        ["totalShippedQuantity", "Số lượng đã gửi"],
    ],
    inventory: [
        ["totalProducts", "Sản phẩm khác nhau"],
        ["totalBalanceRecords", "Dòng tồn kho"],
        ["totalQuantity", "Tồn vật lý"],
        ["totalReservedQuantity", "Đang giữ chỗ"],
        ["totalAvailableQuantity", "Tồn khả dụng"],
    ],
};

const COLUMNS = {
    imports: [
        ["receiptCode", "Mã phiếu"],
        ["warehouseName", "Kho nhập"],
        ["status", "Trạng thái"],
        ["createdAt", "Ngày tạo"],
        ["receivedAt", "Ngày nhận"],
        ["acceptedQuantity", "Số lượng chấp nhận"],
    ],
    exports: [
        ["transferCode", "Mã phiếu"],
        ["fromWarehouseName", "Kho xuất"],
        ["toWarehouseName", "Kho nhận"],
        ["status", "Trạng thái"],
        ["createdAt", "Ngày tạo"],
        ["shippedAt", "Ngày xuất"],
        ["shippedQuantity", "Số lượng đã gửi"],
    ],
    transfers: [
        ["transferCode", "Mã phiếu"],
        ["fromWarehouseName", "Kho gửi"],
        ["toWarehouseName", "Kho nhận"],
        ["status", "Trạng thái"],
        ["createdAt", "Ngày tạo"],
        ["shippedAt", "Ngày xuất"],
        ["shippedQuantity", "Số lượng đã gửi"],
    ],
    inventory: [
        ["productCode", "Mã sản phẩm"],
        ["productName", "Sản phẩm"],
        ["warehouseName", "Kho"],
        ["locationCode", "Mã vị trí"],
        ["locationType", "Loại vị trí"],
        ["quantity", "Tồn vật lý"],
        ["reservedQuantity", "Giữ chỗ"],
        ["availableQuantity", "Khả dụng"],
    ],
};

const STATUS_LABELS = {
    DRAFT: "Nháp",
    CREATED: "Đã tạo",
    RECEIVING: "Đang nhận",
    INSPECTED: "Đã kiểm hàng",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    PICKING: "Đang lấy hàng",
    PACKING: "Đang đóng gói",
    SHIPPED: "Đã xuất hàng",
    IN_TRANSIT: "Đang vận chuyển",
    CONFIRMED: "Đã xác nhận",
};

const PAGE_SIZE = 12;
const EMPTY_FILTER = { warehouseId: "", fromDate: "", toDate: "" };
const inputClass = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
const labelClass = "mb-1.5 block text-xs font-semibold text-slate-600";
const buttonClass = "rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50";

function display(value) {
    return value === null || value === undefined || value === "" ? "—" : String(value);
}

function number(value) {
    if (value === null || value === undefined || value === "") return "—";
    const result = Number(value);
    return Number.isFinite(result) ? result.toLocaleString("vi-VN") : String(value);
}

// Java LocalDateTime contains no timezone. Preserve the clock time returned by the server.
function dateTime(value) {
    if (!value) return "—";
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    return match ? `${match[3]}/${match[2]}/${match[1]} ${match[4]}:${match[5]}` : display(value);
}

function formatCell(key, value) {
    if (key === "createdAt" || key === "receivedAt" || key === "shippedAt") return dateTime(value);
    if (key === "status") return STATUS_LABELS[value] || display(value);
    if (/Quantity$/.test(key) || key === "quantity") return number(value);
    return display(value);
}

function toFilters(fields, type) {
    return {
        warehouseId: fields.warehouseId.trim(),
        fromDate: type === "inventory" ? "" : fields.fromDate,
        toDate: type === "inventory" ? "" : fields.toDate,
    };
}

function validateFilters(filters) {
    if (filters.warehouseId && (!/^[1-9][0-9]*$/.test(filters.warehouseId) || !Number.isSafeInteger(Number(filters.warehouseId)))) {
        return "ID kho phải là số nguyên dương.";
    }
    if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
        return "Ngày bắt đầu không được sau ngày kết thúc.";
    }
    return "";
}

function requestError(error) {
    if (error?.response?.status === 401) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    if (error?.response?.status === 403) return "Tài khoản không có quyền Business Manager (403).";
    const body = error?.response?.data;
    if (typeof body?.message === "string" && body.message.trim()) return body.message;
    if (typeof body === "string" && body.trim()) return body;
    return "Không tải được báo cáo. Kiểm tra Backend và thử lại.";
}

async function excelError(error) {
    const blob = error?.response?.data;
    if (blob && typeof blob.text === "function") {
        try {
            const text = await blob.text();
            const parsed = JSON.parse(text);
            if (typeof parsed?.message === "string") return parsed.message;
        } catch {
            // No JSON body returned by the server.
        }
    }
    return requestError(error);
}

function Status({ value }) {
    const isComplete = value === "COMPLETED" || value === "CONFIRMED";
    const isCancelled = value === "CANCELLED";
    const color = isComplete
        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
        : isCancelled
            ? "border-red-200 bg-red-50 text-red-700"
            : "border-amber-200 bg-amber-50 text-amber-800";
    return <span className={`inline-flex whitespace-nowrap rounded-full border px-2.5 py-1 text-xs font-medium ${color}`}>
        {STATUS_LABELS[value] || display(value)}
    </span>;
}

export default function BusinessActivityReportsPage() {
    const [type, setType] = useState("imports");
    const [draft, setDraft] = useState(EMPTY_FILTER);
    const [applied, setApplied] = useState(EMPTY_FILTER);
    const [reload, setReload] = useState(0);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [filterError, setFilterError] = useState("");
    const [exporting, setExporting] = useState(false);
    const [exportError, setExportError] = useState("");
    const [page, setPage] = useState(0);

    const current = TYPES.find((entry) => entry.id === type) || TYPES[0];
    const filtered = useMemo(() => toFilters(applied, type), [applied, type]);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        setReport(null);
        setPage(0);
        getBusinessActivityReport(type, filtered, controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) {
                    if (!data || typeof data !== "object" || Array.isArray(data) ||
                        (data.records != null && !Array.isArray(data.records))) {
                        throw new Error("Dữ liệu báo cáo không đúng định dạng.");
                    }
                    setReport({ ...data, records: Array.isArray(data.records) ? data.records : [] });
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted && err?.code !== "ERR_CANCELED") {
                    setError(err?.message === "Dữ liệu báo cáo không đúng định dạng."
                        ? err.message : requestError(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [type, filtered, reload]);

    const rows = Array.isArray(report?.records) ? report.records : [];
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    const pageRows = rows.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
    const breakdown = type === "transfers" && report?.transfersByStatus
        && typeof report.transfersByStatus === "object"
        ? Object.entries(report.transfersByStatus).filter(([, count]) => Number(count) > 0)
        : [];
    const maxStatus = Math.max(1, ...breakdown.map(([, count]) => Number(count) || 0));

    function applyFilters(event) {
        event.preventDefault();
        const next = toFilters(draft, type);
        const message = validateFilters(next);
        setFilterError(message);
        if (message) return;
        setApplied(next);
        setReload((value) => value + 1);
        setExportError("");
    }

    function clearFilters() {
        setDraft({ ...EMPTY_FILTER });
        setApplied({ ...EMPTY_FILTER });
        setReload((value) => value + 1);
        setFilterError("");
        setExportError("");
    }

    async function exportExcel() {
        const next = toFilters(draft, type);
        const message = validateFilters(next);
        setFilterError(message);
        if (message || exporting) return;
        // Keep visible report and downloaded Excel on the same filter selection.
        const filtersChanged = JSON.stringify(next) !== JSON.stringify(toFilters(applied, type));
        if (filtersChanged) {
            setApplied(next);
            setReload((value) => value + 1);
        }
        setExporting(true);
        setExportError("");
        try {
            const response = await getBusinessActivityExcel(type, next);
            const data = response?.data;
            if (!(data instanceof Blob) || data.size === 0) {
                throw new Error("Backend không trả về file Excel hợp lệ.");
            }
            const contentType = String(response?.headers?.["content-type"] || data.type || "").toLowerCase();
            if (contentType.includes("json") || contentType.includes("text/html")) {
                throw new Error("Backend trả về dữ liệu không phải Excel.");
            }
            const url = URL.createObjectURL(data);
            const anchor = document.createElement("a");
            anchor.href = url;
            anchor.download = current.file;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.setTimeout(() => URL.revokeObjectURL(url), 1500);
        } catch (err) {
            setExportError(err?.message?.startsWith("Backend") ? err.message : await excelError(err));
        } finally {
            setExporting(false);
        }
    }

    return (
        <MainLayout title="Business Manager" items={businessManagerMenuItems}>
            <div className="mx-auto max-w-7xl space-y-5 px-3 py-6 sm:px-6 lg:py-10">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">Xem báo cáo hoạt động</h1>
                    <p className="mt-1 text-sm text-slate-500">Thống kê toàn hệ thống theo dữ liệu Backend. Chức năng chỉ xem và xuất Excel.</p>
                </div>

                <div className="flex flex-wrap gap-2" role="tablist" aria-label="Loại báo cáo">
                    {TYPES.map((entry) => (
                        <button key={entry.id} type="button" role="tab" aria-selected={type === entry.id}
                            onClick={() => { setType(entry.id); setFilterError(""); setExportError(""); }}
                            className={`${buttonClass} ${type === entry.id ? "bg-blue-700 text-white shadow-sm" : "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50"}`}>
                            {entry.short}
                        </button>
                    ))}
                </div>

                <form onSubmit={applyFilters} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
                    <div className="grid gap-3 sm:grid-cols-3">
                        <label className="block">
                            <span className={labelClass}>ID kho (để trống = tất cả)</span>
                            <input className={inputClass} type="text" inputMode="numeric" placeholder="Ví dụ: 9"
                                value={draft.warehouseId} onChange={(event) => setDraft((old) => ({ ...old, warehouseId: event.target.value }))}/>
                        </label>
                        {type !== "inventory" ? <>
                            <label className="block">
                                <span className={labelClass}>Từ ngày</span>
                                <input className={inputClass} type="date" value={draft.fromDate}
                                    onChange={(event) => setDraft((old) => ({ ...old, fromDate: event.target.value }))}/>
                            </label>
                            <label className="block">
                                <span className={labelClass}>Đến ngày</span>
                                <input className={inputClass} type="date" value={draft.toDate}
                                    onChange={(event) => setDraft((old) => ({ ...old, toDate: event.target.value }))}/>
                            </label>
                        </> : <div className="sm:col-span-2 flex items-center text-sm text-slate-500">
                            Báo cáo tồn kho là số liệu tại thời điểm hiện tại, không có bộ lọc ngày.
                        </div>}
                    </div>
                    {filterError && <p role="alert" className="mt-3 text-sm text-red-600">{filterError}</p>}
                    <div className="mt-4 flex flex-wrap items-center gap-2">
                        <button type="submit" className={`${buttonClass} bg-blue-700 text-white hover:bg-blue-800`}>Xem báo cáo / Lọc</button>
                        <button type="button" onClick={clearFilters} className={`${buttonClass} border border-slate-200 bg-white text-slate-700 hover:bg-slate-50`}>Xóa bộ lọc</button>
                        <button type="button" onClick={() => setReload((value) => value + 1)} className={`${buttonClass} text-blue-700 hover:bg-blue-50`}>Tải lại</button>
                        <button type="button" onClick={exportExcel} disabled={exporting}
                            className={`${buttonClass} ml-auto bg-emerald-600 text-white hover:bg-emerald-700`}>
                            {exporting ? "Đang xuất Excel..." : "Xuất báo cáo Excel"}
                        </button>
                    </div>
                    {exportError && <p role="alert" className="mt-3 text-sm text-red-600">{exportError}</p>}
                </form>

                {loading ? <div role="status" className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500">Đang tải báo cáo...</div>
                    : error ? <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
                        : report && <>
                            <section aria-label="Chỉ số báo cáo" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
                                {METRICS[type].map(([field, label]) => (
                                    <div key={field} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                                        <div className="text-xs font-semibold text-slate-500">{label}</div>
                                        <div className="mt-2 text-2xl font-bold tabular-nums text-slate-900">{number(report[field])}</div>
                                    </div>
                                ))}
                            </section>

                            {type === "inventory" && <p className="text-sm text-slate-500">Thời điểm dữ liệu tồn kho: <strong>{dateTime(report.snapshotAt)}</strong>. Tồn vật lý, giữ chỗ và khả dụng được hiển thị theo số liệu Backend.</p>}

                            {breakdown.length > 0 && <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                                <h2 className="mb-4 text-sm font-bold text-slate-800">Phiếu điều chuyển theo trạng thái</h2>
                                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                                    {breakdown.map(([status, value]) => (
                                        <div key={status} className="space-y-2">
                                            <div className="flex justify-between gap-3 text-xs text-slate-600"><span>{STATUS_LABELS[status] || status}</span><strong>{number(value)}</strong></div>
                                            <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-blue-600" style={{ width: `${Math.max(0, Math.min(100, Number(value) / maxStatus * 100))}%` }}/></div>
                                        </div>
                                    ))}
                                </div>
                            </section>}

                            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 p-4">
                                    <h2 className="font-bold text-slate-900">Chi tiết {current.label.toLowerCase()}</h2>
                                    <span className="text-xs text-slate-500">{number(rows.length)} dòng · Dữ liệu từ Backend</span>
                                </div>
                                {rows.length === 0 ? <p className="p-10 text-center text-sm text-slate-500">Không có dữ liệu trong bộ lọc đã chọn.</p> : <>
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-slate-200 text-sm">
                                            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                                <tr>{COLUMNS[type].map(([, label]) => <th key={label} scope="col" className="whitespace-nowrap px-4 py-3">{label}</th>)}</tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-100 text-slate-700">
                                                {pageRows.map((row, index) => <tr key={row.id ?? row.inventoryBalanceId ?? `${page}-${index}`} className="hover:bg-slate-50">
                                                    {COLUMNS[type].map(([key]) => <td key={key} className="whitespace-nowrap px-4 py-3">
                                                        {key === "status" ? <Status value={row.status}/> : formatCell(key, row[key])}
                                                    </td>)}
                                                </tr>)}
                                            </tbody>
                                        </table>
                                    </div>
                                    <div className="flex items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-xs text-slate-500">
                                        <span>Trang {page + 1} / {totalPages}</span>
                                        <div className="flex gap-2">
                                            <button type="button" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}
                                                className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Trước</button>
                                            <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
                                                className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Sau</button>
                                        </div>
                                    </div>
                                </>}
                            </section>
                        </>}
            </div>
        </MainLayout>
    );
}
