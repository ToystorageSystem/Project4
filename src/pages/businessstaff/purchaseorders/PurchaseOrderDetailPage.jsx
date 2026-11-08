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
import SupplierPriceComparisonPanel from "./SupplierPriceComparisonPanel";
import SupplierFeedbackPanel from "./SupplierFeedbackPanel";
import RelatedGoodsReceipts from "../goodsreceipts/RelatedGoodsReceipts";


const ROOT = "/business/purchase-orders";

const cancellationStatusNames = {
    PENDING: "Chờ xét duyệt",
    APPROVED: "Đã đồng ý hủy",
    REJECTED: "Bị từ chối",
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
            setError("Mã đơn mua hàng không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }
        getPurchaseOrderDetail(numericId, controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setError(purchaseOrderErrorMessage(requestError, "Không tải được chi tiết đơn mua hàng."));
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
            ? "Đơn mua hàng tạo tự động cần kiểm tra riêng trước khi hủy."
            : cancellationError
                ? "Chưa kiểm tra được các yêu cầu hủy cũ. Hãy làm mới trang."
                : !cancellationCheckReady && !hasPendingCancellation
                    ? "Đang kiểm tra trạng thái yêu cầu hủy..."
                    : hasPendingCancellation
                        ? "Đơn đang có yêu cầu hủy từ quy trình cũ cần đối soát."
                        : hasReceivedQuantity
                            ? "Đơn đã có số lượng hàng nhận, không thể hủy."
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
                ? "Đã gửi đơn mua hàng thành công."
                : "Đã hủy đơn mua hàng. Lịch sử vẫn được giữ lại.");
            setActionType("");
            setActionConfirmed(false);
            setCancelReason("");
            setSupplierAgreed(false);
            setRefresh((value) => value + 1);
        } catch (requestError) {
            setActionError(purchaseOrderErrorMessage(requestError, "Không thực hiện được thao tác. Vui lòng kiểm tra trạng thái đơn và thử lại."));
        } finally {
            setActionBusy(false);
        }
    };

    const columns = [
        { key: "productCode", label: "Mã sản phẩm", render: (value) => value || "—" },
        { key: "productName", label: "Tên sản phẩm", render: (value) => value || "—" },
        { key: "baseUnit", label: "Đơn vị", render: (value) => value || "—" },
        { key: "orderedQuantity", label: "Số lượng đặt", render: (value) => value ?? "—" },
        { key: "receivedQuantity", label: "Số lượng đã nhận", render: (value) => value ?? "—" },
        { key: "unitPrice", label: "Đơn giá", render: formatPurchaseOrderMoney },
        { key: "lineTotal", label: "Thành tiền", render: formatPurchaseOrderMoney },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết đơn mua hàng"
                    description="Xem sản phẩm, nhà cung cấp và tiến độ xử lý đơn hàng. Bạn chỉ có thể sửa đơn còn ở trạng thái nháp."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Quay lại danh sách</Button>
                            {detail?.status === "DRAFT" && <Button type="button" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>Sửa đơn nháp</Button>}
                            {canSubmit && <Button type="button" disabled={actionBusy} onClick={() => openAction("submit")}>Gửi đơn cho nhà cung cấp</Button>}

                            {canCancel && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Hủy đơn nháp</Button>}
                            {canCancelOrdered && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Hủy đơn đã đặt</Button>}
                            <Button type="button" variant="ghost" disabled={actionBusy} onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                        </div>
                    }
                />
                {detail?.status === "DRAFT" && !detail.expectedDeliveryDate && (
                    <Alert type="warning">
                        Vui lòng bổ sung ngày nhận hàng mong muốn trước khi gửi đơn.
                        <Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>
                            Sửa đơn nháp
                        </Button>
                    </Alert>
                )}
                {location.state?.notice && <Alert type="success">{location.state.notice}</Alert>}
                {actionSuccess && <div role="status"><Alert type="success">{actionSuccess}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {cancellationBlockedReason && !loading && <Alert type="warning">{cancellationBlockedReason}</Alert>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Đang tải chi tiết đơn mua hàng...
                    </div>
                ) : detail && !error ? (
                    <>
                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <p className="text-sm text-slate-500">Mã đơn mua hàng</p>
                                    <h2 className="text-xl font-bold text-slate-900">{detail.orderCode}</h2>
                                    {String(detail.orderCode || "").startsWith("PO-AUTO-") && (
                                        <p className="mt-1 text-sm text-slate-500">Đơn này được hệ thống tạo cùng với phiếu nhận hàng.</p>
                                    )}
                                </div>
                                <PurchaseOrderStatusBadge status={detail.status} />
                            </div>
                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                                <DetailField label="Nhà cung cấp" value={detail.supplierName} />
                                <DetailField label="Kho/cửa hàng nhận" value={detail.warehouseName} />
                                <DetailField label="Ngày nhận hàng mong muốn" value={formatExpectedDeliveryDate(detail.expectedDeliveryDate)} />
                                <DetailField label="Ngày NCC đề xuất" value={formatExpectedDeliveryDate(detail.supplierProposedDeliveryDate)} />
                                <DetailField label="Ngày giao đã thống nhất" value={formatExpectedDeliveryDate(detail.agreedDeliveryDate)} />
                                <DetailField label="Người tạo" value={detail.createdByName} />
                                <DetailField label="Ngày tạo" value={formatPurchaseOrderDate(detail.createdAt)} />
                                <DetailField label="Cập nhật lần cuối" value={formatPurchaseOrderDate(detail.updatedAt)} />
                                <DetailField label="Số dòng sản phẩm" value={detail.totalProducts ?? "—"} />
                                <DetailField label="Tổng lượng đặt" value={detail.totalOrderedQuantity ?? "—"} />
                            </div>
                            {detail.note && <p className="mt-5 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Ghi chú: {detail.note}</p>}
                            {detail.cancelReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-rose-50 p-4 text-sm text-rose-800">Lý do hủy: {detail.cancelReason}</p>}
                            {detail.rejectionReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Lý do từ chối: {detail.rejectionReason}</p>}
                        </Card>
                        {detail.status === "CANCELLED" && <Card>
                            <h2 className="text-lg font-semibold text-slate-900">Lịch sử hủy đơn</h2>
                            <p className="mt-1 text-sm text-slate-500">Hệ thống ghi nhận người thực hiện và thời điểm hủy.</p>
                            {historyLoading && <p className="mt-3 text-sm text-slate-500">Đang tải lịch sử...</p>}
                            {historyError && <div className="mt-3" role="alert"><Alert type="danger">{historyError}</Alert></div>}
                            {!historyLoading && !historyError && cancellationHistory.length === 0 && (
                                <p className="mt-3 text-sm text-slate-500">Chưa tìm thấy nhật ký hủy cũ. Lý do hủy vẫn được lưu trên đơn.</p>
                            )}
                            <div className="mt-3 space-y-2">
                                {cancellationHistory.map((entry) => (
                                    <div key={entry.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                                        <p className="font-semibold text-slate-800">Người hủy: {entry.cancelledByName || `ID ${entry.cancelledById || "—"}`}</p>
                                        <p className="mt-1 text-slate-500">Thời gian: {formatPurchaseOrderDate(entry.cancelledAt)}</p>
                                    </div>
                                ))}
                            </div>
                        </Card>}
                        <Card className="space-y-4">
                            <h2 className="text-lg font-semibold text-slate-900">Danh sách sản phẩm trong đơn</h2>
                            {Array.isArray(detail.items) && detail.items.length > 0 ? (
                                <Table columns={columns} data={detail.items} />
                            ) : (
                                <EmptyState title="Đơn chưa có sản phẩm" />
                            )}
                            <div className="flex justify-end border-t border-slate-100 pt-4">
                                <div className="text-right">
                                    <p className="text-sm text-slate-500">Tổng giá trị đơn mua hàng</p>
                                    <strong className="text-2xl text-slate-900">{formatPurchaseOrderMoney(detail.totalAmount)}</strong>
                                </div>
                            </div>
                        </Card>


                        {(cancellationLoading || cancellationError || cancellationRequests.length > 0) && <Card>
                            <h2 className="text-lg font-semibold text-slate-900">Lịch sử yêu cầu hủy cũ</h2>
                            <p className="mt-1 text-sm text-slate-500">Chỉ hiển thị yêu cầu từ quy trình cũ. Luồng mới không tạo yêu cầu duyệt.</p>
                            {cancellationLoading && <p className="mt-3 text-sm text-slate-500">Đang tải lịch sử đề nghị hủy...</p>}
                            {cancellationError && <div className="mt-3" role="alert"><Alert type="danger">{cancellationError}</Alert></div>}
                            {!cancellationLoading && !cancellationError && cancellationRequests.length === 0 && (
                                <p className="mt-3 text-sm text-slate-500">Chưa có đề nghị hủy nào.</p>
                            )}
                            <div className="mt-3 space-y-3">
                                {cancellationRequests.map((row) => (
                                    <div key={row.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                                        <p className="font-semibold text-slate-800">Yêu cầu số {row.id} — {cancellationStatusNames[row.status] || "Chưa rõ trạng thái"}</p>
                                        <p className="mt-1 text-slate-700">Lý do đề nghị: {row.reason}</p>
                                        <p className="mt-1 text-slate-500">Người đề nghị: {row.requestedByName || "—"} · {formatPurchaseOrderDate(row.requestedAt)}</p>
                                        {row.reviewedAt && <p className="mt-1 text-slate-500">Người xét duyệt: {row.reviewedByName || "—"} · {formatPurchaseOrderDate(row.reviewedAt)}</p>}
                                        {row.decisionNote && <p className="mt-1 text-slate-700">Ý kiến quản lý: {row.decisionNote}</p>}
                                    </div>
                                ))}
                            </div>
                        </Card>}

                        <RelatedGoodsReceipts purchaseOrderId={detail.id} />

                        {/* Issue #16 - Supplier Price Comparison */}
                        <SupplierFeedbackPanel purchaseOrder={detail} onSaved={() => setRefresh((value) => value + 1)} />
                        <SupplierPriceComparisonPanel
                            purchaseOrderId={detail.id}
                            purchaseOrderStatus={detail.status}
                        />

                        <Alert type="info">Hủy đơn nháp trực tiếp. Với đơn đã đặt, cần xác nhận nhà cung cấp đồng ý. Hệ thống chặn hủy nếu đã có phiếu nhập kho hoặc hóa đơn liên quan.</Alert>
                    </>
                ) : !error ? (
                    <EmptyState title="Không tìm thấy đơn mua hàng" />
                ) : null}
                <Modal
                    open={Boolean(actionType)}
                    title={actionType === "submit" ? "Xác nhận gửi đơn mua hàng" : "Hủy đơn mua hàng"}
                    onClose={closeAction}
                    footer={
                        <>
                            <Button type="button" variant="outline" disabled={actionBusy} onClick={closeAction}>Đóng</Button>
                            <Button type="button" variant={actionType === "submit" ? "primary" : "danger"}
                                disabled={actionBusy || !actionConfirmed
                                    || (actionType === "cancel" && (
                                        cancelReason.trim().length < (detail?.status === "ORDERED" ? 10 : 1)
                                        || cancelReason.trim().length > 1000
                                        || (detail?.status === "ORDERED" && !supplierAgreed)
                                    ))}
                                onClick={performAction}>
                                {actionBusy ? "Đang xử lý..." : actionType === "submit" ? "Xác nhận gửi đơn" : "Xác nhận hủy đơn"}
                            </Button>
                        </>
                    }
                >
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Đơn mua hàng <strong>{detail?.orderCode}</strong> — {actionType === "submit"
                                ? "Gửi đơn từ trạng thái nháp sang đã đặt."
                                : detail?.status === "ORDERED"
                                    ? "Hủy trực tiếp sau khi xác nhận NCC đồng ý. Nếu có phiếu nhập kho hoặc hóa đơn, hệ thống sẽ từ chối."
                                    : "Đơn nháp sẽ được đánh dấu đã hủy; lịch sử vẫn giữ lại."}
                        </p>
                        {actionType === "cancel" && <p className="text-sm text-slate-600">Mọi tài khoản Nghiệp vụ kinh doanh được hủy PO đủ điều kiện. Hệ thống lưu tài khoản hủy và thời điểm.</p>}
                        {actionType === "cancel" && <TextArea label="Lý do hủy đơn *" rows={3} value={cancelReason}
                            disabled={actionBusy} onChange={(event) => setCancelReason(event.target.value)}
                            placeholder="Nêu lý do hủy và phương án với nhà cung cấp..." />}
                        {actionType === "cancel" && detail?.status === "ORDERED" && <Checkbox
                            checked={supplierAgreed} disabled={actionBusy}
                            onChange={(event) => setSupplierAgreed(event.target.checked)}
                            label="Tôi xác nhận nhà cung cấp đã đồng ý hủy đơn này." />}
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                            <Checkbox checked={actionConfirmed} disabled={actionBusy}
                                onChange={(event) => setActionConfirmed(event.target.checked)}
                                label="Tôi đã kiểm tra và đồng ý lưu thao tác này vào hệ thống." />
                        </div>
                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
