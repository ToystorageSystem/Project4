import { useState } from "react";
import {
    cancelSupplierInvoice,
    supplierInvoiceErrorMessage,
    uploadSupplierInvoiceAttachment,
} from "../../../api/business/supplierInvoices";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Modal from "../../../components/ui/Modal";

const EDITABLE_STATUSES = new Set(["DRAFT", "PENDING_VERIFICATION"]);
const FILE_EXTENSION_PATTERN = /\.(pdf|xml|jpe?g|png|webp)$/i;
const FILE_ACCEPT = ".pdf,.xml,.jpg,.jpeg,.png,.webp,application/pdf,application/xml,text/xml,image/jpeg,image/png,image/webp";

export function supplierInvoiceCanChange(invoice) {
    return Boolean(invoice && EDITABLE_STATUSES.has(invoice.status));
}

function mutationError(error, fallback) {
    // Unexpected local data errors should not be shown as Backend connection failures.
    if (error?.response || error?.request) return supplierInvoiceErrorMessage(error, fallback);
    return error?.message || fallback;
}

export function SupplierInvoiceCancelAction({ invoice, onUpdated }) {
    const [open, setOpen] = useState(false);
    const [reason, setReason] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    if (!supplierInvoiceCanChange(invoice)) return null;

    const close = () => {
        if (saving) return;
        setOpen(false);
        setReason("");
        setConfirmed(false);
        setError("");
    };

    const openModal = () => {
        setReason("");
        setConfirmed(false);
        setError("");
        setOpen(true);
    };

    const validReason = reason.trim().length > 0 && reason.trim().length <= 500;

    const cancelInvoice = async () => {
        if (saving || !validReason || !confirmed) return;
        setSaving(true);
        setError("");
        try {
            const updated = await cancelSupplierInvoice(invoice.id, reason.trim());
            if (!updated || Number(updated.id) !== Number(invoice.id) || updated.status !== "CANCELLED") {
                throw new Error("Backend chưa trả về trạng thái hóa đơn CANCELLED hợp lệ. Hãy làm mới để kiểm tra.");
            }
            closeAfterSuccess();
            onUpdated(updated, `Đã hủy hóa đơn ${invoice.invoiceNumber || invoice.invoiceCode}. Lý do được lưu trong lịch sử hóa đơn.`);
        } catch (requestError) {
            setError(mutationError(requestError, "Không thể hủy hóa đơn. Hãy kiểm tra trạng thái hiện tại."));
        } finally {
            setSaving(false);
        }
    };

    const closeAfterSuccess = () => {
        setOpen(false);
        setReason("");
        setConfirmed(false);
    };

    return (
        <>
            <Button type="button" variant="danger" onClick={openModal}>
                Hủy hóa đơn
            </Button>
            <Modal
                open={open}
                title="Xác nhận hủy hóa đơn nhà cung cấp"
                onClose={close}
                footer={
                    <>
                        <Button type="button" variant="outline" onClick={close} disabled={saving}>Đóng</Button>
                        <Button type="button" variant="danger" onClick={cancelInvoice} disabled={saving || !validReason || !confirmed}>
                            {saving ? "Đang hủy..." : "Xác nhận hủy hóa đơn"}
                        </Button>
                    </>
                }
            >
                <div className="space-y-4">
                    <p className="text-sm leading-6 text-slate-700">
                        Hóa đơn <strong>{invoice.invoiceNumber || invoice.invoiceCode}</strong> sẽ chuyển từ
                        <strong> {invoice.status}</strong> sang <strong>CANCELLED</strong>. Hóa đơn không bị xóa khỏi hệ thống.
                    </p>
                    <div>
                        <label htmlFor="supplier-invoice-cancel-reason" className="mb-2 block text-sm font-medium text-slate-800">
                            Lý do hủy * (tối đa 500 ký tự)
                        </label>
                        <textarea
                            id="supplier-invoice-cancel-reason"
                            value={reason}
                            onChange={(event) => {
                                setReason(event.target.value);
                                setConfirmed(false);
                                setError("");
                            }}
                            maxLength={500}
                            rows={4}
                            disabled={saving}
                            placeholder="Nhập lý do hủy hóa đơn nháp sai..."
                            className="w-full resize-y rounded-xl border border-slate-300 p-3 text-sm text-slate-900 outline-none focus:border-[#f25d19] focus:ring-1 focus:ring-[#f25d19]"
                        />
                        <p className="mt-1 text-right text-xs text-slate-500">{reason.length}/500 ký tự</p>
                    </div>
                    <label className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-slate-700">
                        <input type="checkbox" className="mt-1 accent-[#f25d19]" checked={confirmed}
                            onChange={(event) => setConfirmed(event.target.checked)} disabled={saving} />
                        <span>Tôi xác nhận thao tác hủy sẽ thay đổi dữ liệu thật trong MySQL chung của nhóm.</span>
                    </label>
                    {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                </div>
            </Modal>
        </>
    );
}

export function SupplierInvoiceAttachmentUploader({ invoice, onUpdated }) {
    const [file, setFile] = useState(null);
    const [fileInputKey, setFileInputKey] = useState(0);
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    if (!supplierInvoiceCanChange(invoice)) return null;

    const fileError = file && (!FILE_EXTENSION_PATTERN.test(file.name) || file.size <= 0)
        ? "Vui lòng chọn một tệp PDF, XML, JPG, JPEG, PNG hoặc WEBP không rỗng."
        : "";

    const upload = async () => {
        if (saving || !file || fileError || !confirmed) return;
        setSaving(true);
        setError("");
        setSuccess("");
        try {
            const updated = await uploadSupplierInvoiceAttachment(invoice.id, file);
            if (!updated || Number(updated.id) !== Number(invoice.id)) {
                throw new Error("Backend không trả về dữ liệu hóa đơn hợp lệ sau khi tải tệp.");
            }
            setFile(null);
            setFileInputKey((value) => value + 1);
            setConfirmed(false);
            setSuccess("Tải file thành công. Liên kết file được cập nhật ở phần trên.");
            onUpdated(updated, "File hóa đơn đã được tải lên Cloudinary và lưu liên kết trong hệ thống.");
        } catch (requestError) {
            setError(mutationError(requestError, "Không thể tải file hóa đơn. Hãy kiểm tra tệp và cấu hình Cloudinary."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="mt-5 space-y-3 border-t border-slate-200 pt-5">
            <h4 className="font-semibold text-slate-900">Tải file hóa đơn</h4>
            <p className="text-sm text-slate-600">
                Hỗ trợ PDF, XML, JPG, JPEG, PNG, WEBP. Chọn file mới cùng loại sẽ cập nhật đường dẫn file hiện có.
                File được gửi lên Cloudinary qua Backend; giới hạn dung lượng phụ thuộc cấu hình server.
            </p>
            <label htmlFor="supplier-invoice-attachment" className="block text-sm font-medium text-slate-800">
                Chọn file hóa đơn *
            </label>
            <input
                key={fileInputKey}
                id="supplier-invoice-attachment"
                type="file"
                accept={FILE_ACCEPT}
                disabled={saving}
                onChange={(event) => {
                    setFile(event.target.files?.[0] || null);
                    setConfirmed(false);
                    setError("");
                    setSuccess("");
                }}
                className="block w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-[#fff1e9] file:px-3 file:py-2 file:font-semibold file:text-[#c94810]"
            />
            {file && <p className="break-all text-xs text-slate-600">Đã chọn: {file.name} ({(file.size / 1024).toFixed(1)} KB)</p>}
            {fileError && <div role="alert"><Alert type="danger">{fileError}</Alert></div>}
            <label className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-slate-700">
                <input type="checkbox" className="mt-1 accent-[#f25d19]" checked={confirmed}
                    onChange={(event) => setConfirmed(event.target.checked)} disabled={saving} />
                <span>Tôi xác nhận thao tác sẽ tải file lên Cloudinary và cập nhật dữ liệu thật trong MySQL chung.</span>
            </label>
            {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
            {success && <div role="status"><Alert type="success">{success}</Alert></div>}
            <div className="flex justify-end">
                <Button type="button" onClick={upload} disabled={saving || !file || !!fileError || !confirmed}>
                    {saving ? "Đang tải file..." : "Tải file hóa đơn"}
                </Button>
            </div>
        </div>
    );
}
