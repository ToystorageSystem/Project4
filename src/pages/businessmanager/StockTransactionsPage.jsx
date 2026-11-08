import { useEffect, useRef, useState } from "react";
import {
    getStockTransactionDetail,
    listStockTransactions,
} from "../../api/businessmanager/stockTransactions";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";

const PAGE_SIZE = 10;
const EMPTY_FILTERS = { keyword: "", status: "", warehouseId: "", fromDate: "", toDate: "" };
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0 };
const STATUS_LABELS = {
    DRAFT: "Draft", CREATED: "Created", ORDERED: "Ordered", INSPECTING: "Inspecting",
    INSPECTED: "Inspected", COMPLETED: "Completed", CANCELLED: "Cancelled",
    PENDING_SOURCE_CONFIRMATION: "Awaiting Source Warehouse Confirmation", PICKING: "Picking",
    PICKED: "Picked", PACKING: "Packing", PACKED: "Packed",
    SHIPPED: "Shipped", IN_TRANSIT: "In Transit", RECEIVED: "Received",
    CONFIRMED: "Confirmed", REJECTED: "Reject",
};
const ITEM_FIELDS = {
    imports: [
        ["expectedQuantity", "Expected"], ["actualQuantity", "Actual Received"],
        ["acceptedQuantity", "Accepted"], ["damagedQuantity", "Damaged"],
        ["shortageQuantity", "Shortage"], ["surplusQuantity", "Overage"],
    ],
    exports: [
        ["requestedQuantity", "Request"], ["approvedQuantity", "Approve"],
        ["pickedQuantity", "Picked"], ["packedQuantity", "Closed"],
        ["shippedQuantity", "Sent"], ["receivedQuantity", "Received"],
        ["shortageQuantity", "Shortage"], ["surplusQuantity", "Overage"],
    ],
};
const inputClass = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
const labelClass = "mb-1.5 block text-xs font-semibold text-slate-600";
const buttonClass = "rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50";

function show(value) {
    return value === null || value === undefined || value === "" ? "—" : String(value);
}

function dateTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value).replace("T", " ").slice(0, 19)
        : date.toLocaleString("en-GB");
}

function requestError(error) {
    if (error?.response?.status === 401) return "Your session has expired. Please sign in again.";
    if (error?.response?.status === 403) return "You do not have permission to view warehouse activities.";
    if (error?.response?.status === 404) return "Document not found (404).";
    const message = error?.response?.data?.message || error?.response?.data?.error;
    return typeof message === "string" && message.trim()
        ? message : "Unable to load data. Check your connection and try again.";
}

