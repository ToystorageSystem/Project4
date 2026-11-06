import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../../auth/AuthContext";
import {
    cancelPurchaseOrder,
    getPurchaseOrderDetail,
    listPurchaseOrderCancellationRequests,
    requestPurchaseOrderCancellation,
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


const ROOT = "/business-staff/purchase-orders";

const cancellationStatusNames = {
    PENDING: "Chờ xét duyệt",
    APPROVED: "Đã đồng ý hủy",
    REJECTED: "Đã từ chối",
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
    const { user } = useAuth();
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
        if (!detail?.id || Number(detail.createdById) !== Number(user?.id)) {
            setCancellationRequests([]);
            setCancellationError("");
            return () => controller.abort();
        }
        setCancellationLoading(true);
        setCancellationError("");
        listPurchaseOrderCancellationRequests(detail.id, controller.signal)
            .then((rows) => {
                if (!controller.signal.aborted) setCancellationRequests(Array.isArray(rows) ? rows : []);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setCancellationError(purchaseOrderErrorMessage(requestError));
            })
            .finally(() => {
                if (!controller.signal.aborted) setCancellationLoading(false);
            });
        return () => controller.abort();
    }, [detail?.id, detail?.createdById, refresh, user?.id]);

    const canSubmit = detail?.status === "DRAFT" && Array.isArray(detail.items) && detail.items.length > 0;
    // Conservative UI guard: Backend currently does not fully protect partially received orders from cancellation.
    // Only expose cancellation for DRAFT orders; never offer it for already ordered/received or PO-AUTO orders.
    const isCreator = Boolean(user?.id) && Number(user.id) === Number(detail?.createdById);
    const hasPendingCancellation = cancellationRequests.some((row) => row.status === "PENDING");
    const canCancel = detail?.status === "DRAFT" && isCreator && !String(detail.orderCode || "").startsWith("PO-AUTO-");
    const canRequestCancellation = detail?.status === "ORDERED" && isCreator
        && !cancellationLoading && !cancellationError && !hasPendingCancellation
        && !String(detail.orderCode || "").startsWith("PO-AUTO-")
        && Array.isArray(detail.items)
        && detail.items.every((item) => Number(item.receivedQuantity || 0) === 0);

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
        if (actionType === "cancel" && (!canCancel || !cancelReason.trim())) return;
        if (actionType === "requestCancel" && (
            !canRequestCancellation || cancelReason.trim().length < 10 || !supplierAgreed
        )) return;
        if (!["submit", "cancel", "requestCancel"].includes(actionType)) return;
        setActionBusy(true);
        setActionError("");
        try {
            if (actionType === "requestCancel") {
                const response = await requestPurchaseOrderCancellation(detail.id, {
                    reason: cancelReason.trim(),
                    supplierAgreed: true,
                });
                if (!response?.id) throw new Error("Chưa xác nhận được yêu cầu hủy đã được lưu. Vui lòng kiểm tra lại lịch sử trước khi gửi lần nữa.");
                setActionSuccess("Đã gửi đề nghị hủy đơn. Đơn mua hàng vẫn có hiệu lực cho đến khi quản lý xét duyệt.");
            } else {
                const result = actionType === "submit"
                    ? await submitPurchaseOrder(detail.id)
                    : await cancelPurchaseOrder(detail.id, cancelReason);
                if (!result?.id) throw new Error("Chưa xác nhận được kết quả. Vui lòng tải lại đơn hàng để kiểm tra.");
                setDetail(result);
                setActionSuccess(actionType === "submit"
                    ? "Đã gửi đơn mua hàng thành công."
                    : "Đã hủy đơn mua hàng nháp.");
            }
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
        { key: "orderedQuantity", label: "SL đặt", render: (value) => value ?? "—" },
        { key: "receivedQuantity", label: "SL nhận", render: (value) => value ?? "—" },
        { key: "unitPrice", label: "Đơn giá", render: formatPurchaseOrderMoney },
        { key: "lineTotal", label: "Thành tiền", render: formatPurchaseOrderMoney },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết đơn mua hàng"
                    description="Xem sản phẩm, nhà cung cấp và tiến độ xử lý đơn hàng. Bạn chỉ có thể sửa đơn còn ở trạng thái nháp."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Danh sách</Button>
                            {detail?.status === "DRAFT" && <Button type="button" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>Sửa đơn nháp</Button>}
                            {canSubmit && <Button type="button" disabled={actionBusy} onClick={() => openAction("submit")}>Gửi đơn cho nhà cung cấp</Button>}
                            {canCancel && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Hủy đơn nháp</Button>}
                            {canRequestCancellation && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("requestCancel")}>Đề nghị hủy đơn</Button>}
                            <Button type="button" variant="ghost" disabled={actionBusy} onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                        </div>
                    }
                />
                {location.state?.notice && <Alert type="success">{location.state.notice}</Alert>}
                {actionSuccess && <div role="status"><Alert type="success">{actionSuccess}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
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
                                <DetailField label="Kho nhận" value={detail.warehouseName} />
                                <DetailField label="Dự kiến giao hàng" value={formatExpectedDeliveryDate(detail.expectedDeliveryDate)} />
                                <DetailField label="Người tạo" value={detail.createdByName} />
                                <DetailField label="Ngày tạo" value={formatPurchaseOrderDate(detail.createdAt)} />
                                <DetailField label="Cập nhật lần cuối" value={formatPurchaseOrderDate(detail.updatedAt)} />
                                <DetailField label="Người duyệt" value={detail.approvedByName || "—"} />
                                <DetailField label="Số mặt hàng" value={detail.totalProducts ?? "—"} />
                                <DetailField label="Tổng lượng đặt" value={detail.totalOrderedQuantity ?? "—"} />
                            </div>
                            {detail.note && <p className="mt-5 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Ghi chú: {detail.note}</p>}
                            {detail.cancelReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-rose-50 p-4 text-sm text-rose-800">Lý do hủy: {detail.cancelReason}</p>}
                            {detail.rejectionReason && <p className="mt-3 whitespace-pre-wrap rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Lý do từ chối: {detail.rejectionReason}</p>}
                        </Card>
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


                        {isCreator && <Card>
                            <h2 className="text-lg font-semibold text-slate-900">Lịch sử đề nghị hủy đơn</h2>
                            <p className="mt-1 text-sm text-slate-500">Đơn đã gửi chỉ được hủy khi quản lý kinh doanh đồng ý. Việc gửi đề nghị không làm thay đổi số lượng hàng trong kho.</p>
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
                        <SupplierFeedbackPanel purchaseOrder={detail} />
                        <SupplierPriceComparisonPanel
                            purchaseOrderId={detail.id}
                            purchaseOrderStatus={detail.status}
                        />

                        <Alert type="info">Người lập có thể hủy đơn nháp. Với đơn đã gửi, nhân viên cần gửi đề nghị để quản lý xét duyệt. Đơn đã có phiếu nhận hàng hoặc hóa đơn liên quan sẽ không được hủy theo cách này.</Alert>
                    </>
                ) : !error ? (
                    <EmptyState title="Không tìm thấy đơn mua hàng" />
                ) : null}
                <Modal
                    open={Boolean(actionType)}
                    title={actionType === "submit" ? "Xác nhận gửi đơn mua hàng" : actionType === "cancel" ? "Hủy đơn mua hàng nháp" : "Đề nghị hủy đơn mua hàng"}
                    onClose={closeAction}
                    footer={
                        <>
                            <Button type="button" variant="outline" disabled={actionBusy} onClick={closeAction}>Đóng</Button>
                            <Button type="button" variant={actionType !== "submit" ? "danger" : "primary"}
                                disabled={actionBusy || !actionConfirmed
                                    || (actionType === "cancel" && !cancelReason.trim())
                                    || (actionType === "requestCancel" && (cancelReason.trim().length < 10 || !supplierAgreed))}
                                onClick={performAction}>
                                {actionBusy ? "Đang xử lý..." : actionType === "submit" ? "Xác nhận gửi đơn" : actionType === "cancel" ? "Xác nhận hủy đơn nháp" : "Gửi đề nghị hủy"}
                            </Button>
                        </>
                    }
                >
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Đơn mua hàng <strong>{detail?.orderCode}</strong> — {actionType === "submit"
                                ? "Sau khi gửi, đơn sẽ chuyển từ trạng thái nháp sang đã gửi nhà cung cấp."
                                : actionType === "cancel"
                                    ? "Đơn nháp sẽ được đánh dấu đã hủy. Lịch sử đơn hàng vẫn được giữ lại."
                                    : "Đề nghị sẽ được chuyển đến quản lý kinh doanh. Trong thời gian chờ xét duyệt, đơn vẫn có hiệu lực."}
                        </p>
                        {["cancel", "requestCancel"].includes(actionType) && <TextArea label="Lý do hủy đơn *" rows={3} value={cancelReason}
                            disabled={actionBusy} onChange={(event) => setCancelReason(event.target.value)}
                            placeholder="Ví dụ: Nhà cung cấp đề nghị tăng giá nhưng hai bên không thống nhất được..." />}
                        {actionType === "requestCancel" && <Checkbox checked={supplierAgreed} disabled={actionBusy}
                            onChange={(event) => setSupplierAgreed(event.target.checked)}
                            label="Tôi xác nhận đã trao đổi và nhà cung cấp đồng ý hủy đơn này." />}
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                            <Checkbox checked={actionConfirmed} disabled={actionBusy}
                                onChange={(event) => setActionConfirmed(event.target.checked)}
                                label="Tôi đã kiểm tra thông tin và đồng ý lưu thao tác này vào hệ thống." />
                        </div>
                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
