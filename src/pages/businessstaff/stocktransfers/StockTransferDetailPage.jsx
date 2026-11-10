import { formatBusinessStatus } from "../../../utils/viStatus";
import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    cancelStockTransfer,
    getStockTransferDetail,
    stockTransferErrorMessage,
    submitStockTransfer,
} from "../../../api/business/stockTransfers";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Spinner from "../../../components/ui/Spinner";
import { TRANSFER_REASONS, canEditStockTransfer } from "./stockTransferDraft";
import StockTransferActionDialog from "./StockTransferActionDialog";

const ROOT = "/business/stock-transfers";
const CANCELLABLE_STATUSES = new Set(["DRAFT", "CREATED", "PENDING_SOURCE_CONFIRMATION"]);
const numberFormatter = new Intl.NumberFormat("en-GB");
const quantity = (value) => value == null ? "—" : numberFormatter.format(Number(value));
const dateText = (value) => {
    if (!value) return "—";
    const text = String(value);
    if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
        const [y, m, d] = text.split("-");
        return `${d}/${m}/${y}`;
    }
    const date = new Date(text);
    return Number.isNaN(date.getTime()) ? text : date.toLocaleString("en-GB");
};

function InfoField({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 break-words text-sm font-semibold text-slate-900">{value ?? "—"}</p>
        </div>
    );
}

