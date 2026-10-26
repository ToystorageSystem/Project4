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
import { TRANSFER_REASONS } from "./stockTransferDraft";
import StockTransferActionDialog from "./StockTransferActionDialog";

const ROOT = "/business-staff/stock-transfers";
const CANCELLABLE_STATUSES = new Set(["DRAFT", "CREATED", "PENDING_SOURCE_CONFIRMATION"]);
const numberFormatter = new Intl.NumberFormat("vi-VN");
const quantity = (value) => value == null ? "—" : numberFormatter.format(Number(value));
const dateText = (value) => {
    if (!value) return "—";
    const text = String(value);
    if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
        const [y, m, d] = text.split("-");
        return `${d}/${m}/${y}`;
    }
    const date = new Date(text);
    return Number.isNaN(date.getTime()) ? text : date.toLocaleString("vi-VN");
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
            setError("Mã định danh phiếu điều chuyển không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }
        getStockTransferDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || typeof data !== "object" || Number(data.id) !== id || !Array.isArray(data.items)) {
                    throw new Error("Dữ liệu chi tiết phiếu không đúng định dạng.");
                }
                setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(stockTransferErrorMessage(requestError, "Không tải được chi tiết phiếu điều chuyển."));
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
                setActionError("Phiếu đã thay đổi trạng thái ở nơi khác. Hãy đóng hộp thoại và làm mới chi tiết.");
                return;
            }
            if (type === "submit" && latest.status !== "DRAFT") {
                setActionError("Chỉ phiếu DRAFT mới được gửi.");
                return;
            }
            if (type === "cancel" && !CANCELLABLE_STATUSES.has(latest.status)) {
                setActionError("Trạng thái hiện tại không cho phép hủy phiếu.");
                return;
            }
            requestSent = true;
            const result = type === "submit"
                ? await submitStockTransfer(detail.id)
                : await cancelStockTransfer(detail.id, reasonText.trim());
            const expectedStatus = type === "submit" ? "CREATED" : "CANCELLED";
            if (!result || Number(result.id) !== Number(detail.id) || result.status !== expectedStatus) {
                setActionUncertain(true);
                setActionError("Backend đã phản hồi nhưng chưa xác nhận được trạng thái mới. Không bấm lại; hãy kiểm tra phiếu với nhóm.");
                return;
            }
            setActionNotice(type === "submit"
                ? "Gửi phiếu thành công. Backend đã chuyển sang CREATED và xử lý giữ chỗ tồn kho."
                : "Hủy phiếu thành công. Backend đã chuyển sang CANCELLED và xử lý hoàn giữ chỗ nếu có.");
            setDetail(result);
            setActionType("");
            setRefresh((value) => value + 1);
        } catch (requestError) {
            if (requestSent && !requestError?.response) {
                setActionUncertain(true);
                setActionError("Mất kết nối sau khi gửi yêu cầu. Phiếu có thể đã được xử lý; KHÔNG bấm gửi/hủy lại.");
            } else {
                setActionError(stockTransferErrorMessage(requestError, "Backend từ chối thao tác với phiếu."));
            }
        } finally {
            actionInFlight.current = false;
            setActionBusy(false);
        }
    };

    const reason = TRANSFER_REASONS.find((item) => item.value === detail?.reasonCode);
    const statusClass = detail?.status === "DRAFT" ? "bg-slate-100 text-slate-700" : "bg-blue-100 text-blue-700";

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết phiếu điều chuyển hàng"
                    description="Xem trạng thái thực tế từ Backend. Sửa DRAFT, gửi CREATED hoặc hủy đều yêu cầu xác nhận riêng."
                    actions={<div className="flex flex-wrap items-center gap-2">
                        <Button type="button" variant="outline" onClick={() => navigate(`${ROOT}/new`)}>← Tạo phiếu khác</Button>
                        {detail?.status === "DRAFT" && <>
                            <Button type="button" disabled={actionBusy || actionUncertain} onClick={() => navigate(`${ROOT}/${detail.id}/edit`)}>Sửa DRAFT</Button>
                            <Button type="button" disabled={actionBusy || actionUncertain} onClick={() => openAction("submit")}>Gửi phiếu (CREATED)</Button>
                        </>}
                        {detail && CANCELLABLE_STATUSES.has(detail.status) && <Button type="button" variant="outline" disabled={actionBusy || actionUncertain} onClick={() => openAction("cancel")}>Hủy phiếu</Button>}
                        <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                    </div>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {actionNotice && <Alert type="success">{actionNotice}</Alert>}
                {actionUncertain && <Alert type="warning">Yêu cầu trước có kết quả chưa rõ ràng. Không lặp lại thao tác. Hãy kiểm tra trạng thái với nhóm.</Alert>}
                {loading ? <Card className="flex items-center gap-3"><Spinner /> Đang tải chi tiết phiếu...</Card> : detail && <>
                    <Card className="space-y-5">
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                            <div>
                                <p className="text-sm text-slate-500">Mã phiếu điều chuyển</p>
                                <h2 className="text-xl font-bold text-slate-900">{detail.transferCode || `#${detail.id}`}</h2>
                            </div>
                            <span className={`rounded-full px-3 py-1 text-sm font-semibold ${statusClass}`}>{detail.status || "—"}</span>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                            <InfoField label="Điểm xuất" value={`${detail.fromWarehouseName || "—"} (${detail.fromWarehouseCode || "—"})`} />
                            <InfoField label="Điểm nhận" value={`${detail.toWarehouseName || "—"} (${detail.toWarehouseCode || "—"})`} />
                            <InfoField label="Loại điều chuyển" value={detail.transferType || "—"} />
                            <InfoField label="Ngày xuất dự kiến" value={dateText(detail.expectedShipmentDate)} />
                            <InfoField label="Ngày nhận dự kiến" value={dateText(detail.expectedReceiptDate)} />
                            <InfoField label="Lý do điều chuyển" value={reason?.label || detail.reasonCode || "—"} />
                            <InfoField label="Người tạo" value={detail.createdByName || detail.createdByCode || "—"} />
                            <InfoField label="Ngày tạo" value={dateText(detail.createdAt)} />
                            <InfoField label="Cập nhật lần cuối" value={dateText(detail.updatedAt)} />
                            <InfoField label="Số sản phẩm" value={quantity(detail.totalProducts)} />
                            <InfoField label="Tổng số lượng yêu cầu" value={quantity(detail.totalRequestedQuantity)} />
                            <InfoField label="Người xác nhận" value={detail.confirmedByName || "—"} />
                        </div>
                        {detail.reasonNote && <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700"><strong>Diễn giải:</strong> {detail.reasonNote}</p>}
                        {detail.notes && <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700"><strong>Ghi chú:</strong> {detail.notes}</p>}
                    </Card>
                    <Card className="space-y-4">
                        <h2 className="text-lg font-semibold text-slate-900">Sản phẩm điều chuyển ({detail.items.length})</h2>
                        <div className="overflow-x-auto rounded-xl border border-slate-200">
                            <table className="min-w-[900px] w-full text-left text-sm">
                                <thead className="bg-orange-50 text-xs uppercase text-slate-600">
                                    <tr>
                                        {["Mã SP", "Tên sản phẩm", "Đơn vị", "SL yêu cầu", "SL duyệt", "Đã lấy", "Đã đóng", "Đã xuất", "Đã nhận"].map((label) => <th className="px-3 py-3" key={label}>{label}</th>)}
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
                    <Alert type="info">
                        {detail.status === "DRAFT"
                            ? "Phiếu DRAFT chưa giữ chỗ tồn kho. Có thể sửa, gửi hoặc hủy nếu có quyền theo Backend."
                            : detail.status === "CREATED" || detail.status === "PENDING_SOURCE_CONFIRMATION"
                                ? "Phiếu đã qua DRAFT. Chỉ được hủy theo quyền và trạng thái Backend; thao tác hủy có thể hoàn phần tồn giữ chỗ."
                                : "Phiếu đang ở giai đoạn xử lý khác; không thể sửa hoặc hủy tại trang này."}
                        {" "}Chỉ người tạo hoặc Business Manager/Admin có quyền sửa và hủy theo Service.
                    </Alert>
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
