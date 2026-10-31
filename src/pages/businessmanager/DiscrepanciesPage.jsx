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
    title: "X\u1eed l\u00fd b\u00e1o c\u00e1o ch\u00eanh l\u1ec7ch h\u00e0ng h\u00f3a",
    desc: "Xem c\u00e1c b\u00e1o c\u00e1o nh\u1eadp h\u00e0ng, \u0111i\u1ec1u chuy\u1ec3n \u0111\u00e3 chuy\u1ec3n l\u00ean Business Manager v\u00e0 quy\u1ebft \u0111\u1ecbnh ph\u01b0\u01a1ng \u00e1n x\u1eed l\u00fd.",
    reports: "Danh s\u00e1ch b\u00e1o c\u00e1o",
    refresh: "L\u00e0m m\u1edbi",
    receiving: "Nh\u1eadp h\u00e0ng",
    transfer: "\u0110i\u1ec1u chuy\u1ec3n",
    all: "T\u1ea5t c\u1ea3",
    status: "Tr\u1ea1ng th\u00e1i",
    discrepancy: "Lo\u1ea1i ch\u00eanh l\u1ec7ch",
    source: "Ngu\u1ed3n ph\u00e1t sinh",
    search: "T\u00ecm m\u00e3 b\u00e1o c\u00e1o, ch\u1ee9ng t\u1eeb, kho...",
    pending: "Ch\u1edd Business Manager x\u1eed l\u00fd",
    empty: "Kh\u00f4ng c\u00f3 b\u00e1o c\u00e1o ph\u00f9 h\u1ee3p",
    detail: "Chi ti\u1ebft b\u00e1o c\u00e1o",
    view: "Xem chi ti\u1ebft",
    close: "\u0110\u00f3ng",
    fields: "S\u1ed1 l\u01b0\u1ee3ng v\u00e0 ki\u1ec3m nh\u1eadn",
    action: "Ph\u01b0\u01a1ng \u00e1n x\u1eed l\u00fd",
    note: "K\u1ebft lu\u1eadn / ghi ch\u00fa x\u1eed l\u00fd",
    final: "S\u1ed1 l\u01b0\u1ee3ng cu\u1ed1i c\u00f9ng",
    resolve: "X\u00e1c nh\u1eadn x\u1eed l\u00fd",
    receipt: "Phi\u1ebfu nh\u1eadp h\u00e0ng",
    transferDoc: "Phi\u1ebfu \u0111i\u1ec1u chuy\u1ec3n",
    evidence: "H\u00ecnh \u1ea3nh minh ch\u1ee9ng",
    confirm: "T\u00f4i \u0111\u00e3 ki\u1ec3m tra ch\u1ee9ng t\u1eeb, s\u1ed1 l\u01b0\u1ee3ng v\u00e0 x\u00e1c nh\u1eadn ghi k\u1ebft qu\u1ea3 v\u00e0o MySQL chung.",
    inspect: "Xem minh ch\u1ee9ng",
    completed: "\u0110\u00e3 x\u1eed l\u00fd",
};

const REFERENCE_TYPES = [
    ["", T.all],
    ["GOODS_RECEIPT", T.receiving],
    ["STOCK_TRANSFER", T.transfer],
];
const TYPES = [
    ["", T.all],
    ["SHORTAGE", "H\u00e0ng thi\u1ebfu"],
    ["SURPLUS", "H\u00e0ng th\u1eeba"],
    ["DAMAGED", "H\u00e0ng h\u1ecfng"],
    ["WRONG_PRODUCT", "Sai s\u1ea3n ph\u1ea9m"],
    ["QUANTITY_MISMATCH", "Sai l\u1ec7ch s\u1ed1 l\u01b0\u1ee3ng"],
    ["LOCATION_MISMATCH", "Sai v\u1ecb tr\u00ed"],
    ["OTHER", "Kh\u00e1c"],
];
const STATUSES = [
    ["", T.all],
    ["OPEN", "M\u1edbi ph\u00e1t sinh"],
    ["INVESTIGATING", "\u0110ang \u0111i\u1ec1u tra"],
    ["RESOLVED", "\u0110\u00e3 x\u1eed l\u00fd"],
    ["CANCELLED", "\u0110\u00e3 h\u1ee7y"],
];
// RECOUNT is deliberately excluded: the Backend rejects it as a final decision.
const ACTIONS = [
    ["ADJUST_INVENTORY", "X\u00e1c nh\u1eadn s\u1ed1 l\u01b0\u1ee3ng / \u0111i\u1ec1u ch\u1ec9nh"],
    ["RETURN_TO_SUPPLIER", "Ph\u01b0\u01a1ng \u00e1n tr\u1ea3 nh\u00e0 cung c\u1ea5p"],
    ["TRANSFER_STOCK", "Ph\u01b0\u01a1ng \u00e1n \u0111i\u1ec1u chuy\u1ec3n"],
    ["REJECT", "Kh\u00f4ng ch\u1ea5p nh\u1eadn ph\u01b0\u01a1ng \u00e1n"],
    ["NO_ACTION", "Gi\u1eef nguy\u00ean s\u1ed1 l\u01b0\u1ee3ng"],
];