export default function StockTransferDetailPage() {
    const { transferId } = useParams();
    const navigate = useNavigate();
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [actionType, setActionType] = useState("");
    const [actionError, setActionError] = useState("");
    const [actionNotice, setActionNotice] = useState("");
    const [actionBusy, setActionBusy] = useState(false);
    const [actionUncertain, setActionUncertain] = useState(false);
    const actionInFlight = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        const id = Number(transferId);
        setDetail(null);
        setLoading(true);
        setError("");
        if (!Number.isSafeInteger(id) || id <= 0) {
            setError("Invalid stock transfer identifier.");
            setLoading(false);
            return () => controller.abort();
        }
        getStockTransferDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || typeof data !== "object" || Number(data.id) !== id || !Array.isArray(data.items)) {
                    throw new Error("Invalid transfer details format.");
                }
                setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(stockTransferErrorMessage(requestError, "Unable to load stock transfer details."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [transferId, refresh]);

    const openAction = (type) => {
        if (actionBusy || actionUncertain || !detail) return;
        if (type === "submit" && detail.status !== "DRAFT") return;
        if (type === "cancel" && !CANCELLABLE_STATUSES.has(detail.status)) return;
        setActionError("");
        setActionType(type);
    };

    const closeAction = () => {
        if (actionBusy) return;
        setActionType("");
        setActionError("");
    };

    const confirmAction = async (reasonText) => {
        if (actionInFlight.current || !actionType || !detail || actionUncertain) return;
        const type = actionType;
        if (type === "cancel" && !reasonText.trim()) return;
        actionInFlight.current = true;
        setActionBusy(true);
        setActionError("");
        let requestSent = false;
        try {
            // A fresh GET avoids acting on a status changed by another session.
            const latest = await getStockTransferDetail(detail.id);
            if (Number(latest?.id) !== Number(detail.id) || latest.status !== detail.status) {
                setActionError("Transfer status changed elsewhere. Close this dialog and refresh the details.");
                return;
            }
            if (type === "submit" && latest.status !== "DRAFT") {
                setActionError("Only draft transfers can be submitted.");
                return;
            }
            if (type === "cancel" && !CANCELLABLE_STATUSES.has(latest.status)) {
                setActionError("The current status does not allow cancellation.");
                return;
            }
            requestSent = true;
            const result = type === "submit"
                ? await submitStockTransfer(detail.id)
                : await cancelStockTransfer(detail.id, reasonText.trim());
            const expectedStatus = type === "submit" ? "CREATED" : "CANCELLED";
            if (!result || Number(result.id) !== Number(detail.id) || result.status !== expectedStatus) {
                setActionUncertain(true);
                setActionError("Unable to confirm the transfer status changed. Check the details before retrying.");
                return;
            }
            setActionNotice(type === "submit"
                ? "Stock transfer submitted. The required stock has been reserved at the source warehouse."
                : "Stock transfer cancelled. Reserved stock, if any, has been released back to available stock.");
            setDetail(result);
            setActionType("");
            setRefresh((value) => value + 1);
        } catch (requestError) {
            if (requestSent && !requestError?.response) {
                setActionUncertain(true);
                setActionError("Connection lost after submitting the request. The transfer may have been processed. DO NOT submit or cancel again.");
            } else {
                setActionError(stockTransferErrorMessage(requestError, "This action is not allowed for the transfer's current status."));
            }
        } finally {
            actionInFlight.current = false;
            setActionBusy(false);
        }
    };

    const reason = TRANSFER_REASONS.find((item) => item.value === detail?.reasonCode);
    const statusClass = detail?.status === "DRAFT" ? "bg-slate-100 text-slate-700" : "bg-blue-100 text-blue-700";

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Stock Transfer Details"
                    description="Track stock transfer progress. Transfers can be edited or cancelled only before warehouse picking begins."
                    actions={<div className="flex flex-wrap items-center gap-2">
                        <Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/new`)}>← Create Another Transfer</Button>
                        {detail && canEditStockTransfer(detail.status) &&
                            <Button type="button" disabled={actionBusy || actionUncertain} onClick={() => navigate(`${ROOT}/${detail.id}/edit`)}>Edit Transfer</Button>}
                        {detail?.status === "DRAFT" &&
                            <Button type="button" disabled={actionBusy || actionUncertain} onClick={() => openAction("submit")}>Submit Transfer to Warehouse</Button>}
                        {detail && CANCELLABLE_STATUSES.has(detail.status) && <Button type="button" variant="outline" disabled={actionBusy || actionUncertain} onClick={() => openAction("cancel")}>Cancel Transfer</Button>}
                        <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Refresh</Button>
                    </div>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {actionNotice && <Alert type="success">{actionNotice}</Alert>}
                {actionUncertain && <Alert type="warning">The previous request's outcome is uncertain. Do not repeat the action. Confirm the status with the team.</Alert>}
                {loading ? <Card className="flex items-center gap-3"><Spinner /> Loading transfer details...</Card> : detail && <>
                    <Card className="space-y-5">
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                            <div>
                                <p className="text-sm text-slate-500">Stock Transfer Code</p>
                                <h2 className="text-xl font-bold text-slate-900">{detail.transferCode || `#${detail.id}`}</h2>
                            </div>
                            <span className={`rounded-full px-3 py-1 text-sm font-semibold ${statusClass}`}>{detail.status ? formatBusinessStatus(detail.status) : "—"}</span>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                            <InfoField label="Origin" value={`${detail.fromWarehouseName || "—"} (${detail.fromWarehouseCode || "—"})`} />
                            <InfoField label="Destination" value={`${detail.toWarehouseName || "—"} (${detail.toWarehouseCode || "—"})`} />
                            <InfoField label="Transfer Type" value={detail.transferType || "—"} />
                            <InfoField label="Expected Ship Date" value={dateText(detail.expectedShipmentDate)} />
                            <InfoField label="Expected Receipt Date" value={dateText(detail.expectedReceiptDate)} />
                            <InfoField label="Transfer Reason" value={reason?.label || detail.reasonCode || "—"} />
                            <InfoField label="Created By" value={detail.createdByName || detail.createdByCode || "—"} />
                            <InfoField label="Created At" value={dateText(detail.createdAt)} />
                            <InfoField label="Last Updated" value={dateText(detail.updatedAt)} />
                            <InfoField label="Product Count" value={quantity(detail.totalProducts)} />
                            <InfoField label="Total Requested Quantity" value={quantity(detail.totalRequestedQuantity)} />
                            <InfoField label="Confirmed By" value={detail.confirmedByName || "—"} />
                            <InfoField label="Confirmed At" value={dateText(detail.confirmedAt)} />
                        </div>
                        {detail.reasonNote && <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700"><strong>Reason Notes:</strong> {detail.reasonNote}</p>}
                        {detail.notes && <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700"><strong>Notes:</strong> {detail.notes}</p>}
                        {detail.status === "CANCELLED" && (detail.cancelReason || detail.rejectionReason) &&
                            <p className="rounded-xl bg-orange-50 p-4 text-sm text-slate-800"><strong>Cancellation Reason:</strong> {detail.cancelReason || detail.rejectionReason}</p>}
                    </Card>
                    <Card className="space-y-4">
                        <h2 className="text-lg font-semibold text-slate-900">Transfer Items ({detail.items.length})</h2>
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                            <table className="min-w-[900px] w-full text-left text-sm">
                                <thead className="bg-orange-50 text-xs uppercase text-slate-600">
                                    <tr>
                                        {["SKU", "Product Name", "Unit", "Requested Quantity", "Approved Qty", "Picked", "Closed", "Shipped", "Received"].map((label) => <th className="px-3 py-3" key={label}>{label}</th>)}
                                    </tr>
                                </thead>
                                <tbody>
                                    {detail.items.map((item, index) => <tr key={item.id ?? `${item.productId}-${index}`} className="border-t border-slate-100">
                                        <td className="px-3 py-3">{item.productCode || "—"}</td>
                                        <td className="px-3 py-3">{item.productName || "—"}</td>
                                        <td className="px-3 py-3">{item.baseUnit || "—"}</td>
                                        <td className="px-3 py-3 font-semibold">{quantity(item.requestedQuantity)}</td>
                                        <td className="px-3 py-3">{quantity(item.approvedQuantity)}</td>
                                        <td className="px-3 py-3">{quantity(item.pickedQuantity)}</td>
                                        <td className="px-3 py-3">{quantity(item.packedQuantity)}</td>
                                        <td className="px-3 py-3">{quantity(item.shippedQuantity)}</td>
                                        <td className="px-3 py-3">{quantity(item.receivedQuantity)}</td>
                                    </tr>)}
                                </tbody>
                            </table>
                        </div>
                    </Card>
                </>}
                {actionType && detail && <StockTransferActionDialog
                    key={actionType}
                    type={actionType}
                    transferCode={detail.transferCode || `#${detail.id}`}
                    status={detail.status}
                    busy={actionBusy}
                    error={actionError}
                    uncertain={actionUncertain}
                    onClose={closeAction}
                    onConfirm={confirmAction}
                />}
            </div>
        </MainLayout>
    );
}

