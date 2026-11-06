import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    getSupplierInvoiceDetail,
    supplierInvoiceErrorMessage,
} from "../../../api/business/supplierInvoices";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";
import {
    SupplierInvoiceAttachmentUploader,
    SupplierInvoiceCancelAction,
    supplierInvoiceCanChange,
} from "./SupplierInvoiceActions";
import {
    formatSupplierInvoiceDate,
    formatSupplierInvoiceMoney,
    SupplierInvoiceMatchBadge,
    SupplierInvoiceStatusBadge,
} from "./supplierInvoiceFormat";

const ROOT = "/business-staff/supplier-invoices";

function DetailField({ label, value }) {
    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 break-words text-sm font-semibold text-slate-900">{value ?? "—"}</p>
        </div>
    );
}

function SafeAttachmentLink({ label, url }) {
    // Attachment URLs originate from the backend; render only https links.
    if (typeof url !== "string" || !/^https:\/\/[^\s]+$/i.test(url)) return null;
    return <a href={url} target="_blank" rel="noopener noreferrer" className="font-semibold text-[#c94810] underline-offset-2 hover:underline">{label}</a>;
}

export default function SupplierInvoiceDetailPage() {
    const { invoiceId } = useParams();
    const navigate = useNavigate();
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [notice, setNotice] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        const id = Number(invoiceId);
        setDetail(null);
        setNotice("");
        setError("");
        setLoading(true);
        if (!Number.isSafeInteger(id) || id <= 0) {
            setError("Mã hóa đơn không hợp lệ.");
            setLoading(false);
            return () => controller.abort();
        }
        getSupplierInvoiceDetail(id, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!data || typeof data !== "object" || !data.id) throw new Error("Thông tin hóa đơn chưa hợp lệ");
                setDetail(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(supplierInvoiceErrorMessage(requestError, "Không tải được chi tiết hóa đơn."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [invoiceId, refresh]);

    const handleUpdated = (updated, message) => {
        setDetail(updated);
        setError("");
        setNotice(message);
    };

    const lineColumns = [
        { key: "productCode", label: "Mã sản phẩm", render: (value) => value || "—" },
        { key: "productName", label: "Sản phẩm", render: (value) => value || "—" },
        { key: "orderedQuantity", label: "SL đặt", render: (value) => value ?? "—" },
        { key: "receivedQuantity", label: "SL nhận", render: (value) => value ?? "—" },
        { key: "unitPrice", label: "Giá PO", render: formatSupplierInvoiceMoney },
        { key: "lineTotal", label: "Thành tiền đơn mua hàng", render: formatSupplierInvoiceMoney },
    ];

    const attachments = detail ? [
        { label: "Xem PDF", url: detail.pdfFileUrl },
        { label: "Xem ảnh", url: detail.imageFileUrl },
        { label: "Xem XML", url: detail.xmlFileUrl },
    ].filter((entry) => typeof entry.url === "string" && /^https:\/\/[^\s]+$/i.test(entry.url)) : [];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Chi tiết hóa đơn nhà cung cấp"
                    description="Xem thông tin hóa đơn và đối chiếu với đơn mua hàng liên quan. Chỉ hóa đơn chưa được xác nhận mới có thể chỉnh sửa."
                    actions={<>
                        <Button type="button" variant="outline" onClick={() => navigate(ROOT)}>← Danh sách</Button>
                        {detail && supplierInvoiceCanChange(detail) && (
                            <Button type="button" onClick={() => navigate(`${ROOT}/${detail.id}/edit`)}>Sửa hóa đơn</Button>
                        )}
                        {detail && <SupplierInvoiceCancelAction invoice={detail} onUpdated={handleUpdated} />}
                        <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                    </>}
                />
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Đang tải chi tiết hóa đơn...
                    </div>
                ) : detail ? (
                    <>
                        <Card>
                            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 pb-4">
                                <div>
                                    <p className="text-sm text-slate-500">Số hóa đơn</p>
                                    <h2 className="text-xl font-bold text-slate-900">{detail.invoiceNumber || "—"}</h2>
                                    <p className="mt-1 text-sm text-slate-500">Mã hệ thống: {detail.invoiceCode || "—"}</p>
                                </div>
                                <SupplierInvoiceStatusBadge status={detail.status} />
                            </div>
                            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                                <DetailField label="Nhà cung cấp" value={detail.supplierName || "—"} />
                                <DetailField label="Mã nhà cung cấp / Mã số thuế" value={`${detail.supplierCode || "—"} / ${detail.supplierTaxCode || "—"}`} />
                                <DetailField label="Đơn mua hàng liên kết" value={detail.purchaseOrderId ? (
                                    <Link className="text-[#c94810] hover:underline" to={`/business-staff/purchase-orders/${detail.purchaseOrderId}`}>
                                        {detail.purchaseOrderCode || `PO #${detail.purchaseOrderId}`}
                                    </Link>
                                ) : "—"} />
                                <DetailField label="Trạng thái đơn mua hàng" value={detail.purchaseOrderStatus || "—"} />
                                <DetailField label="Ngày phát hành" value={formatSupplierInvoiceDate(detail.invoiceDate)} />
                                <DetailField label="Hạn thanh toán" value={formatSupplierInvoiceDate(detail.dueDate)} />
                                <DetailField label="Ký hiệu hóa đơn" value={detail.invoiceSeries || "—"} />
                                <DetailField label="Mã tra cứu" value={detail.lookupCode || "—"} />
                                <DetailField label="Người tải lên" value={detail.uploadedByName || "—"} />
                                <DetailField label="Ngày ghi nhận" value={formatSupplierInvoiceDate(detail.uploadedAt, true)} />
                                <DetailField label="Cập nhật lần cuối" value={formatSupplierInvoiceDate(detail.updatedAt, true)} />
                            </div>
                            {detail.verificationNote && <div className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-700">Ghi chú xác nhận: {detail.verificationNote}</div>}
                        </Card>
                        <Card>
                            <h3 className="mb-4 text-lg font-semibold text-slate-900">Giá trị hóa đơn</h3>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                <DetailField label="Trước thuế (subtotal)" value={formatSupplierInvoiceMoney(detail.subtotal)} />
                                <DetailField label="Tiền thuế" value={formatSupplierInvoiceMoney(detail.taxAmount)} />
                                <DetailField label="Tổng đã gồm thuế" value={formatSupplierInvoiceMoney(detail.totalAmount)} />
                            </div>
                        </Card>
                        <Card>
                            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                                <h3 className="text-lg font-semibold text-slate-900">Đối chiếu với đơn mua hàng</h3>
                                <SupplierInvoiceMatchBadge matched={detail.purchaseOrderMatched} />
                            </div>
                            <p className="mb-4 text-sm text-slate-500">Hệ thống so sánh số tiền <strong>trước thuế</strong> của hóa đơn với giá trị đơn mua hàng.</p>
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                                <DetailField label="Giá trị đơn mua hàng" value={formatSupplierInvoiceMoney(detail.purchaseOrderAmount)} />
                                <DetailField label="Hóa đơn trước thuế" value={formatSupplierInvoiceMoney(detail.subtotal)} />
                                <DetailField label="Chênh lệch giá trị trước thuế" value={formatSupplierInvoiceMoney(detail.amountDifference)} />
                            </div>
                            {detail.purchaseOrderMatched === false && <div className="mt-4" role="status"><Alert type="warning">
                                Số tiền trước thuế của hóa đơn khác với đơn mua hàng. Vui lòng kiểm tra trước khi tiếp tục.
                                {detail.comparisonWarning && <p className="mt-2 text-xs">Thông tin chi tiết: {detail.comparisonWarning}</p>}
                            </Alert></div>}
                            {detail.purchaseOrderMatched === true && <div className="mt-4"><Alert type="success">Giá trị trước thuế của hóa đơn khớp với đơn mua hàng.</Alert></div>}
                        </Card>
                        <Card>
                            <h3 className="mb-4 text-lg font-semibold text-slate-900">Sản phẩm theo đơn mua hàng</h3>
                            <p className="mb-4 text-sm text-slate-500">Danh sách sản phẩm được lấy từ đơn mua hàng đã liên kết.</p>
                            {Array.isArray(detail.purchaseOrderItems) && detail.purchaseOrderItems.length > 0 ? (
                                <Table columns={lineColumns} data={detail.purchaseOrderItems} />
                            ) : (
                                <EmptyState title="Đơn mua hàng chưa có thông tin sản phẩm" />
                            )}
                        </Card>
                        <Card>
                            <h3 className="mb-3 text-lg font-semibold text-slate-900">File hóa đơn</h3>
                            {attachments.length > 0 ? (
                                <div className="flex flex-wrap gap-4 text-sm">
                                    {attachments.map((entry) => <SafeAttachmentLink key={entry.label} label={entry.label} url={entry.url} />)}
                                </div>
                            ) : (
                                <p className="text-sm text-slate-500">Hóa đơn này chưa có tệp chứng từ đính kèm.</p>
                            )}
                            {supplierInvoiceCanChange(detail) ? (
                                <SupplierInvoiceAttachmentUploader invoice={detail} onUpdated={handleUpdated} />
                            ) : (
                                <p className="mt-4 text-xs text-slate-500">Chỉ hóa đơn nháp hoặc đang chờ kiểm tra mới có thể đính kèm hoặc thay tệp.</p>
                            )}
                        </Card>
                        <Alert type="info">Chỉ hóa đơn nháp hoặc đang chờ kiểm tra mới được sửa, hủy và đính kèm tệp. Mỗi lần thay đổi đều cần xác nhận.</Alert>
                    </>
                ) : !error ? <EmptyState title="Không tìm thấy hóa đơn nhà cung cấp" /> : null}
            </div>
        </MainLayout>
    );
}