const SELECT_STYLE = "w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-100";
const NUMBER_STYLE = "w-24 rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-right text-sm focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-100";
const integer = (n) => n === null || n === undefined || n === "" ? "\u2014" : Number(n).toLocaleString("vi-VN");
const time = (value) => {
    if (!value) return "\u2014";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("vi-VN", { hour12: false });
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
    if (status === 403) return "T\u00e0i kho\u1ea3n kh\u00f4ng c\u00f3 quy\u1ec1n Business Manager (403).";
    if (status === 401) return "Phi\u00ean \u0111\u0103ng nh\u1eadp \u0111\u00e3 h\u1ebft h\u1ea1n (401).";
    return error?.response?.data?.message || "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i Backend. Ki\u1ec3m tra API v\u00e0 tr\u1ea1ng th\u00e1i ch\u1ee9ng t\u1eeb.";
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
                if (!Array.isArray(data)) throw new Error("Invalid discrepancy response");
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
            if (!report || report.id == null || !Array.isArray(report.items)) throw new Error("Invalid detail response");
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
            if (!updated || updated.status !== "RESOLVED") throw new Error("Unexpected resolve response");
            setDetail(updated);
            resetForm(updated);
            setSuccess(`B\u00e1o c\u00e1o #${id} \u0111\u00e3 \u0111\u01b0\u1ee3c x\u1eed l\u00fd th\u00e0nh c\u00f4ng. S\u1ed1 l\u01b0\u1ee3ng tr\u00ean ch\u1ee9ng t\u1eeb ngu\u1ed3n \u0111\u00e3 \u0111\u01b0\u1ee3c c\u1eadp nh\u1eadt theo quy\u1ebft \u0111\u1ecbnh.`);
            setRefresh((n) => n + 1);
        } catch (err) {
            setDetailError(apiError(err));
        } finally {
            setSaving(false);
        }
    };

    return (
        <MainLayout title="Business Manager" items={businessManagerMenuItems}>
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
                    {loading ? <div role="status" className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Đang tải danh sách...</div> : !error && shown.length === 0 ? <EmptyState title={T.empty} description="API chỉ trả về báo cáo Warehouse/Store đã chuyển lên BUSINESS_MANAGER. Không cần tạo dữ liệu mẫu trực tiếp trong SQL chung." /> : !error && (
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                            <table className="min-w-full divide-y divide-slate-200 text-sm">
                                <thead className="bg-orange-50/60 text-left text-xs font-semibold uppercase tracking-wide text-slate-600"><tr><th className="px-4 py-3">{T.reports}</th><th className="px-4 py-3">{T.source}</th><th className="px-4 py-3">{T.discrepancy}</th><th className="px-4 py-3">{T.status}</th><th className="px-4 py-3">{T.fields}</th><th className="px-4 py-3">{T.view}</th></tr></thead>
                                <tbody className="divide-y divide-slate-100">{shown.map((r) => <tr key={r.id} className="hover:bg-slate-50">
                                    <td className="min-w-48 px-4 py-3"><div className="font-semibold text-slate-900">{r.reportCode || `#${r.id}`}</div><div className="mt-1 text-xs text-slate-500">ID #{r.id} | {time(r.createdAt)}</div><div className="mt-1 text-xs text-slate-500">{r.warehouseName || r.warehouseCode || "\u2014"}</div></td>
                                    <td className="min-w-40 px-4 py-3"><div>{sourceName(r.referenceType)}</div><div className="mt-1 font-mono text-xs text-slate-500">{r.referenceCode || `ID ${r.referenceId}`}</div></td>
                                    <td className="px-4 py-3">{discrepancyName(r.discrepancyType)}</td>
                                    <td className="px-4 py-3"><Badge status={r.status}/></td>
                                    <td className="px-4 py-3 text-slate-600">{Array.isArray(r.items) ? r.items.length : 0} sản phẩm</td>
                                    <td className="px-4 py-3"><Button type="button" variant="outline" size="sm" disabled={saving} onClick={() => openReport(r.id)}>{T.view}</Button></td>
                                </tr>)}</tbody>
                            </table>
                        </div>
                    )}
                    <p className="text-xs text-slate-500">{T.all}: {shown.length} / {items.length}. Backend chỉ trả về báo cáo thuộc phạm vi Business Manager.</p>
                </Card>

                {detailLoading && <Card><div role="status" className="flex items-center gap-2 text-sm text-slate-500"><Spinner /> Đang tải chi tiết...</div></Card>}
                {detailError && <div role="alert"><Alert type="danger">{detailError}</Alert></div>}
                {detail && (
                    <Card className="space-y-5 border-orange-200">
                        <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h2 className="break-words text-lg font-bold text-slate-900">{T.detail}: {detail.reportCode || `#${detail.id}`}</h2><p className="mt-1 text-sm text-slate-500">{sourceName(detail.referenceType)} - {detail.referenceCode || `#${detail.referenceId}`} - #{detail.id}</p></div><Button type="button" variant="ghost" size="sm" disabled={saving} onClick={closeReport}>{T.close}</Button></div>
                        <div className="grid gap-4 rounded-xl bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                            <Definition title={T.discrepancy} value={discrepancyName(detail.discrepancyType)}/>
                            <div><dt className="text-xs uppercase tracking-wide text-slate-500">{T.status}</dt><dd className="mt-1"><Badge status={detail.status}/></dd></div>
                            <Definition title="Kho / Cửa hàng" value={detail.warehouseName || detail.warehouseCode}/>
                            <Definition title="Chứng từ liên quan" value={detail.referenceCode || detail.referenceId}/>
                            <Definition title="Trạng thái chứng từ" value={detail.sourceStatus}/>
                            <Definition title="Người báo cáo" value={detail.reportedByName || (detail.reportedBy ? `#${detail.reportedBy}` : "")}/>
                            <Definition title="Người kiểm tra trước" value={detail.reviewedByName || (detail.reviewedBy ? `#${detail.reviewedBy}` : "")}/>
                            <Definition title="Thời điểm kiểm tra" value={time(detail.reviewedAt)}/>
                        </div>
                        {detail.description && <Alert type="info"><strong>{T.discrepancy}:</strong> {detail.description}</Alert>}
                        {detail.resolutionNote && <Alert type="warning"><strong>Ghi chú đã ghi nhận:</strong> {detail.resolutionNote}</Alert>}

                        <section className="space-y-3">
                            <h3 className="font-semibold text-slate-900">{detail.referenceType === "GOODS_RECEIPT" ? T.receipt : T.transferDoc}</h3>
                            <div className="grid gap-3 rounded-xl border border-slate-200 p-4 sm:grid-cols-2 xl:grid-cols-4">
                                <Definition title="Mã chứng từ" value={detail.referenceCode}/>
                                <Definition title="ID chứng từ" value={detail.referenceId}/>
                                <Definition title="Trạng thái chứng từ" value={detail.sourceStatus}/>
                                {detail.referenceType === "GOODS_RECEIPT" ? <>
                                    <Definition title="Đơn mua hàng" value={detail.purchaseOrderCode || detail.purchaseOrderId}/>
                                    <Definition title="Người xác nhận kho" value={detail.warehouseConfirmedByName}/>
                                    <Definition title="Thời gian xác nhận kho" value={time(detail.warehouseConfirmedAt)}/>
                                </> : <>
                                    <Definition title="Kho xuất" value={`${detail.fromWarehouseName || detail.fromWarehouseCode || "\u2014"}`}/>
                                    <Definition title="Kho nhận" value={`${detail.toWarehouseName || detail.toWarehouseCode || "\u2014"}`}/>
                                    <Definition title="Người xác nhận kho" value={detail.warehouseConfirmedByName}/>
                                    <Definition title="Phiếu nhận của cửa hàng" value={detail.storeReceiptCode || detail.storeReceiptId}/>
                                    <Definition title="Trạng thái phiếu nhận" value={detail.storeReceiptStatus}/>
                                    <Definition title="Người xác nhận cửa hàng" value={detail.storeConfirmedByName}/>
                                    <Definition title="Thời gian xác nhận cửa hàng" value={time(detail.storeConfirmedAt)}/>
                                </>}
                            </div>
                            <div className="text-sm text-slate-600">{T.evidence}: <Evidence value={detail.evidenceImageUrl}/></div>
                            <p className="text-xs text-slate-500">Chứng từ nguồn chỉ hiển thị dữ liệu từ API Manager, không mở trang dành riêng cho role khác.</p>
                        </section>

                        <section className="space-y-3">
                            <div><h3 className="font-semibold text-slate-900">{T.fields}</h3><p className="mt-1 text-sm text-slate-500">{T.final} {detail.referenceType === "GOODS_RECEIPT" ? "= acceptedQuantity" : "= receivedQuantity"}. Backend yêu cầu nhập đủ từng sản phẩm.</p></div>
                            {reportItems.length === 0 ? <Alert type="warning">Báo cáo chưa có sản phẩm. Backend không cho phép xử lý khi danh sách rỗng.</Alert> : (
                                <div className="overflow-x-auto rounded-xl border border-slate-200"><table className="min-w-full divide-y divide-slate-200 text-sm">
                                    <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-600"><tr><th className="px-3 py-3">Sản phẩm</th><th className="px-3 py-3">Dự kiến</th><th className="px-3 py-3">Thực tế</th><th className="px-3 py-3">Chênh lệch</th><th className="px-3 py-3">{detail.referenceType === "GOODS_RECEIPT" ? "Ordered / Accepted" : "Shipped / Received"}</th><th className="px-3 py-3">{T.final}</th><th className="px-3 py-3">Kiểm tra / {T.evidence}</th></tr></thead>
                                    <tbody className="divide-y divide-slate-100">{reportItems.map((row, idx) => {
                                        const key = String(row.productId);
                                        const value = quantities[key] ?? "";
                                        const invalid = value === "" || !/^(0|[1-9][0-9]*)$/.test(value) || !Number.isSafeInteger(Number(value)) || (detail.referenceType === "GOODS_RECEIPT" && Number(value) > (row.actualQuantity ?? 0));
                                        return <tr key={`${key}-${idx}`}>
                                            <td className="min-w-44 px-3 py-3"><div className="font-semibold text-slate-800">{row.productName || row.productCode || `#${key}`}</div><div className="text-xs text-slate-500">{row.productCode} | ID {key}</div>{row.notes && <p className="mt-1 text-xs text-slate-500">{row.notes}</p>}</td>
                                            <td className="px-3 py-3">{integer(row.expectedQuantity)}</td>
                                            <td className="px-3 py-3">{integer(row.actualQuantity)}</td>
                                            <td className={`px-3 py-3 font-semibold ${Number(row.differenceQuantity) !== 0 ? "text-orange-700" : "text-slate-700"}`}>{integer(row.differenceQuantity)}</td>
                                            <td className="min-w-36 px-3 py-3">{detail.referenceType === "GOODS_RECEIPT" ? `${integer(row.orderedQuantity)} / ${integer(row.acceptedQuantity)}` : `${integer(row.shippedQuantity)} / ${integer(row.receivedQuantity)}`}</td>
                                            <td className="px-3 py-3">{canResolve(detail) ? <input type="number" min="0" step="1" inputMode="numeric" aria-label={`${T.final} ${row.productName || key}`} className={`${NUMBER_STYLE} ${invalid ? "border-red-300" : ""}`} value={value} disabled={saving} onChange={(e) => { setQuantities((old) => ({ ...old, [key]: e.target.value })); setConfirmed(false); }}/>: integer(row.finalQuantity)}{canResolve(detail) && invalid && <div className="mt-1 text-xs text-red-600">{Number(value) > (row.actualQuantity ?? 0) && detail.referenceType === "GOODS_RECEIPT" ? "Không vượt số thực nhận" : "Nhập số nguyên không âm"}</div>}</td>
                                            <td className="min-w-28 px-3 py-3"><div className="text-xs text-slate-600">{row.inspectionResult || "\u2014"}</div><Evidence value={row.evidenceImageUrl}/></td>
                                        </tr>;
                                    })}</tbody>
                                </table></div>
                            )}
                        </section>

                        {detail.resolvedAt && <section className="grid gap-3 rounded-xl bg-green-50 p-4 sm:grid-cols-3"><Definition title={T.completed} value={actionName(detail.resolutionAction)}/><Definition title="Người xử lý" value={detail.resolvedByName || detail.resolvedBy}/><Definition title="Thời gian xử lý" value={time(detail.resolvedAt)}/></section>}
                        {canResolve(detail) ? <form onSubmit={submitResolution} className="space-y-4 rounded-2xl border border-orange-200 bg-orange-50/40 p-4">
                            <div><h3 className="font-semibold text-slate-900">{T.resolve}</h3><p className="mt-1 text-sm text-slate-600">Chức năng này CẬP NHẬT số lượng trên chứng từ nguồn và lưu kết quả, không chỉ đóng báo cáo. Chỉ xử lý báo cáo INVESTIGATING đã chuyển cấp.</p></div>
                            {detail.referenceType === "GOODS_RECEIPT" && detail.sourceStatus !== "INSPECTED" && <Alert type="warning">Phiếu nhập phải ở trạng thái INSPECTED để Backend cho phép xử lý.</Alert>}
                            {detail.referenceType === "STOCK_TRANSFER" && (detail.sourceStatus === "COMPLETED" || detail.sourceStatus === "CANCELLED") && <Alert type="warning">Phiếu điều chuyển đã COMPLETED/CANCELLED. Backend sẽ từ chối thay đổi số lượng.</Alert>}
                            {detail.referenceType === "STOCK_TRANSFER" && !["INSPECTED", "CONFIRMED"].includes(detail.storeReceiptStatus) && <Alert type="warning">Store Receipt phải được kiểm hàng (INSPECTED/CONFIRMED) trước khi xử lý.</Alert>}
                            <div className="grid gap-4 md:grid-cols-2">
                                <label className="block space-y-1"><span className="text-sm font-semibold text-slate-700">{T.action} *</span><select value={action} className={SELECT_STYLE} disabled={saving} required onChange={(e) => { setAction(e.target.value); setConfirmed(false); }}><option value="">Chọn phương án</option>{ACTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                                <label className="block space-y-1"><span className="text-sm font-semibold text-slate-700">{T.note} *</span><textarea className={`${SELECT_STYLE} min-h-24`} rows={3} value={note} required disabled={saving} onChange={(e) => { setNote(e.target.value); setConfirmed(false); }} placeholder="Ghi rõ nguyên nhân, phương án và cơ sở xác nhận số lượng" /></label>
                            </div>
                            <Alert type="warning">Quan trọng: Ngay cả khi chọn NO_ACTION hoặc REJECT, Backend vẫn ghi số lượng cuối cùng vào chứng từ. Hãy kiểm tra tất cả sản phẩm; số nhập không được vượt số thực tế.</Alert>
                            <label className="flex items-start gap-2 text-sm text-slate-700"><input type="checkbox" className="mt-1 h-4 w-4 accent-orange-600" checked={confirmed} disabled={saving} onChange={(e) => setConfirmed(e.target.checked)}/><span>{T.confirm}</span></label>
                            <label className="block max-w-xs space-y-1"><span className="text-sm text-slate-700">Nhập ID báo cáo <strong>#{detail.id}</strong> để xác nhận *</span><input type="text" inputMode="numeric" className={SELECT_STYLE} value={typedId} disabled={saving} onChange={(e) => setTypedId(e.target.value)} placeholder={`Nhập ${detail.id}`} /></label>
                            <Button type="submit" variant="danger" disabled={!canSubmit}>{saving ? "Đang xử lý..." : T.resolve}</Button>
                        </form> : <Alert type="info">{detail.status === "RESOLVED" ? "Báo cáo đã xử lý; chỉ có thể xem lại kết quả và lịch sử." : "Báo cáo không ở trạng thái INVESTIGATING hoặc chưa được chuyển cấp. Chỉ xem thông tin."}</Alert>}
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
