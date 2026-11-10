import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getInventoryWarehouseOptions } from "../../api/business/inventoryReport";
import { aggregateWarehouseProducts } from "../../utils/businessInventorySummary";
import {
    getBusinessActivityExcel,
    getBusinessActivityReport,
} from "../../api/businessmanager/businessActivityReports";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";

const TYPES = [
    { id: "imports", label: "Goods Receipt Report", short: "Goods Receipts", file: "goods-receipt-report.xlsx" },
    { id: "exports", label: "Outbound Stock Report", short: "Outbound Stock", file: "outbound-stock-report.xlsx" },
    { id: "transfers", label: "Stock Transfer Report", short: "Stock Transfers", file: "stock-transfer-report.xlsx" },
    { id: "inventory", label: "Inventory Report", short: "Inventory", file: "inventory-report.xlsx" },
];

const METRICS = {
    imports: [
        ["totalReceipts", "Total Goods Receipts"],
        ["completedReceipts", "Completed Receipts"],
        ["totalAcceptedQuantity", "Accepted Quantity"],
    ],
    exports: [
        ["totalExports", "Total Outbound Transfers"],
        ["shippedExports", "Shipped Transfers"],
        ["totalShippedQuantity", "Shipped Quantity"],
    ],
    transfers: [
        ["totalTransfers", "Total Stock Transfers"],
        ["completedTransfers", "Completed Transfers"],
        ["totalShippedQuantity", "Shipped Quantity"],
    ],
    inventory: [
        ["totalProducts", "Distinct Products"],
        ["warehouseProductCount", "Product / Warehouse Rows"],
        ["totalQuantity", "Physical Stock"],
        ["totalReservedQuantity", "Reserved Stock"],
        ["totalAvailableQuantity", "Available Stock"],
    ],
};

const COLUMNS = {
    imports: [
        ["receiptCode", "Document No."],
        ["warehouseName", "Receiving Warehouse"],
        ["status", "Status"],
        ["createdAt", "Created At"],
        ["receivedAt", "Received At"],
        ["acceptedQuantity", "Accepted Quantity"],
    ],
    exports: [
        ["transferCode", "Document No."],
        ["fromWarehouseName", "Source Warehouse"],
        ["toWarehouseName", "Destination Warehouse / Store"],
        ["status", "Status"],
        ["createdAt", "Created At"],
        ["shippedAt", "Shipped At"],
        ["shippedQuantity", "Shipped Quantity"],
    ],
    transfers: [
        ["transferCode", "Document No."],
        ["fromWarehouseName", "Origin Warehouse"],
        ["toWarehouseName", "Destination Warehouse / Store"],
        ["status", "Status"],
        ["createdAt", "Created At"],
        ["shippedAt", "Shipped At"],
        ["shippedQuantity", "Shipped Quantity"],
    ],
    inventory: [
        ["productCode", "Product Code"],
        ["productName", "Product"],
        ["warehouseName", "Warehouse"],
        ["quantity", "Physical Stock"],
        ["reservedQuantity", "Reserved"],
        ["availableQuantity", "Available"],
    ],
};

const STATUS_LABELS = {
    DRAFT: "Draft",
    CREATED: "Created",
    RECEIVING: "Receiving",
    INSPECTED: "Inspected",
    COMPLETED: "Completed",
    CANCELLED: "Cancelled",
    PICKING: "Picking",
    PACKING: "Packing",
    SHIPPED: "Shipped",
    IN_TRANSIT: "In Transit",
    CONFIRMED: "Confirmed",
};

const PAGE_SIZE = 12;
const EMPTY_FILTER = { keyword: "", warehouseId: "", fromDate: "", toDate: "" };
const inputClass = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
const labelClass = "mb-1.5 block text-xs font-semibold text-slate-600";
const buttonClass = "rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50";

function display(value) {
    return value === null || value === undefined || value === "" ? "—" : String(value);
}

function number(value) {
    if (value === null || value === undefined || value === "") return "—";
    const result = Number(value);
    return Number.isFinite(result) ? result.toLocaleString("en-GB") : String(value);
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
        keyword: String(fields.keyword || "").trim(),
        warehouseId: fields.warehouseId.trim(),
        fromDate: type === "inventory" ? "" : fields.fromDate,
        toDate: type === "inventory" ? "" : fields.toDate,
    };
}

function keywordMeta(type) {
    if (type === "inventory") {
        return {
            label: "Product / Warehouse Search",
            placeholder: "Search by product code, product name, or warehouse...",
        };
    }

    return {
        label: "Document Search",
        placeholder: "Search by document no., warehouse, or status...",
    };
}

function rowMatchesKeyword(row, type, keyword) {
    const term = String(keyword || "").trim().toLocaleLowerCase();
    if (!term) return true;

    const fieldsByType = {
        imports: [row.receiptCode, row.warehouseName, row.status],
        exports: [row.transferCode, row.fromWarehouseName, row.toWarehouseName, row.status],
        transfers: [row.transferCode, row.fromWarehouseName, row.toWarehouseName, row.status],
        inventory: [row.productCode, row.productName, row.warehouseName],
    };

    return (fieldsByType[type] || []).some((value) =>
        String(value ?? "").toLocaleLowerCase().includes(term)
    );
}

