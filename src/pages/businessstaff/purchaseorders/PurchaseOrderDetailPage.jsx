import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
    cancelPurchaseOrder,
    getPurchaseOrderDetail,
    getPurchaseOrderCancellationHistory,
    listPurchaseOrderCancellationRequests,
    purchaseOrderErrorMessage,
    submitPurchaseOrder,
} from "../../../api/business/purchaseOrders";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Checkbox from "../../../components/ui/Checkbox";
import EmptyState from "../../../components/ui/EmptyState";
import Modal from "../../../components/ui/Modal";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";
import TextArea from "../../../components/ui/TextArea";
import {
    formatExpectedDeliveryDate,
    formatPurchaseOrderDate,
    formatPurchaseOrderMoney,
    PurchaseOrderStatusBadge,
} from "./purchaseOrderFormat";
import RelatedGoodsReceipts from "../goodsreceipts/RelatedGoodsReceipts";


const ROOT = "/business/purchase-orders";

const cancellationStatusNames = {
    PENDING: "Pending Review",
    APPROVED: "Cancellation Approved",
    REJECTED: "Rejected",
};

function DetailField({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 break-words text-sm font-semibold text-slate-900">{value ?? "—"}</p>
        </div>
    );
}

export default function PurchaseOrderDetailPage() {
    const { orderId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [detail, setDetail] = useState(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [refresh, setRefresh] = useState(0);
    const [actionType, setActionType] = useState("");
    const [actionConfirmed, setActionConfirmed] = useState(false);
    const [cancelReason, setCancelReason] = useState("");
    const [actionBusy, setActionBusy] = useState(false);
    const [actionError, setActionError] = useState("");
    const [actionSuccess, setActionSuccess] = useState("");
    const [supplierAgreed, setSupplierAgreed] = useState(false);
    const [cancellationRequests, setCancellationRequests] = useState([]);
    const [cancellationLoading, setCancellationLoading] = useState(false);
    const [cancellationError, setCancellationError] = useState("");
    const [cancellationCheckedId, setCancellationCheckedId] = useState(null);
    const [cancellationHistory, setCancellationHistory] = useState([]);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [historyError, setHistoryError] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        const numericId = Number(orderId);
        setDetail(null);
        setError("");
        setLoading(true);
        if (!Number.isSafeInteger(numericId) || numericId <= 0) {
            setError("Invalid purchase order ID.");
            setLoading(false);
            return () => controller.abort();
        }
        getPurchaseOrderDetail(numericId, controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setError(purchaseOrderErrorMessage(requestError, "Unable to load purchase order details."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [orderId, refresh]);

    useEffect(() => {
        const controller = new AbortController();
        // Tất cả Business Operations được xem lịch sử yêu cầu hủy cũ.
        // Không phụ thuộc người đã tạo Purchase Order.
        setCancellationCheckedId(null);
        if (!detail?.id) {
            setCancellationRequests([]);
            setCancellationError("");
            setCancellationLoading(false);
            return () => controller.abort();
        }
        setCancellationLoading(true);
        setCancellationError("");
        listPurchaseOrderCancellationRequests(detail.id, controller.signal)
            .then((rows) => {
                if (!controller.signal.aborted) {
                    setCancellationRequests(Array.isArray(rows) ? rows : []);
                    setCancellationCheckedId(detail.id);
                }
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setCancellationError(purchaseOrderErrorMessage(requestError));
            })
            .finally(() => {
                if (!controller.signal.aborted) setCancellationLoading(false);
            });
        return () => controller.abort();
    }, [detail?.id, refresh]);

    useEffect(() => {
        const controller = new AbortController();
        setCancellationHistory([]);
        setHistoryError("");
        if (!detail?.id || detail.status !== "CANCELLED") {
            setHistoryLoading(false);
            return () => controller.abort();
        }
        setHistoryLoading(true);
        getPurchaseOrderCancellationHistory(detail.id, controller.signal)
            .then((rows) => {
                if (!controller.signal.aborted) setCancellationHistory(Array.isArray(rows) ? rows : []);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setHistoryError(purchaseOrderErrorMessage(requestError));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) setHistoryLoading(false);
            });
        return () => controller.abort();
    }, [detail?.id, detail?.status, refresh]);

    const canSubmit = detail?.status === "DRAFT" && Boolean(detail.expectedDeliveryDate)
        && Array.isArray(detail.items) && detail.items.length > 0;
    // Business Operations được hủy PO do bất kỳ nhân viên Business nào lập.
    // Backend vẫn kiểm tra GR, hóa đơn và trạng thái trong một transaction.
    const hasPendingCancellation = cancellationRequests.some((row) => row.status === "PENDING");
    const isAutoGenerated = String(detail?.orderCode || "").startsWith("PO-AUTO-");
    const hasReceivedQuantity = Array.isArray(detail?.items)
        && detail.items.some((item) => Number(item.receivedQuantity || 0) > 0);
    const cancellationCheckReady = Boolean(detail?.id) && cancellationCheckedId === detail.id
        && !cancellationLoading && !cancellationError && !hasPendingCancellation;
    const canCancel = detail?.status === "DRAFT" && cancellationCheckReady && !isAutoGenerated;
    const canCancelOrdered = detail?.status === "ORDERED" && cancellationCheckReady
        && !isAutoGenerated && Array.isArray(detail.items) && !hasReceivedQuantity;
    const cancellationBlockedReason = ["DRAFT", "ORDERED"].includes(detail?.status)
        ? isAutoGenerated
            ? "Automatically generated purchase orders require separate review before cancellation."
            : cancellationError
                ? "Unable to check legacy cancellation requests. Refresh the page."
                : !cancellationCheckReady && !hasPendingCancellation
                    ? "Checking cancellation request status..."
                    : hasPendingCancellation
                        ? "This order has a legacy cancellation request that requires reconciliation."
                        : hasReceivedQuantity
                            ? "This purchase order has already received goods and cannot be cancelled."
                            : ""
        : "";

    const openAction = (type) => {
        if (actionBusy) return;
        setActionType(type);
        setActionConfirmed(false);
        setCancelReason("");
        setSupplierAgreed(false);
        setActionError("");
        setActionSuccess("");
    };

    const closeAction = () => {
        if (actionBusy) return;
        setActionType("");
        setActionError("");
        setActionConfirmed(false);
        setCancelReason("");
        setSupplierAgreed(false);
    };

    const performAction = async () => {
        if (actionBusy || !actionConfirmed || !detail?.id) return;
        if (actionType === "submit" && !canSubmit) return;
        if (actionType === "cancel") {
            const allowed = canCancel || canCancelOrdered;
            const reason = cancelReason.trim();
            if (!allowed || reason.length < (detail.status === "ORDERED" ? 10 : 1)
                || reason.length > 1000) return;
            if (detail.status === "ORDERED" && !supplierAgreed) return;
        }
        if (!["submit", "cancel"].includes(actionType)) return;
        setActionBusy(true);
        setActionError("");
        try {
            const result = actionType === "submit"
                ? await submitPurchaseOrder(detail.id)
                : await cancelPurchaseOrder(detail.id, cancelReason, detail.status === "ORDERED" && supplierAgreed);
            if (!result?.id) throw new Error("Operation result is not confirmed. Refresh the order before retrying.");
            setDetail(result);
            setActionSuccess(actionType === "submit"
                ? "Purchase order submitted. A goods receipt has been created for the warehouse."
                : "Purchase order cancelled. Its history has been retained.");
            setActionType("");
            setActionConfirmed(false);
            setCancelReason("");
            setSupplierAgreed(false);
            setRefresh((value) => value + 1);
        } catch (requestError) {
            setActionError(purchaseOrderErrorMessage(requestError, "Unable to complete the action. Check the order status and try again."));
        } finally {
            setActionBusy(false);
        }
    };

    const columns = [
        { key: "productCode", label: "Product Code", render: (value) => value || "—" },
        { key: "productName", label: "Product Name", render: (value) => value || "—" },
        { key: "baseUnit", label: "Unit", render: (value) => value || "—" },
        { key: "orderedQuantity", label: "Ordered Quantity", render: (value) => value ?? "—" },
        { key: "receivedQuantity", label: "Received Quantity", render: (value) => value ?? "—" },
        { key: "unitPrice", label: "Unit Price", render: formatPurchaseOrderMoney },
        { key: "lineTotal", label: "Line Total", render: formatPurchaseOrderMoney },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Purchase Order Details"
                    description="View products, supplier information, and purchase order progress. Only draft orders can be edited."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Back to List</Button>
                            {detail?.status === "DRAFT" && <Button type="button" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>Edit Draft Order</Button>}
                            {canSubmit && <Button type="button" disabled={actionBusy} onClick={() => openAction("submit")}>Submit Order to Supplier</Button>}

                            {canCancel && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Cancel Draft Order</Button>}
                            {canCancelOrdered && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Cancel Placed Order</Button>}
                            <Button type="button" variant="ghost" disabled={actionBusy} onClick={() => setRefresh((value) => value + 1)}>Refresh</Button>
                        </div>
                    }
                />
                {detail?.status === "DRAFT" && !detail.expectedDeliveryDate && (
                    <Alert type="warning">
                        Set the agreed delivery date before submitting the order.
                        <Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>
                            Edit Draft Order
                        </Button>
                    </Alert>
                )}
                {location.state?.notice && <Alert type="success">{location.state.notice}</Alert>}
                {actionSuccess && <div role="status"><Alert type="success">{actionSuccess}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {cancellationBlockedReason && !loading && <Alert type="warning">{cancellationBlockedReason}</Alert>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Loading purchase order details...
                    </div>
                ) : detail && !error ? (
                    <>
                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <p className="text-sm text-slate-500">Purchase Order Code</p>
                                    <h2 className="text-xl font-bold text-slate-900">{detail.orderCode}</h2>
                                    {String(detail.orderCode || "").startsWith("PO-AUTO-") && (
                                        <p className="mt-1 text-sm text-slate-500">This purchase order was generated by the system along with a goods receipt.</p>
                                    )}
                                </div>
                                <PurchaseOrderStatusBadge status={detail.status} />
                            </div>
                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                                <DetailField label="Supplier" value={detail.supplierName} />
                                <DetailField label="Destination Warehouse / Store" value={detail.warehouseName} />
                                <DetailField label="Delivery Date" value={formatExpectedDeliveryDate(detail.expectedDeliveryDate)} />
                                <DetailField label="Created By" value={detail.createdByName} />
                                <DetailField label="Created At" value={formatPurchaseOrderDate(detail.createdAt)} />
                                <DetailField label="Last Updated" value={formatPurchaseOrderDate(detail.updatedAt)} />
                                <DetailField label="Product Lines" value={detail.totalProducts ?? "—"} />
                                <DetailField label="Total Ordered Quantity" value={detail.totalOrderedQuantity ?? "—"} />
                            </div>
                            {detail.note && <p className="mt-5 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Notes: {detail.note}</p>}
                            {detail.cancelReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-rose-50 p-4 text-sm text-rose-800">Cancellation Reason: {detail.cancelReason}</p>}
                            {detail.rejectionReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Rejection Reason: {detail.rejectionReason}</p>}
                        </Card>
                        {detail.status === "CANCELLED" && <Card>
                            <h2 className="text-lg font-semibold text-slate-900">Cancellation History</h2>
                            <p className="mt-1 text-sm text-slate-500">The system records who cancelled the order and when.</p>
                            {historyLoading && <p className="mt-3 text-sm text-slate-500">Loading history...</p>}
                            {historyError && <div className="mt-3" role="alert"><Alert type="danger">{historyError}</Alert></div>}
                            {!historyLoading && !historyError && cancellationHistory.length === 0 && (
                                <p className="mt-3 text-sm text-slate-500">No legacy cancellation log found. The cancellation reason remains on the order.</p>
                            )}
                            <div className="mt-3 space-y-2">
                                {cancellationHistory.map((entry) => (
                                    <div key={entry.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                                        <p className="font-semibold text-slate-800">Cancelled By: {entry.cancelledByName || `ID ${entry.cancelledById || "—"}`}</p>
                                        <p className="mt-1 text-slate-500">Time: {formatPurchaseOrderDate(entry.cancelledAt)}</p>
                                    </div>
                                ))}
                            </div>
                        </Card>}
                        <Card className="space-y-4">
                            <h2 className="text-lg font-semibold text-slate-900">Purchase Order Items</h2>
                            {Array.isArray(detail.items) && detail.items.length > 0 ? (
                                <Table columns={columns} data={detail.items} />
                            ) : (
                                <EmptyState title="This order contains no products" />
                            )}
                            <div className="flex justify-end border-t border-slate-100 pt-4">
                                <div className="text-right">
                                    <p className="text-sm text-slate-500">Purchase Order Total</p>
                                    <strong className="text-2xl text-slate-900">{formatPurchaseOrderMoney(detail.totalAmount)}</strong>
                                </div>
                            </div>
                        </Card>


                        {(cancellationLoading || cancellationError || cancellationRequests.length > 0) && <Card>
                            <h2 className="text-lg font-semibold text-slate-900">Legacy Cancellation Request History</h2>
                            <p className="mt-1 text-sm text-slate-500">Shows only requests from the legacy workflow. The new workflow does not require approval requests.</p>
                            {cancellationLoading && <p className="mt-3 text-sm text-slate-500">Loading cancellation request history...</p>}
                            {cancellationError && <div className="mt-3" role="alert"><Alert type="danger">{cancellationError}</Alert></div>}
                            {!cancellationLoading && !cancellationError && cancellationRequests.length === 0 && (
                                <p className="mt-3 text-sm text-slate-500">No cancellation requests found.</p>
                            )}
                            <div className="mt-3 space-y-3">
                                {cancellationRequests.map((row) => (
                                    <div key={row.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                                        <p className="font-semibold text-slate-800">Request # {row.id} — {cancellationStatusNames[row.status] || "Status Unknown"}</p>
                                        <p className="mt-1 text-slate-700">Request Reason: {row.reason}</p>
                                        <p className="mt-1 text-slate-500">Requested By: {row.requestedByName || "—"} · {formatPurchaseOrderDate(row.requestedAt)}</p>
                                        {row.reviewedAt && <p className="mt-1 text-slate-500">Reviewed By: {row.reviewedByName || "—"} · {formatPurchaseOrderDate(row.reviewedAt)}</p>}
                                        {row.decisionNote && <p className="mt-1 text-slate-700">Manager's Decision Notes: {row.decisionNote}</p>}
                                    </div>
                                ))}
                            </div>
                        </Card>}

                        <RelatedGoodsReceipts purchaseOrderId={detail.id} />

                    </>
                ) : !error ? (
                    <EmptyState title="Purchase Order Not Found" />
                ) : null}
                <Modal
                    open={Boolean(actionType)}
                    title={actionType === "submit" ? "Confirm Purchase Order Submission" : "Cancel Purchase Order"}
                    onClose={closeAction}
                    footer={
                        <>
                            <Button type="button" variant="outline" disabled={actionBusy} onClick={closeAction}>Close</Button>
                            <Button type="button" variant={actionType === "submit" ? "primary" : "danger"}
                                disabled={actionBusy || !actionConfirmed
                                    || (actionType === "cancel" && (
                                        cancelReason.trim().length < (detail?.status === "ORDERED" ? 10 : 1)
                                        || cancelReason.trim().length > 1000
                                        || (detail?.status === "ORDERED" && !supplierAgreed)
                                    ))}
                                onClick={performAction}>
                                {actionBusy ? "Processing..." : actionType === "submit" ? "Confirm Order Submission" : "Confirm Order Cancellation"}
                            </Button>
                        </>
                    }
                >
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Purchase Orders <strong>{detail?.orderCode}</strong> — {actionType === "submit"
                                ? "Submit the order, changing its status from Draft to Ordered."
                                : detail?.status === "ORDERED"
                                    ? "Cancel directly after confirming supplier agreement. The system will reject cancellation if related goods receipts or invoices exist."
                                    : "The draft order will be marked cancelled, but its history will be retained."}
                        </p>
                        {actionType === "cancel" && <p className="text-sm text-slate-600">Any authorized Business Operations user can cancel an eligible purchase order. The system records the user and cancellation time.</p>}
                        {actionType === "cancel" && <TextArea label="Cancellation Reason *" rows={3} value={cancelReason}
                            disabled={actionBusy} onChange={(event) => setCancelReason(event.target.value)}
                            placeholder="Explain the cancellation reason and arrangements with the supplier..." />}
                        {actionType === "cancel" && detail?.status === "ORDERED" && <Checkbox
                            checked={supplierAgreed} disabled={actionBusy}
                            onChange={(event) => setSupplierAgreed(event.target.checked)}
                            label="I confirm that the supplier has agreed to cancel this order." />}
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                            <Checkbox checked={actionConfirmed} disabled={actionBusy}
                                onChange={(event) => setActionConfirmed(event.target.checked)}
                                label="I have reviewed this action and agree to save it." />
                        </div>
                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}

