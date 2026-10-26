import { useState } from "react";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";

// Explicit confirmation for every mutation to the team's shared MySQL database.
export default function StockTransferActionDialog({
    type, transferCode, status, busy, error, uncertain, onClose, onConfirm,
}) {
    const [reason, setReason] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const isSubmit = type === "submit";
    const canConfirm = confirmed && !busy && !uncertain && (isSubmit || reason.trim().length > 0);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4">
            <div role="dialog" aria-modal="true" aria-labelledby="stock-transfer-action-title" className="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-xl">
                <div className="border-b border-slate-200 px-6 py-4">
                    <h2 id="stock-transfer-action-title" className="text-lg font-semibold text-slate-900">
                        {isSubmit ? "Xác nhận gửi phiếu điều chuyển" : "Xác nhận hủy phiếu điều chuyển"}
                    </h2>
                </div>
                <div className="space-y-4 px-6 py-5 text-sm text-slate-700">
                    {isSubmit ? <p>
                        Phiếu <strong>{transferCode}</strong> sẽ chuyển từ <strong>DRAFT</strong> sang <strong>CREATED</strong>.
                        Backend sẽ kiểm tra lại tồn khả dụng và <strong>giữ chỗ số lượng hàng tại điểm xuất</strong>.
                        Sau đó không thể gửi lại hoặc sửa tùy ý như DRAFT.
                    </p> : <>
                        <p>
                            Hủy phiếu <strong>{transferCode}</strong> (trạng thái <strong>{status}</strong>) sẽ chuyển sang <strong>CANCELLED</strong>.
                            Nếu phiếu có tồn đang được giữ chỗ, Backend sẽ hoàn phần giữ chỗ tương ứng.
                            Không thể tiếp tục xử lý phiếu đã hủy.
                        </p>
                        <label className="block font-medium text-slate-800" htmlFor="stock-transfer-cancel-reason">Lý do hủy *</label>
                        <textarea
                            id="stock-transfer-cancel-reason"
                            className="min-h-24 w-full rounded-xl border border-slate-300 px-3 py-2 outline-none focus:border-orange-500"
                            placeholder="Nhập lý do hủy phiếu..."
                            value={reason}
                            maxLength={500}
                            disabled={busy || uncertain}
                            onChange={(event) => setReason(event.target.value)}
                        />
                    </>}
                    <label className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-3">
                        <input type="checkbox" checked={confirmed} disabled={busy || uncertain} onChange={(event) => setConfirmed(event.target.checked)} className="mt-1 accent-orange-600" />
                        <span>Tôi hiểu thao tác này <strong>thay đổi dữ liệu thật trên MySQL chung của nhóm</strong>{isSubmit ? " và sẽ giữ chỗ tồn kho." : " và có thể giải phóng phần tồn đã giữ chỗ."}</span>
                    </label>
                    {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                    {uncertain && <Alert type="warning">Không thử lại thao tác này. Hãy làm mới chi tiết, kiểm tra trạng thái và đối chiếu với nhóm trước khi quyết định tiếp.</Alert>}
                </div>
                <div className="flex justify-end gap-3 border-t border-slate-200 px-6 py-4">
                    <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Đóng</Button>
                    <Button type="button" disabled={!canConfirm} onClick={() => onConfirm(reason.trim())}>
                        {busy ? "Đang xử lý..." : (isSubmit ? "Xác nhận gửi CREATED" : "Xác nhận hủy phiếu")}
                    </Button>
                </div>
            </div>
        </div>
    );
}