function validateFilters(filters) {
    if (filters.warehouseId && (!/^[1-9][0-9]*$/.test(filters.warehouseId) || !Number.isSafeInteger(Number(filters.warehouseId)))) {
        return "Warehouse ID must be a positive integer.";
    }
    if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
        return "Start date cannot be after end date.";
    }
    return "";
}

function requestError(error) {
    if (error?.response?.status === 401) return "Your session has expired. Please sign in again.";
    if (error?.response?.status === 403) return "You do not have permission to view Business Operations reports.";
    const body = error?.response?.data;
    if (typeof body?.message === "string" && body.message.trim()) return body.message;
    if (typeof body === "string" && body.trim()) return body;
    return "Unable to load report. Check your connection and try again.";
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
    const [warehouses, setWarehouses] = useState([]);
    const [warehouseError, setWarehouseError] = useState("");
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

    useEffect(() => {
        const controller = new AbortController();
        getInventoryWarehouseOptions(controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) {
                    if (!Array.isArray(data)) throw new Error("Invalid warehouse list.");
                    setWarehouses(data);
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted && err?.code !== "ERR_CANCELED") {
                    setWarehouseError("Unable to load warehouse options. Try reloading the page.");
                }
            });
        return () => controller.abort();
    }, []);

    const current = TYPES.find((entry) => entry.id === type) || TYPES[0];
    const searchMeta = useMemo(() => keywordMeta(type), [type]);
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
                        throw new Error("Invalid report data format.");
                    }
                    setReport({ ...data, records: Array.isArray(data.records) ? data.records : [] });
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted && err?.code !== "ERR_CANCELED") {
                    setError(err?.message === "Invalid report data format."
                        ? err.message : requestError(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [type, filtered, reload]);

    const rows = useMemo(() => {
        const records = Array.isArray(report?.records) ? report.records : [];
        const baseRows = type === "inventory"
            ? aggregateWarehouseProducts(records, { physicalKey: "quantity" })
            : records;

        return baseRows.filter((row) => rowMatchesKeyword(row, type, filtered.keyword));
    }, [report, type, filtered.keyword]);
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
                throw new Error("Unable to generate a valid Excel file.");
            }
            const contentType = String(response?.headers?.["content-type"] || data.type || "").toLowerCase();
            if (contentType.includes("json") || contentType.includes("text/html")) {
                throw new Error("Unable to download the Excel file. Please try again.");
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
            setExportError(err?.message?.startsWith("Unable") ? err.message : await excelError(err));
        } finally {
            setExporting(false);
        }
    }

    return (
        <MainLayout title="Business Operations" items={businessManagerMenuItems}>
            <div className="mx-auto max-w-7xl space-y-5 px-3 py-6 sm:px-6 lg:py-10">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">View Activity Reports</h1>
                    <p className="mt-1 text-sm text-slate-500">View goods receipts, outbound transfers, stock transfers, and inventory across the system. Export reports to Excel.</p>
                </div>

                <div className="flex flex-wrap gap-2" role="tablist" aria-label="Report Type">
                    {TYPES.map((entry) => (
                        <button key={entry.id} type="button" role="tab" aria-selected={type === entry.id}
                            onClick={() => { setType(entry.id); setFilterError(""); setExportError(""); }}
                            className={`${buttonClass} ${type === entry.id ? "bg-blue-700 text-white shadow-sm" : "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50"}`}>
                            {entry.short}
                        </button>
                    ))}
                </div>

                <form onSubmit={applyFilters} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">

                        {/* Tim kiem theo ma don */}
                        <label className="block">
                            <span className={labelClass}>
                                {searchMeta.label}
                            </span>

                            <input
                                className={inputClass}
                                type="text"
                                placeholder={searchMeta.placeholder}
                                value={draft.keyword}
                                onChange={(event) =>
                                    setDraft((old) => ({
                                        ...old,
                                        keyword: event.target.value
                                    }))
                                }
                            />
                        </label>

                        {/* Loc theo kho hoac cua hang */}
                        <label className="block">
                            <span className={labelClass}>
                                Warehouse / Store
                            </span>

                            <select
                                className={inputClass}
                                value={draft.warehouseId}
                                onChange={(event) =>
                                    setDraft((old) => ({
                                        ...old,
                                        warehouseId: event.target.value
                                    }))
                                }
                            >
                                <option value="">All Warehouses / Stores</option>

                                {warehouses.map((warehouse) => (
                                    <option
                                        key={warehouse.id}
                                        value={String(warehouse.id)}
                                    >
                                        {warehouse.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        {/* Loc theo ngay, khong ap dung cho Inventory */}
                        {type !== "inventory" ? (
                            <>
                                <label className="block">
                                    <span className={labelClass}>
                                        From Date
                                    </span>

                                    <input
                                        className={inputClass}
                                        type="date"
                                        value={draft.fromDate}
                                        onChange={(event) =>
                                            setDraft((old) => ({
                                                ...old,
                                                fromDate: event.target.value
                                            }))
                                        }
                                    />
                                </label>

                                <label className="block">
                                    <span className={labelClass}>
                                        To Date
                                    </span>

                                    <input
                                        className={inputClass}
                                        type="date"
                                        value={draft.toDate}
                                        onChange={(event) =>
                                            setDraft((old) => ({
                                                ...old,
                                                toDate: event.target.value
                                            }))
                                        }
                                    />
                                </label>
                            </>
                        ) : (
                            <div className="sm:col-span-2 flex items-center text-sm text-slate-500">
                                The inventory report is a current snapshot and does not support date filtering.
                            </div>
                        )}

                    
                            {/* Giữ nguyên các nút View Report, Clear Filters, Reload và Export bên dưới */}
                    </div>
                    {warehouseError && <p role="alert" className="mt-3 text-sm text-red-600">{warehouseError}</p>}
                    {filterError && <p role="alert" className="mt-3 text-sm text-red-600">{filterError}</p>}
                    <div className="mt-4 flex flex-wrap items-center gap-2">
                        <button type="submit" className={`${buttonClass} bg-blue-700 text-white hover:bg-blue-800`}>View Report / Apply Filters</button>
                        <button type="button" onClick={clearFilters} className={`${buttonClass} border border-slate-200 bg-white text-slate-700 hover:bg-slate-50`}>Clear Filters</button>
                        <button type="button" onClick={() => setReload((value) => value + 1)} className={`${buttonClass} text-blue-700 hover:bg-blue-50`}>Reload</button>
                        <button type="button" onClick={exportExcel} disabled={exporting}
                            className={`${buttonClass} ml-auto bg-emerald-600 text-white hover:bg-emerald-700`}>
                            {exporting ? "Exporting Excel..." : "Export to Excel"}
                        </button>
                    </div>
                    {exportError && <p role="alert" className="mt-3 text-sm text-red-600">{exportError}</p>}
                </form>

                {loading ? <div role="status" className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500">Loading report...</div>
                    : error ? <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
                        : report && <>
                            <section aria-label="Report Metrics" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
                                {METRICS[type].map(([field, label]) => (
                                    <div key={field} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                                        <div className="text-xs font-semibold text-slate-500">{label}</div>
                                        <div className="mt-2 text-2xl font-bold tabular-nums text-slate-900">{number(field === "warehouseProductCount" ? rows.length : report[field])}</div>
                                    </div>
                                ))}
                            </section>

                            {type === "inventory" && <p className="text-sm text-slate-500">Inventory snapshot as of: <strong>{dateTime(report.snapshotAt)}</strong>. Physical, reserved, and available stock quantities are synchronized with the system.</p>}

                            {breakdown.length > 0 && <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                                <h2 className="mb-4 text-sm font-bold text-slate-800">Stock Transfers by Status</h2>
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
                                    <h2 className="font-bold text-slate-900">Details {current.label.toLowerCase()}</h2>
                                    <span className="text-xs text-slate-500">{number(rows.length)} matching records</span>
                                </div>
                                {rows.length === 0 ? <p className="p-10 text-center text-sm text-slate-500">No records match the selected filters.</p> : <>
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-slate-200 text-sm">
                                            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                                <tr>{COLUMNS[type].map(([, label]) => <th key={label} scope="col" className="whitespace-nowrap px-4 py-3">{label}</th>)}</tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-100 text-slate-700">
                                                {pageRows.map((row, index) => <tr key={row.id ?? row.inventoryBalanceId ?? `${page}-${index}`} className="hover:bg-slate-50">
                                                    {COLUMNS[type].map(([key]) => <td key={key} className="whitespace-nowrap px-4 py-3">
                                                        {((type === "imports" && key === "receiptCode")
                                                            || ((type === "exports" || type === "transfers") && key === "transferCode")) && row.id != null
                                                            ? <Link className="font-semibold text-blue-700 hover:underline"
                                                                to={type === "imports" ? `/business/goods-receipts/${row.id}` : `/business/stock-transfers/${row.id}`}>
                                                                {formatCell(key, row[key])}
                                                            </Link>
                                                            : key === "status" ? <Status value={row.status}/> : formatCell(key, row[key])}
                                                    </td>)}
                                                </tr>)}
                                            </tbody>
                                        </table>
                                    </div>
                                    <div className="flex items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-xs text-slate-500">
                                        <span>Page {page + 1} / {totalPages}</span>
                                        <div className="flex gap-2">
                                            <button type="button" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}
                                                className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Previous</button>
                                            <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
                                                className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Next</button>
                                        </div>
                                    </div>
                                </>}
                            </section>
                        </>}
            </div>
        </MainLayout>
    );
}

