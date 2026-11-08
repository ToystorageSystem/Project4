import { useEffect, useState } from "react";
import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import {
    listPurchaseOrderCancellationApprovals,
    reviewPurchaseOrderCancellation,
} from "../../api/businessmanager/purchaseOrders";

function apiError(error) {
    return friendlyBusinessError(error, "Unable to process the cancellation request. Please try again.");
}

function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("en-GB");
}

export default function PurchaseOrderCancellationReviewPanel({ onChanged }) {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [reload, setReload] = useState(0);
    const [selected, setSelected] = useState(null);
    const [decision, setDecision] = useState("");
    const [note, setNote] = useState("");
    const [confirmed, setConfirmed] = useState(false);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listPurchaseOrderCancellationApprovals("PENDING", controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) setRequests(Array.isArray(data) ? data : []);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) setError(apiError(requestError));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [reload]);

    function openDecision(item, action) {
        setSelected(item);
        setDecision(action);
        setConfirmed(false);
        setNote("");
        setError("");
        setSuccess("");
    }

    async function submitReview(event) {
        event.preventDefault();
        if (!selected || busy || !confirmed || (decision === "reject" && note.trim().length < 10)) return;
        setBusy(true);
        setError("");
        try {
            await reviewPurchaseOrderCancellation(selected.id, decision, note);
            setSuccess(decision === "approve" ? "Purchase order cancellation approved." : "Cancellation request rejected. The purchase order remains active.");
            setSelected(null);
            setReload((value) => value + 1);
            onChanged?.();
        } catch (requestError) {
            setError(apiError(requestError));
        } finally {
            setBusy(false);
        }
    }

    return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
                <h2 className="text-lg font-bold text-slate-900">Pending Purchase Order Cancellation Requests</h2>
                <p className="mt-1 text-sm text-slate-500">Review the reasons, supplier confirmations, and decide whether to cancel submitted purchase orders.</p>
            </div>
            <button type="button" onClick={() => setReload((value) => value + 1)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50">Refresh List</button>
        </div>
        {success && <p role="status" className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}
        {error && <p role="alert" className="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {loading && <p className="mt-3 text-sm text-slate-500">Loading cancellation requests...</p>}
        {!loading && !requests.length && !error && <p className="mt-3 text-sm text-slate-500">There are no cancellation requests awaiting review.</p>}
        <div className="mt-4 space-y-3">
            {requests.map((item) => <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0 flex-1 text-sm">
                        <p className="font-bold text-slate-900">{item.orderCode} · Request # {item.id}</p>
                        <p className="mt-1 text-slate-600">Supplier: {item.supplierName || "—"}</p>
                        <p className="mt-1 text-slate-600">Requested By: {item.requestedByName || "—"} · {formatDate(item.requestedAt)}</p>
                        <p className="mt-1 text-slate-700">Lý do: {item.reason}</p>
                        <p className="mt-1 text-slate-700">Supplier Confirmation: {item.supplierAgreed ? "Staff confirmed the supplier's agreement" : "Not Confirmed"}</p>
                    </div>
                    <div className="flex gap-2">
                        <button type="button" onClick={() => openDecision(item, "approve")}
                            className="rounded-lg bg-rose-600 px-3 py-2 text-sm font-semibold text-white hover:bg-rose-700">Approve Cancellation</button>
                        <button type="button" onClick={() => openDecision(item, "reject")}
                            className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100">Reject Request</button>
                    </div>
                </div>
            </div>)}
        </div>
        {selected && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation">
            <form onSubmit={submitReview} className="w-full max-w-lg space-y-4 rounded-2xl bg-white p-6 shadow-2xl" role="dialog" aria-modal="true" aria-labelledby="po-cancel-dialog-title">
                <h3 id="po-cancel-dialog-title" className="text-lg font-bold text-slate-900">{decision === "approve" ? "Confirm Purchase Order Cancellation" : "Confirm Rejection of Cancellation Request"}</h3>
                <p className="text-sm text-slate-700">Purchase Orders {selected.orderCode}. {decision === "approve" ? "If no goods have been received and no related supplier invoices exist, the purchase order will be marked as cancelled." : "The purchase order will remain active. The requester will be able to see your rejection reason."}</p>
                <label className="block text-sm text-slate-700">Reviewer's Notes {decision === "reject" ? "(required, at least 10 characters)" : "(optional)"}
                    <textarea value={note} onChange={(event) => setNote(event.target.value)} maxLength={1000} rows={3}
                        className="mt-1 w-full rounded-xl border border-slate-300 p-3 outline-none focus:border-blue-500" disabled={busy}/>
                </label>
                <label className="flex items-start gap-2 text-sm text-slate-700"><input type="checkbox" checked={confirmed}
                    onChange={(event) => setConfirmed(event.target.checked)} disabled={busy} className="mt-1"/>
                    I have reviewed the information and agree to save this decision.</label>
                <div className="flex justify-end gap-2">
                    <button type="button" disabled={busy} onClick={() => { setSelected(null); setError(""); }}
                        className="rounded-lg border border-slate-300 px-4 py-2 text-sm">Close</button>
                    <button type="submit" disabled={busy || !confirmed || (decision === "reject" && note.trim().length < 10)}
                        className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{busy ? "Processing..." : "Confirm Decision"}</button>
                </div>
                {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
            </form>
        </div>}
    </section>;
}
