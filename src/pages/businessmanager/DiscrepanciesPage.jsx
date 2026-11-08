import { useEffect, useMemo, useRef, useState } from "react";
import {
    getBusinessManagerDiscrepancy,
    listBusinessManagerDiscrepancies,
    resolveBusinessManagerDiscrepancy,
} from "../../api/businessmanager/discrepancies";
import { businessManagerMenuItems } from "../../components/icons/businessMenuItems";
import MainLayout from "../../components/layout/MainLayout";
import PageHeader from "../../components/layout/PageHeader";
import Card from "../../components/ui/Card";
import Alert from "../../components/ui/Alert";
import Button from "../../components/ui/Button";
import EmptyState from "../../components/ui/EmptyState";
import Spinner from "../../components/ui/Spinner";

const T = {
    title: "Resolve Discrepancy Reports",
    desc: "Review discrepancies escalated by warehouses and stores, then select an appropriate resolution.",
    reports: "Discrepancy Reports",
    refresh: "Refresh",
    receiving: "Goods Receipts",
    transfer: "Stock Transfers",
    all: "All",
    status: "Status",
    discrepancy: "Discrepancy Type",
    source: "Source",
    search: "Search report, document, or warehouse code...",
    pending: "Awaiting Management Review",
    empty: "No matching discrepancy reports",
    detail: "Discrepancy Report Details",
    view: "View Details",
    close: "Close",
    fields: "Quantities & Inspection",
    action: "Resolution Action",
    note: "Resolution Notes / Decision",
    final: "Final Accepted Quantity",
    resolve: "Confirm Resolution",
    receipt: "Goods Receipt",
    transferDoc: "Stock Transfer",
    evidence: "Supporting Images",
    confirm: "I have verified the documents and quantities and agree to save the resolution.",
    inspect: "View Evidence",
    completed: "Resolved",
};

const REFERENCE_TYPES = [
    ["", T.all],
    ["GOODS_RECEIPT", T.receiving],
    ["STOCK_TRANSFER", T.transfer],
];
const TYPES = [
    ["", T.all],
    ["SHORTAGE", "Shortage"],
    ["SURPLUS", "Overage"],
    ["DAMAGED", "Damaged Goods"],
    ["WRONG_PRODUCT", "Wrong Product"],
    ["QUANTITY_MISMATCH", "Quantity Mismatch"],
    ["LOCATION_MISMATCH", "Location Mismatch"],
    ["OTHER", "Other"],
];
const STATUSES = [
    ["", T.all],
    ["OPEN", "Open"],
    ["INVESTIGATING", "Under Investigation"],
    ["RESOLVED", "Resolved"],
    ["CANCELLED", "Cancelled"],
];
// RECOUNT is deliberately excluded: the Backend rejects it as a final decision.
const ACTIONS = [
    ["ADJUST_INVENTORY", "Confirm Final Quantity / Adjust Source Document"],
    ["RETURN_TO_SUPPLIER", "Return to Supplier (Resolution Option)"],
    ["TRANSFER_STOCK", "Stock Transfer (Resolution Option)"],
    ["REJECT", "Reject Proposed Resolution"],
    ["NO_ACTION", "No Change to Quantities"],
];

const SELECT_STYLE = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-100";
const NUMBER_STYLE = "w-24 rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-right text-sm focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-100";
const integer = (n) => n === null || n === undefined || n === "" ? "\u2014" : Number(n).toLocaleString("en-GB");
const time = (value) => {
    if (!value) return "\u2014";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("en-GB", { hour12: false });
};
const labelFrom = (options, value) => options.find(([id]) => id === value)?.[1] || value || "\u2014";
const sourceName = (value) => labelFrom(REFERENCE_TYPES, value);
const discrepancyName = (value) => labelFrom(TYPES, value);
const statusName = (value) => labelFrom(STATUSES, value);
const actionName = (value) => labelFrom(ACTIONS, value);
const safeEvidenceUrl = (value) => typeof value === "string" && /^https?:\/\//i.test(value.trim()) ? value.trim() : null;
const canResolve = (report) => report?.responsibleParty === "BUSINESS_MANAGER" && report?.status === "INVESTIGATING";
const sourceReady = (report) => {
    if (!canResolve(report)) return false;
    if (report.referenceType === "GOODS_RECEIPT") return report.sourceStatus === "INSPECTED";
    if (report.referenceType === "STOCK_TRANSFER") return report.sourceStatus !== "COMPLETED"
        && report.sourceStatus !== "CANCELLED"
        && ["INSPECTED", "CONFIRMED"].includes(report.storeReceiptStatus);
    return false;
};