function StatusChip({ value }) {
    const done = value === "COMPLETED" || value === "CONFIRMED";
    const cancelled = value === "CANCELLED" || value === "REJECTED";
    const color = done ? "bg-emerald-50 text-emerald-700 border-emerald-200"
        : cancelled ? "bg-red-50 text-red-700 border-red-200"
        : "bg-amber-50 text-amber-800 border-amber-200";
    return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${color}`}>
        {STATUS_LABELS[value] || show(value)}
    </span>;
}

function Definition({ label, value }) {
    return <div className="space-y-1">
        <dt className="text-xs text-slate-500">{label}</dt>
        <dd className="break-words text-sm font-medium text-slate-800">{show(value)}</dd>
    </div>;
}

export default function StockTransactionsPage() {
    const [type, setType] = useState("imports");
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [applied, setApplied] = useState(EMPTY_FILTERS);
    const [page, setPage] = useState(0);
    const [result, setResult] = useState(EMPTY_PAGE);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const [knownStatuses, setKnownStatuses] = useState({ imports: [], exports: [] });
    const [detail, setDetail] = useState(null);
    const [detailKey, setDetailKey] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const detailController = useRef(null);
    const detailSequence = useRef(0);

    useEffect(() => {
        const controller = new AbortController();
        let active = true;
        setLoading(true);
        setError("");
        listStockTransactions(type, { ...applied, page, size: PAGE_SIZE }, controller.signal)
            .then((data) => {
                if (!active) return;
                if (!data || !Array.isArray(data.content)) {
                    throw new Error("Invalid pagination data.");
                }
                setResult(data);
                const statuses = data.content.map((item) => item.status).filter(Boolean);
                setKnownStatuses((previous) => ({
                    ...previous,
                    [type]: [...new Set([...previous[type], ...statuses])].sort(),
                }));
            })
            .catch((err) => {
                if (!active || err?.code === "ERR_CANCELED" || err?.name === "CanceledError") return;
                setError(err?.message === "Invalid pagination data."
                    ? err.message : requestError(err));
                setResult(EMPTY_PAGE);
            })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; controller.abort(); };
    }, [type, applied, page, reload]);

    useEffect(() => () => detailController.current?.abort(), []);

    function closeDetail() {
        ++detailSequence.current;
        detailController.current?.abort();
        setDetailKey(null);
        setDetail(null);
        setDetailError("");
        setDetailLoading(false);
    }

    function switchType(nextType) {
        if (nextType === type) return;
        closeDetail();
        setType(nextType);
        setFilters({ ...EMPTY_FILTERS });
        setApplied({ ...EMPTY_FILTERS });
        setPage(0);
        setResult(EMPTY_PAGE);
        setError("");
    }

    function updateFilter(key, value) {
        setFilters((previous) => ({ ...previous, [key]: value }));
    }

    function search(event) {
        event.preventDefault();
        if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
            setError("Start date cannot be after end date.");
            return;
        }
        if (filters.warehouseId && (!/^\d+$/.test(filters.warehouseId)
            || !Number.isSafeInteger(Number(filters.warehouseId))
            || Number(filters.warehouseId) < 1)) {
            setError("Warehouse ID must be a positive integer.");
            return;
        }
        closeDetail();
        setError("");
        setPage(0);
        setApplied({ ...filters, keyword: filters.keyword.trim() });
        setReload((value) => value + 1);
    }

    function clearFilters() {
        closeDetail();
        setFilters({ ...EMPTY_FILTERS });
        setApplied({ ...EMPTY_FILTERS });
        setPage(0);
        setError("");
        setReload((value) => value + 1);
    }

    async function openDetail(id) {
        detailController.current?.abort();
        const controller = new AbortController();
        detailController.current = controller;
        const sequence = ++detailSequence.current;
        setDetailKey({ type, id });
        setDetail(null);
        setDetailError("");
        setDetailLoading(true);
        try {
            const data = await getStockTransactionDetail(type, id, controller.signal);
            if (sequence === detailSequence.current) setDetail(data);
        } catch (err) {
            if (sequence !== detailSequence.current || err?.code === "ERR_CANCELED") return;
            setDetailError(requestError(err));
        } finally {
            if (sequence === detailSequence.current) setDetailLoading(false);
        }
    }

    const rows = Array.isArray(result.content) ? result.content : [];
    const totalPages = Math.max(Number(result.totalPages) || 0, 0);
    const totalElements = Math.max(Number(result.totalElements) || 0, 0);
    const activeStatuses = [...new Set([...knownStatuses[type], filters.status].filter(Boolean))].sort();
    const isImport = type === "imports";

    return (
        <MainLayout title="Business Operations" items={businessManagerMenuItems}>
            <div className="mx-auto w-full max-w-7xl space-y-5 px-4 py-6 sm:px-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold text-slate-900">Track Outbound / Inbound Receipts</h1>
                    <p className="text-sm text-slate-500">Track receipts, shipments, and stock transfers across the system. This page is read-only.</p>
                </div>

                <div className="flex flex-wrap gap-2" role="tablist" aria-label="Document Type">
                    {[["imports", "Goods Receipt"], ["exports", "Outbound / Stock Transfer"]].map(([key, title]) => (
                        <button key={key} type="button" role="tab" aria-selected={type === key}
                            onClick={() => switchType(key)}
                            className={`${buttonClass} ${type === key ? "bg-blue-700 text-white" : "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50"}`}>
                            {title}
                        </button>
                    ))}
                </div>

                <form onSubmit={search} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                        <label><span className={labelClass}>Document No.</span>
                            <input className={inputClass} value={filters.keyword} onChange={(event) => updateFilter("keyword", event.target.value)} placeholder={isImport ? "GR-..." : "ST-..."} />
                        </label>
                        <label><span className={labelClass}>Status</span>
                            <select className={inputClass} value={filters.status} onChange={(event) => updateFilter("status", event.target.value)}>
                                <option value="">All Statuses</option>
                                {activeStatuses.map((value) => <option key={value} value={value}>{STATUS_LABELS[value] || value} ({value})</option>)}
                            </select>
                        </label>
                        <label><span className={labelClass}>Warehouse ID {isImport ? "Inbound" : "Outbound"}</span>
                            <input type="number" min="1" step="1" className={inputClass} value={filters.warehouseId} onChange={(event) => updateFilter("warehouseId", event.target.value)} placeholder="All Warehouses" />
                        </label>
                        <label><span className={labelClass}>Created From</span>
                            <input type="date" className={inputClass} value={filters.fromDate} onChange={(event) => updateFilter("fromDate", event.target.value)} />
                        </label>
                        <label><span className={labelClass}>Created To</span>
                            <input type="date" className={inputClass} value={filters.toDate} onChange={(event) => updateFilter("toDate", event.target.value)} />
                        </label>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <button type="submit" className={`${buttonClass} bg-blue-700 text-white hover:bg-blue-800`}>Search / Filters</button>
                        <button type="button" className={`${buttonClass} border border-slate-200 bg-white text-slate-700 hover:bg-slate-50`} onClick={clearFilters}>Clear Filters</button>
                        <button type="button" disabled={loading} className={`${buttonClass} text-blue-700 hover:bg-blue-50`} onClick={() => setReload((value) => value + 1)}>Reload</button>
                    </div>
                </form>

                {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

                <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-4 py-4">
                        <h2 className="font-semibold text-slate-900">List {isImport ? "goods receipts" : "outbound transfers"}</h2>
                        <span className="text-xs text-slate-500">{loading ? "Loading..." : `${totalElements.toLocaleString("en-GB")} records`}</span>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-100 text-sm">
                            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                <tr>
                                    <th className="whitespace-nowrap px-4 py-3">Document No.</th>
                                    <th className="whitespace-nowrap px-4 py-3">Status</th>
                                    <th className="whitespace-nowrap px-4 py-3">{isImport ? "Receiving Warehouse" : "Source Warehouse"}</th>
                                    {!isImport && <th className="whitespace-nowrap px-4 py-3">Destination Warehouse / Store</th>}
                                    <th className="whitespace-nowrap px-4 py-3">{isImport ? "Received By" : "Created By"}</th>
                                    <th className="whitespace-nowrap px-4 py-3">Created At</th>
                                    <th className="px-4 py-3 text-right">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {!loading && rows.map((row) => (
                                    <tr key={`${type}-${row.id}`} className="hover:bg-slate-50">
                                        <td className="whitespace-nowrap px-4 py-3 font-semibold text-slate-800">{show(row.transactionCode)}</td>
                                        <td className="whitespace-nowrap px-4 py-3"><StatusChip value={row.status} /></td>
                                        <td className="px-4 py-3 text-slate-700">{show(row.warehouseName || (row.warehouseId ? `Warehouse #${row.warehouseId}` : null))}</td>
                                        {!isImport && <td className="px-4 py-3 text-slate-700">{show(row.destinationWarehouseName || (row.destinationWarehouseId ? `Warehouse #${row.destinationWarehouseId}` : null))}</td>}
                                        <td className="px-4 py-3 text-slate-700">{show(row.performedByName || (row.performedById ? `#${row.performedById}` : null))}</td>
                                        <td className="whitespace-nowrap px-4 py-3 text-slate-600">{dateTime(row.createdAt)}</td>
                                        <td className="px-4 py-3 text-right"><button type="button" onClick={() => openDetail(row.id)} className="whitespace-nowrap rounded-lg px-3 py-1.5 font-semibold text-blue-700 hover:bg-blue-50">Details</button></td>
                                    </tr>
                                ))}
                                {loading && <tr><td colSpan={isImport ? 6 : 7} className="px-4 py-12 text-center text-slate-500">Loading list...</td></tr>}
                                {!loading && rows.length === 0 && <tr><td colSpan={isImport ? 6 : 7} className="px-4 py-12 text-center text-slate-500">No matching documents found.</td></tr>}
                            </tbody>
                        </table>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-4 py-3">
                        <span className="text-sm text-slate-500">Trang {totalPages ? page + 1 : 0} / {totalPages}</span>
                        <div className="flex gap-2">
                            <button type="button" disabled={loading || page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Previous</button>
                            <button type="button" disabled={loading || page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} className={`${buttonClass} border border-slate-200 bg-white text-slate-700`}>Sau</button>
                        </div>
                    </div>
                </section>

                {detailKey && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-3 sm:p-6" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeDetail(); }}>
                    <section role="dialog" aria-modal="true" aria-label="Document Details" className="max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
                        <div className="sticky top-0 z-10 flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-5 py-4">
                            <div><h2 className="text-lg font-bold text-slate-900">Details {detailKey.type === "imports" ? "goods receipts" : "outbound transfers"}</h2><p className="text-xs text-slate-500">ID #{detailKey.id}</p></div>
                            <button type="button" onClick={closeDetail} aria-label="Close Details" className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">Close ✕</button>
                        </div>
                        <div className="space-y-5 p-5">
                            {detailLoading && <p className="py-10 text-center text-slate-500">Loading details...</p>}
                            {detailError && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{detailError}</div>}
                            {detail && <>
                                <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-3">
                                    <Definition label="Document No." value={detail.transactionCode} />
                                    <div><dt className="mb-1 text-xs text-slate-500">Status</dt><dd><StatusChip value={detail.status} /></dd></div>
                                    <Definition label="Created At" value={dateTime(detail.createdAt)} />
                                    <Definition label={detailKey.type === "imports" ? "Receiving Warehouse" : "Source Warehouse"} value={detail.warehouseName || (detail.warehouseId ? `Warehouse #${detail.warehouseId}` : null)} />
                                    {detailKey.type === "exports" && <Definition label="Destination Warehouse / Store" value={detail.destinationWarehouseName || (detail.destinationWarehouseId ? `Warehouse #${detail.destinationWarehouseId}` : null)} />}
                                    <Definition label={detailKey.type === "imports" ? "Received By" : "Document Created By"} value={detail.performedByName || (detail.performedById ? `#${detail.performedById}` : null)} />
                                </div>
                                <section className="space-y-3">
                                    <h3 className="font-bold text-slate-900">Product List</h3>
                                    <div className="overflow-x-auto rounded-xl border border-slate-200">
                                        <table className="min-w-full divide-y divide-slate-100 text-sm">
                                            <thead className="bg-slate-50 text-left text-xs font-semibold text-slate-600"><tr>
                                                <th className="px-3 py-3">Products</th>
                                                {ITEM_FIELDS[detailKey.type].map(([key, label]) => <th key={key} className="whitespace-nowrap px-3 py-3 text-right">{label}</th>)}
                                            </tr></thead>
                                            <tbody className="divide-y divide-slate-100">
                                                {(Array.isArray(detail.items) ? detail.items : []).map((item, index) => <tr key={`${item.productId || index}-${index}`}>
                                                    <td className="min-w-44 px-3 py-3"><span className="block font-semibold text-slate-900">{show(item.productName)}</span><span className="text-xs text-slate-500">{show(item.productCode)} · ID {show(item.productId)}</span></td>
                                                    {ITEM_FIELDS[detailKey.type].map(([key]) => <td key={key} className="px-3 py-3 text-right text-slate-700">{show(item[key])}</td>)}
                                                </tr>)}
                                                {(!Array.isArray(detail.items) || detail.items.length === 0) && <tr><td colSpan={ITEM_FIELDS[detailKey.type].length + 1} className="px-3 py-8 text-center text-slate-500">This document contains no product lines.</td></tr>}
                                            </tbody>
                                        </table>
                                    </div>
                                </section>
                            </>}
                        </div>
                    </section>
                </div>}
            </div>
        </MainLayout>
    );
}
