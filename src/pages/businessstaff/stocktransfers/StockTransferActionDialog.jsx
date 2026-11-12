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
                        {isSubmit ? "Confirm Stock Transfer Submission" : "Confirm Stock Transfer Cancellation"}
                    </h2>
                </div>
                <div className="space-y-4 px-6 py-5 text-sm text-slate-700">
                    {isSubmit ? <p>
                        Document <strong>{transferCode}</strong> will be sent to the warehouse team for processing. The system will check available stock and <strong>reserve the required quantity at the source warehouse</strong>. Further edits will be restricted after submission.
                    </p> : <>
                        <p>
                            Cancel Transfer <strong>{transferCode}</strong> (status <strong>{status}</strong>) will be cancelled. Reserved stock, if any, will be released back to available stock. Cancelled transfers cannot be processed further.
                        </p>
                        <label className="block font-medium text-slate-800" htmlFor="stock-transfer-cancel-reason">Cancellation Reason *</label>
                        <textarea
                            id="stock-transfer-cancel-reason"
                            className="min-h-24 w-full rounded-xl border border-slate-300 px-3 py-2 outline-none focus:border-orange-500"
                            placeholder="Enter the cancellation reason..."
                            value={reason}
                            maxLength={500}
                            disabled={busy || uncertain}
                            onChange={(event) => setReason(event.target.value)}
                        />
                    </>}
                    <label className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-3">
                        <input type="checkbox" checked={confirmed} disabled={busy || uncertain} onChange={(event) => setConfirmed(event.target.checked)} className="mt-1 accent-orange-600" />
                        <span>I have reviewed the details and agree to <strong>save the changes</strong>{isSubmit ? " and reserve the stock." : " and release reserved stock if applicable."}</span>
                    </label>
                    {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                    {uncertain && <Alert type="warning">Do not retry this action. Refresh the details, check the status, and confirm with the team before proceeding.</Alert>}
                </div>
                <div className="flex justify-end gap-3 border-t border-slate-200 px-6 py-4">
                    <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Close</Button>
                    <Button type="button" disabled={!canConfirm} onClick={() => onConfirm(reason.trim())}>
                        {busy ? "Processing..." : (isSubmit ? "Confirm Submission" : "Confirm Cancellation")}
                    </Button>
                </div>
            </div>
        </div>
    );
}