function apiError(error) {
    const status = error?.response?.status;
    if (status === 403) return "You do not have permission to resolve discrepancies.";
    if (status === 401) return "Your session has expired. Please sign in again.";
    return error?.response?.data?.message || "Unable to connect to the system. Check your connection and try again.";
}

function Badge({ status }) {
    const tone = {
        OPEN: "bg-slate-100 text-slate-700",
        INVESTIGATING: "bg-amber-100 text-amber-800",
        RESOLVED: "bg-green-100 text-green-800",
        CANCELLED: "bg-red-100 text-red-700",
    }[status] || "bg-slate-100 text-slate-700";
    return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${tone}`}>{statusName(status)}</span>;
}

function Definition({ title, value }) {
    return <div className="min-w-0"><dt className="text-xs uppercase tracking-wide text-slate-500">{title}</dt><dd className="mt-1 break-words text-sm font-medium text-slate-900">{value === null || value === undefined || value === "" ? "\u2014" : value}</dd></div>;
}

function Evidence({ value }) {
    const url = safeEvidenceUrl(value);
    return url ? <a href={url} target="_blank" rel="noopener noreferrer" className="text-sm font-semibold text-orange-600 underline underline-offset-2">{T.inspect}</a> : value ? <span className="break-all text-xs text-slate-500">{String(value)}</span> : <span className="text-slate-400">—</span>;
}

function useReportList(filters, refresh) {
    const [state, setState] = useState({ items: [], loading: true, error: "" });
    useEffect(() => {
        const controller = new AbortController();
        setState((old) => ({ ...old, loading: true, error: "" }));
        listBusinessManagerDiscrepancies(filters, controller.signal)
            .then((data) => {
                if (!Array.isArray(data)) throw new Error("Invalid report data");
                if (!controller.signal.aborted) setState({ items: data, loading: false, error: "" });
            })
            .catch((error) => {
                if (!controller.signal.aborted) setState({ items: [], loading: false, error: apiError(error) });
            });
        return () => controller.abort();
    }, [filters.referenceType, filters.discrepancyType, filters.status, refresh]);
    return state;
}

export default function DiscrepanciesPage() {
    const [filters, setFilters] = useState({ referenceType: "", discrepancyType: "", status: "" });
    const [search, setSearch] = useState("");
    const [refresh, setRefresh] = useState(0);
    const { items, loading, error } = useReportList(filters, refresh);
    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const [success, setSuccess] = useState("");
    const [action, setAction] = useState("");
    const [note, setNote] = useState("");
    const [quantities, setQuantities] = useState({});
    const [confirmed, setConfirmed] = useState(false);
    const [typedId, setTypedId] = useState("");
    const [saving, setSaving] = useState(false);
    const sequence = useRef(0);

    const shown = useMemo(() => {
        const q = search.trim().toLocaleLowerCase("vi-VN");
        if (!q) return items;
        return items.filter((item) => [item.reportCode, item.referenceCode, item.description, item.warehouseName, item.warehouseCode, item.id, item.purchaseOrderCode, item.fromWarehouseName, item.toWarehouseName]
            .some((value) => String(value ?? "").toLocaleLowerCase("vi-VN").includes(q)));
    }, [items, search]);

    const resetForm = (report) => {
        setAction("");
        setNote("");
        setConfirmed(false);
        setTypedId("");
        const initial = {};
        for (const item of report?.items || []) {
            const quantity = item.finalQuantity;
            initial[String(item.productId)] = Number.isInteger(quantity) && quantity >= 0 ? String(quantity) : "";
        }
        setQuantities(initial);
    };

    const openReport = async (id) => {
        const seq = ++sequence.current;
        setDetailLoading(true);
        setDetailError("");
        setSuccess("");
        setDetail(null);
        try {
            const report = await getBusinessManagerDiscrepancy(id);
            if (sequence.current !== seq) return;
            if (!report || report.id == null || !Array.isArray(report.items)) throw new Error("Invalid discrepancy details");
            setDetail(report);
            resetForm(report);
        } catch (err) {
            if (sequence.current === seq) setDetailError(apiError(err));
        } finally {
            if (sequence.current === seq) setDetailLoading(false);
        }
    };

    const closeReport = () => {
        ++sequence.current;
        setDetail(null);
        setDetailLoading(false);
        setDetailError("");
        setConfirmed(false);
    };

    const reportItems = detail?.items || [];
    const quantityRowsValid = reportItems.length > 0 && reportItems.every((item) => {
        const value = quantities[String(item.productId)];
        if (typeof value !== "string" || !/^(0|[1-9][0-9]*)$/.test(value)) return false;
        const num = Number(value);
        if (!Number.isSafeInteger(num)) return false;
        if (detail.referenceType === "GOODS_RECEIPT" && num > (item.actualQuantity ?? 0)) return false;
        return true;
    }) && new Set(reportItems.map((i) => i.productId)).size === reportItems.length;
    const canSubmit = sourceReady(detail) && !!action && note.trim().length > 0 && quantityRowsValid && confirmed && typedId.trim() === String(detail?.id) && !saving;

    const submitResolution = async (event) => {
        event.preventDefault();
        if (!canSubmit) return;
        const id = detail.id;
        const payload = {
            resolutionAction: action,
            resolutionNote: note.trim(),
            items: reportItems.map((item) => ({ productId: item.productId, finalQuantity: Number(quantities[String(item.productId)]) })),
        };
        setSaving(true);
        setDetailError("");
        setSuccess("");
        try {
            const updated = await resolveBusinessManagerDiscrepancy(id, payload);
            if (!updated || updated.status !== "RESOLVED") throw new Error("No discrepancy resolution response was returned");
            setDetail(updated);
            resetForm(updated);
            setSuccess(`Report #${id} has been resolved successfully. Source document quantities were updated according to the decision.`);
            setRefresh((n) => n + 1);
        } catch (err) {
            setDetailError(apiError(err));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Operations" items={businessManagerMenuItems}>
            <div className="space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0 flex-1"><PageHeader title={T.title} description={T.desc} /></div>
                    <Button type="button" variant="outline" disabled={saving} onClick={() => { setRefresh((n) => n + 1); setSuccess(""); }}> {T.refresh} </Button>
                </div>

                {success && <div role="status"><Alert type="success">{success}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}

                <Card className="space-y-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                        <div><h2 className="font-semibold text-slate-900">{T.reports}</h2><p className="mt-1 text-sm text-slate-500">{T.pending}: {items.filter((item) => canResolve(item)).length} | {T.all}: {items.length}</p></div>
                        <span className="rounded-full bg-orange-50 px-3 py-1.5 text-xs font-medium text-orange-700">{T.receiving} / {T.transfer}</span>
                    </div>
                    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                        <label className="block space-y-1"><span className="text-xs font-medium text-slate-600">{T.source}</span><select className={SELECT_STYLE} value={filters.referenceType} onChange={(e) => setFilters((old) => ({ ...old, referenceType: e.target.value }))}>{REFERENCE_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                        <label className="block space-y-1"><span className="text-xs font-medium text-slate-600">{T.discrepancy}</span><select className={SELECT_STYLE} value={filters.discrepancyType} onChange={(e) => setFilters((old) => ({ ...old, discrepancyType: e.target.value }))}>{TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                        <label className="block space-y-1"><span className="text-xs font-medium text-slate-600">{T.status}</span><select className={SELECT_STYLE} value={filters.status} onChange={(e) => setFilters((old) => ({ ...old, status: e.target.value }))}>{STATUSES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                        <label className="block space-y-1"><span className="text-xs font-medium text-slate-600">{T.search}</span><input className={SELECT_STYLE} type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="DRC-..., GR-..., ST-..." /></label>
                    </div>
                    {loading ? <div role="status" className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Loading list...</div> : !error && shown.length === 0 ? <EmptyState title={T.empty} description="This list contains discrepancies escalated by warehouses and stores for Business Operations review." /> : !error && (
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                            <table className="min-w-full divide-y divide-slate-200 text-sm">
                                <thead className="bg-orange-50/60 text-left text-xs font-semibold uppercase tracking-wide text-slate-600"><tr><th className="px-4 py-3">{T.reports}</th><th className="px-4 py-3">{T.source}</th><th className="px-4 py-3">{T.discrepancy}</th><th className="px-4 py-3">{T.status}</th><th className="px-4 py-3">{T.fields}</th><th className="px-4 py-3">{T.view}</th></tr></thead>
                                <tbody className="divide-y divide-slate-100">{shown.map((r) => <tr key={r.id} className="hover:bg-slate-50">
                                    <td className="min-w-48 px-4 py-3"><div className="font-semibold text-slate-900">{r.reportCode || `#${r.id}`}</div><div className="mt-1 text-xs text-slate-500">No. {r.id} | {time(r.createdAt)}</div><div className="mt-1 text-xs text-slate-500">{r.warehouseName || r.warehouseCode || "\u2014"}</div></td>
                                    <td className="min-w-40 px-4 py-3"><div>{sourceName(r.referenceType)}</div><div className="mt-1 font-mono text-xs text-slate-500">{r.referenceCode || `Code ${r.referenceId}`}</div></td>
                                    <td className="px-4 py-3">{discrepancyName(r.discrepancyType)}</td>
                                    <td className="px-4 py-3"><Badge status={r.status}/></td>
                                    <td className="px-4 py-3 text-slate-600">{Array.isArray(r.items) ? r.items.length : 0} products</td>
                                    <td className="px-4 py-3"><Button type="button" variant="outline" size="sm" disabled={saving} onClick={() => openReport(r.id)}>{T.view}</Button></td>
                                </tr>)}</tbody>
                            </table>
                        </div>
                    )}
                    <p className="text-xs text-slate-500">{T.all}: {shown.length} / {items.length}. Only discrepancies within your access rights are shown.</p>
                </Card>

                {detailLoading && <Card><div role="status" className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Loading details...</div></Card>}
                {detailError && <div role="alert"><Alert type="danger">{detailError}</Alert></div>}
                {detail && (
                    <Card className="space-y-5 border-orange-200">
                        <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h2 className="break-words text-lg font-bold text-slate-900">{T.detail}: {detail.reportCode || `#${detail.id}`}</h2><p className="mt-1 text-sm text-slate-500">{sourceName(detail.referenceType)} - {detail.referenceCode || `#${detail.referenceId}`} - #{detail.id}</p></div><Button type="button" variant="ghost" size="sm" disabled={saving} onClick={closeReport}>{T.close}</Button></div>
                        <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                            <Definition title={T.discrepancy} value={discrepancyName(detail.discrepancyType)}/>
                            <div><dt className="text-xs uppercase tracking-wide text-slate-500">{T.status}</dt><dd className="mt-1"><Badge status={detail.status}/></dd></div>
                            <Definition title="Warehouse / Store" value={detail.warehouseName || detail.warehouseCode}/>
                            <Definition title="Related Document" value={detail.referenceCode || detail.referenceId}/>
                            <Definition title="Document Status" value={detail.sourceStatus}/>
                            <Definition title="Reported By" value={detail.reportedByName || (detail.reportedBy ? `#${detail.reportedBy}` : "")}/>
                            <Definition title="Preliminary Reviewer" value={detail.reviewedByName || (detail.reviewedBy ? `#${detail.reviewedBy}` : "")}/>
                            <Definition title="Reviewed At" value={time(detail.reviewedAt)}/>
                        </div>
                        {detail.description && <Alert type="info"><strong>{T.discrepancy}:</strong> {detail.description}</Alert>}
                        {detail.resolutionNote && <Alert type="warning"><strong>Recorded Notes:</strong> {detail.resolutionNote}</Alert>}

                        <section className="space-y-3">
                            <h3 className="font-semibold text-slate-900">{detail.referenceType === "GOODS_RECEIPT" ? T.receipt : T.transferDoc}</h3>
                            <div className="grid gap-3 rounded-xl border border-slate-200 p-4 sm:grid-cols-2 xl:grid-cols-4">
                                <Definition title="Document No." value={detail.referenceCode}/>
                                <Definition title="Document ID" value={detail.referenceId}/>
                                <Definition title="Document Status" value={detail.sourceStatus}/>
                                {detail.referenceType === "GOODS_RECEIPT" ? <>
                                    <Definition title="Purchase Orders" value={detail.purchaseOrderCode || detail.purchaseOrderId}/>
                                    <Definition title="Confirmed by Warehouse" value={detail.warehouseConfirmedByName}/>
                                    <Definition title="Warehouse Confirmation Time" value={time(detail.warehouseConfirmedAt)}/>
                                </> : <>
                                    <Definition title="Source Warehouse" value={`${detail.fromWarehouseName || detail.fromWarehouseCode || "\u2014"}`}/>
                                    <Definition title="Destination Warehouse / Store" value={`${detail.toWarehouseName || detail.toWarehouseCode || "\u2014"}`}/>
                                    <Definition title="Confirmed by Warehouse" value={detail.warehouseConfirmedByName}/>
                                    <Definition title="Store Receipt" value={detail.storeReceiptCode || detail.storeReceiptId}/>
                                    <Definition title="Receipt Status" value={detail.storeReceiptStatus}/>
                                    <Definition title="Confirmed by Store" value={detail.storeConfirmedByName}/>
                                    <Definition title="Store Confirmation Time" value={time(detail.storeConfirmedAt)}/>
                                </>}
                            </div>
                            <div className="text-sm text-slate-600">{T.evidence}: <Evidence value={detail.evidenceImageUrl}/></div>
                            <p className="text-xs text-slate-500">Related document details are shown here for reconciliation.</p>
                        </section>

                        <section className="space-y-3">
                            <div><h3 className="font-semibold text-slate-900">{T.fields}</h3><p className="mt-1 text-sm text-slate-500">{T.final} {detail.referenceType === "GOODS_RECEIPT" ? "= acceptedQuantity" : "= receivedQuantity"}. Review and enter the final quantity for each product.</p></div>
                            {reportItems.length === 0 ? <Alert type="warning">This discrepancy contains no product lines and cannot be resolved.</Alert> : (
                                <div className="overflow-x-auto rounded-xl border border-slate-200"><table className="min-w-full divide-y divide-slate-200 text-sm">
                                    <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-600"><tr><th className="px-3 py-3">Products</th><th className="px-3 py-3">Expected</th><th className="px-3 py-3">Actual</th><th className="px-3 py-3">Variance</th><th className="px-3 py-3">{detail.referenceType === "GOODS_RECEIPT" ? "Ordered / Accepted" : "Shipped / Received"}</th><th className="px-3 py-3">{T.final}</th><th className="px-3 py-3">Review / {T.evidence}</th></tr></thead>
                                    <tbody className="divide-y divide-slate-100">{reportItems.map((row, idx) => {
                                        const key = String(row.productId);
                                        const value = quantities[key] ?? "";
                                        const invalid = value === "" || !/^(0|[1-9][0-9]*)$/.test(value) || !Number.isSafeInteger(Number(value)) || (detail.referenceType === "GOODS_RECEIPT" && Number(value) > (row.actualQuantity ?? 0));
                                        return <tr key={`${key}-${idx}`}>
                                            <td className="min-w-44 px-3 py-3"><div className="font-semibold text-slate-800">{row.productName || row.productCode || `#${key}`}</div><div className="text-xs text-slate-500">{row.productCode} | Code {key}</div>{row.notes && <p className="mt-1 text-xs text-slate-500">{row.notes}</p>}</td>
                                            <td className="px-3 py-3">{integer(row.expectedQuantity)}</td>
                                            <td className="px-3 py-3">{integer(row.actualQuantity)}</td>
                                            <td className={`px-3 py-3 font-semibold ${Number(row.differenceQuantity) !== 0 ? "text-orange-700" : "text-slate-700"}`}>{integer(row.differenceQuantity)}</td>
                                            <td className="min-w-36 px-3 py-3">{detail.referenceType === "GOODS_RECEIPT" ? `${integer(row.orderedQuantity)} / ${integer(row.acceptedQuantity)}` : `${integer(row.shippedQuantity)} / ${integer(row.receivedQuantity)}`}</td>
                                            <td className="px-3 py-3">{canResolve(detail) ? <input type="number" min="0" step="1" inputMode="numeric" aria-label={`${T.final} ${row.productName || key}`} className={`${NUMBER_STYLE} ${invalid ? "border-red-300" : ""}`} value={value} disabled={saving} onChange={(e) => { setQuantities((old) => ({ ...old, [key]: e.target.value })); setConfirmed(false); }}/>: integer(row.finalQuantity)}{canResolve(detail) && invalid && <div className="mt-1 text-xs text-red-600">{Number(value) > (row.actualQuantity ?? 0) && detail.referenceType === "GOODS_RECEIPT" ? "Cannot exceed the actual received quantity" : "Enter a non-negative integer"}</div>}</td>
                                            <td className="min-w-28 px-3 py-3"><div className="text-xs text-slate-600">{row.inspectionResult || "\u2014"}</div><Evidence value={row.evidenceImageUrl}/></td>
                                        </tr>;
                                    })}</tbody>
                                </table></div>
                            )}
                        </section>

                        {detail.resolvedAt && <section className="grid gap-3 rounded-xl bg-green-50 p-4 sm:grid-cols-3"><Definition title={T.completed} value={actionName(detail.resolutionAction)}/><Definition title="Resolved By" value={detail.resolvedByName || detail.resolvedBy}/><Definition title="Resolved At" value={time(detail.resolvedAt)}/></section>}
                        {canResolve(detail) ? <form onSubmit={submitResolution} className="space-y-4 rounded-2xl border border-orange-200 bg-orange-50/40 p-4">
                            <div><h3 className="font-semibold text-slate-900">{T.resolve}</h3><p className="mt-1 text-sm text-slate-600">This action UPDATES the source document quantities and saves the resolution. It does more than close the discrepancy. Only escalated INVESTIGATING reports can be resolved.</p></div>
                            {detail.referenceType === "GOODS_RECEIPT" && detail.sourceStatus !== "INSPECTED" && <Alert type="warning">The warehouse must complete inspection before this discrepancy can be resolved.</Alert>}
                            {detail.referenceType === "STOCK_TRANSFER" && (detail.sourceStatus === "COMPLETED" || detail.sourceStatus === "CANCELLED") && <Alert type="warning">Completed or cancelled stock transfers cannot have their quantities changed.</Alert>}
                            {detail.referenceType === "STOCK_TRANSFER" && !["INSPECTED", "CONFIRMED"].includes(detail.storeReceiptStatus) && <Alert type="warning">The store receipt must be INSPECTED or CONFIRMED before resolution.</Alert>}
                            <div className="grid gap-4 md:grid-cols-2">
                                <label className="block space-y-1"><span className="text-sm font-semibold text-slate-700">{T.action} *</span><select value={action} className={SELECT_STYLE} disabled={saving} required onChange={(e) => { setAction(e.target.value); setConfirmed(false); }}><option value="">Select Resolution Action</option>{ACTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                                <label className="block space-y-1"><span className="text-sm font-semibold text-slate-700">{T.note} *</span><textarea className={`${SELECT_STYLE} min-h-24`} rows={3} value={note} required disabled={saving} onChange={(e) => { setNote(e.target.value); setConfirmed(false); }} placeholder="Describe the cause, resolution, and basis for the final quantities" /></label>
                            </div>
                            <Alert type="warning">Note: Resolving this discrepancy updates the final quantities on the source document. Review every product before confirming.</Alert>
                            <label className="flex items-start gap-2 text-sm text-slate-700"><input type="checkbox" className="mt-1 h-4 w-4 accent-orange-600" checked={confirmed} disabled={saving} onChange={(e) => setConfirmed(e.target.checked)}/><span>{T.confirm}</span></label>
                            <label className="block max-w-xs space-y-1"><span className="text-sm text-slate-700">Enter report number <strong>#{detail.id}</strong> to confirm *</span><input type="text" inputMode="numeric" className={SELECT_STYLE} value={typedId} disabled={saving} onChange={(e) => setTypedId(e.target.value)} placeholder={`Enter ${detail.id}`} /></label>
                            <Button type="submit" variant="danger" disabled={!canSubmit}>{saving ? "Processing..." : T.resolve}</Button>
                        </form> : <Alert type="info">{detail.status === "RESOLVED" ? "This report has already been resolved. Only the details and history can be viewed." : "This report is not INVESTIGATING or has not been escalated. View-only mode."}</Alert>}
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
