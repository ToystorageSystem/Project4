import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
    cancelPurchaseOrder,
    getPurchaseOrderDetail,
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


const ROOT = "/business-staff/purchase-orders";

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

    useEffect(() => {
        const controller = new AbortController();
        const numericId = Number(orderId);
        setDetail(null);
        setError("");
        setLoading(true);
        if (!Number.isSafeInteger(numericId) || numericId <= 0) {
            setError("ID đơn mua hàng không hợp lệ.");
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

    const canSubmit = detail?.status === "DRAFT" && Array.isArray(detail.items) && detail.items.length > 0;
    // Conservative UI guard: Backend currently does not fully protect partially received orders from cancellation.
    // Only expose cancellation for DRAFT orders; never offer it for already ordered/received or PO-AUTO orders.
    const canCancel = detail?.status === "DRAFT" && !String(detail.orderCode || "").startsWith("PO-AUTO-");

    const openAction = (type) => {
        if (actionBusy) return;
        setActionType(type);
        setActionConfirmed(false);
        setCancelReason("");
        setActionError("");
        setActionSuccess("");
    };

    const closeAction = () => {
        if (actionBusy) return;
        setActionType("");
        setActionError("");
        setActionConfirmed(false);
        setCancelReason("");
    };

    const performAction = async () => {
        if (actionBusy || !actionConfirmed || !detail?.id) return;
        if (actionType === "submit" && !canSubmit) return;
        if (actionType === "cancel" && (!canCancel || !cancelReason.trim())) return;
        if (actionType !== "submit" && actionType !== "cancel") return;
        setActionBusy(true);
        setActionError("");
        try {
            const result = actionType === "submit"
                ? await submitPurchaseOrder(detail.id)
                : await cancelPurchaseOrder(detail.id, cancelReason);
            if (!result?.id) throw new Error("Backend không trả về đơn mua hàng sau thao tác.");
            setDetail(result);
            setActionSuccess(actionType === "submit"
                ? "Đã gửi đơn mua hàng. Backend đã chuyển đơn sang ORDERED."
                : "Đã hủy đơn nháp và lưu lý do hủy vào hệ thống.");
            setActionType("");
            setActionConfirmed(false);
            setCancelReason("");
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
                    description="Thông tin đơn mua hàng được truy xuất trực tiếp từ Backend. Chỉ đơn DRAFT mới được chỉnh sửa."
                    actions={
                        <div className="flex flex-wrap gap-2">
                            <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Danh sách</Button>
                            {detail?.status === "DRAFT" && <Button type="button" onClick={() => navigate(`${ROOT}/${orderId}/edit`)}>Sửa đơn nháp</Button>}
                            {canSubmit && <Button type="button" disabled={actionBusy} onClick={() => openAction("submit")}>Gửi đơn (ORDERED)</Button>}
                            {canCancel && <Button type="button" variant="danger" disabled={actionBusy} onClick={() => openAction("cancel")}>Hủy đơn nháp</Button>}
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
                                        <p className="mt-1 text-sm text-slate-500">Đơn được Backend tạo tự động khi lập phiếu nhập.</p>
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


                        {/* Issue #16 - Supplier Price Comparison */}
                        <SupplierFeedbackPanel purchaseOrder={detail} />
                        <SupplierPriceComparisonPanel
                            purchaseOrderId={detail.id}
                            purchaseOrderStatus={detail.status}
                        />

                        <Alert type="info">Chỉ sửa/gửi đơn DRAFT. Hủy đơn hiện chỉ mở cho DRAFT để tránh ảnh hưởng các đơn đã phát sinh nhận hàng. Gửi đơn không cộng tồn kho; việc nhận hàng thuộc Warehouse.</Alert>
                    </>
                ) : !error ? (
                    <EmptyState title="Không tìm thấy đơn mua hàng" />
                ) : null}
                <Modal
                    open={Boolean(actionType)}
                    title={actionType === "submit" ? "Xác nhận gửi đơn mua hàng" : "Xác nhận hủy đơn nháp"}
                    onClose={closeAction}
                    footer={
                        <>
                            <Button type="button" variant="outline" disabled={actionBusy} onClick={closeAction}>Đóng</Button>
                            <Button type="button" variant={actionType === "cancel" ? "danger" : "primary"}
                                disabled={actionBusy || !actionConfirmed || (actionType === "cancel" && !cancelReason.trim())}
                                onClick={performAction}>
                                {actionBusy ? "Đang xử lý..." : actionType === "submit" ? "Xác nhận gửi đơn" : "Xác nhận hủy đơn"}
                            </Button>
                        </>
                    }
                >
                    <div className="space-y-4">
                        <p className="text-sm text-slate-700">
                            Đơn <strong>{detail?.orderCode}</strong> — {actionType === "submit"
                                ? "gửi đơn sẽ chuyển trạng thái DRAFT → ORDERED; sau đó không thể chỉnh sửa danh sách sản phẩm. Backend sẽ kiểm tra lại NCC, kho và MOQ."
                                : "hủy đơn DRAFT sẽ chuyển sang CANCELLED và lưu lý do; không xóa dữ liệu đơn khỏi hệ thống."}
                        </p>
                        {actionType === "cancel" && <TextArea label="Lý do hủy *" rows={3} value={cancelReason}
                            disabled={actionBusy} onChange={(event) => setCancelReason(event.target.value)}
                            placeholder="Nhập lý do hủy đơn..." />}
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                            <Checkbox checked={actionConfirmed} disabled={actionBusy}
                                onChange={(event) => setActionConfirmed(event.target.checked)}
                                label="Tôi xác nhận thao tác này sẽ thay đổi dữ liệu thật trên MySQL chung của nhóm." />
                        </div>
                        {actionError && <div role="alert"><Alert type="danger">{actionError}</Alert></div>}
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
}
