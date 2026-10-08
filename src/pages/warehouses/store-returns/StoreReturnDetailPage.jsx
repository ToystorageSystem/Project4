import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";
import StatusBadge from "../../../components/ui/StatusBadge.jsx";

import {
    confirmStoreReturn,
    getStoreReturn,
    resolveStoreReturnAsWarehouse,
    sendStoreReturnDiscrepancyToStore,
} from "../../../api/stores/returns/storeReturnInspection.js";

const formatDateTime = (value) => {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("en-GB");
};

const SeverityBadge = ({ report }) => {
    const major = report?.severity === "MAJOR" || report?.blocking === true;
    return (
        <span
            className={
                major
                    ? "inline-flex rounded-full bg-red-100 px-2.5 py-1 text-xs font-semibold text-red-700"
                    : "inline-flex rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-700"
            }
        >
            {report?.severity ?? (major ? "MAJOR" : "MINOR")}
        </span>
    );
};

export default function StoreReturnDetailPage() {
    const { returnId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [confirming, setConfirming] = useState(false);
    const [reasons, setReasons] = useState({});
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const load = useCallback(async () => {
        try {
            setLoading(true);
            setError("");
            setData(await getStoreReturn(returnId));
        } catch (err) {
            setError(err.response?.data?.message ?? "Unable to load Store Return detail.");
        } finally {
            setLoading(false);
        }
    }, [returnId]);

    useEffect(() => {
        load();
    }, [load]);

    const discrepancies = Array.isArray(data?.discrepancies) ? data.discrepancies : [];
    const items = Array.isArray(data?.items) ? data.items : [];

    const unresolvedBlocking = useMemo(
        () =>
            discrepancies.filter(
                (report) =>
                    Boolean(report?.blocking) &&
                    ["OPEN", "INVESTIGATING"].includes(
                        String(report?.status ?? "").toUpperCase()
                    )
            ),
        [discrepancies]
    );

    const unresolved = discrepancies.filter((report) =>
        ["OPEN", "INVESTIGATING"].includes(String(report?.status ?? "").toUpperCase())
    );

    const requireReason = (id) => {
        const reason = String(reasons[id] ?? "").trim();
        if (!reason) {
            setError("Reason is required.");
            return null;
        }
        return reason;
    };

    const handleWarehouseResolve = async (report) => {
        const reason = requireReason(report.id);
        if (!reason) return;

        try {
            setBusyId(report.id);
            setError("");
            setSuccess("");
            await resolveStoreReturnAsWarehouse(returnId, report.id, reason);
            setSuccess("Discrepancy resolved as Warehouse responsibility.");
            await load();
        } catch (err) {
            setError(err.response?.data?.message ?? "Unable to resolve discrepancy.");
        } finally {
            setBusyId(null);
        }
    };

    const handleSendStore = async (report) => {
        const reason = requireReason(report.id);
        if (!reason) return;

        try {
            setBusyId(report.id);
            setError("");
            setSuccess("");
            await sendStoreReturnDiscrepancyToStore(returnId, report.id, reason);
            setSuccess("Discrepancy sent to Store Manager for confirmation.");
            await load();
        } catch (err) {
            setError(err.response?.data?.message ?? "Unable to send discrepancy to Store Manager.");
        } finally {
            setBusyId(null);
        }
    };

    const handleConfirmReturn = async () => {
        if (unresolvedBlocking.length > 0) {
            setError("Resolve all MAJOR discrepancies before confirming the Store Return.");
            return;
        }

        if (!window.confirm("Confirm this Store Return and release all remaining held inventory?")) {
            return;
        }

        try {
            setConfirming(true);
            setError("");
            setSuccess("");
            const response = await confirmStoreReturn(returnId);
            setData(response);
            setSuccess("Store Return confirmed successfully.");
        } catch (err) {
            setError(err.response?.data?.message ?? "Unable to confirm Store Return.");
        } finally {
            setConfirming(false);
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <LoadingPage />
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="flex min-h-0 flex-1 flex-col gap-6">
                <PageHeader
                    title={data?.returnCode ?? `Store Return #${returnId}`}
                    description="Warehouse Manager review for returned stock and discrepancy responsibility."
                    actions={
                        <div className="flex gap-2">
                            <Button variant="ghost" onClick={() => navigate("/warehouse/store-returns")}>Back</Button>
                            <Button variant="outline" onClick={load}>Refresh</Button>
                        </div>
                    }
                />

                {error && <Alert type="danger">{error}</Alert>}
                {success && <Alert type="success">{success}</Alert>}

                <Card>
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div>
                            <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">
                                {data?.returnType ?? "STORE RETURN"}
                            </p>
                            <h2 className="mt-1 text-xl font-semibold text-slate-900">
                                {data?.storeName ?? `Store #${data?.storeId ?? "-"}`}
                            </h2>
                            <p className="mt-1 text-sm text-slate-500">
                                Warehouse: {data?.warehouseName ?? data?.warehouseId ?? "-"}
                            </p>
                        </div>
                        <StatusBadge status={String(data?.status ?? "-").toUpperCase()} />
                    </div>

                    <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                        {[
                            ["Requested", data?.totalRequestedQuantity ?? 0],
                            ["Received", data?.totalReceivedQuantity ?? 0],
                            ["Approved", data?.totalApprovedQuantity ?? 0],
                            ["Rejected", data?.totalRejectedQuantity ?? 0],
                            ["MAJOR open", unresolvedBlocking.length],
                        ].map(([label, value]) => (
                            <div key={label} className="rounded-xl bg-slate-50 p-3">
                                <p className="text-xs text-slate-400">{label}</p>
                                <p className="mt-1 text-lg font-semibold text-slate-800">{value}</p>
                            </div>
                        ))}
                    </div>

                    <p className="mt-4 text-xs text-slate-400">
                        Received at: {formatDateTime(data?.receivedAt)}
                    </p>
                </Card>

                <Card className="overflow-hidden p-0">
                    <div className="border-b border-slate-100 p-5">
                        <h3 className="font-semibold text-slate-900">Returned Items</h3>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-200 text-sm">
                            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                                <tr>
                                    <th className="px-4 py-3">Product</th>
                                    <th className="px-4 py-3">Issued</th>
                                    <th className="px-4 py-3">Received</th>
                                    <th className="px-4 py-3">Approved</th>
                                    <th className="px-4 py-3">Rejected</th>
                                    <th className="px-4 py-3">Condition</th>
                                    <th className="px-4 py-3">Note</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 bg-white">
                                {items.map((item) => (
                                    <tr key={item?.itemId ?? item?.productId}>
                                        <td className="px-4 py-3 font-medium text-slate-800">
                                            {item?.productName ?? `Product #${item?.productId ?? "-"}`}
                                        </td>
                                        <td className="px-4 py-3">{item?.issuedQuantity ?? 0}</td>
                                        <td className="px-4 py-3">{item?.receivedQuantity ?? 0}</td>
                                        <td className="px-4 py-3">{item?.approvedQuantity ?? 0}</td>
                                        <td className="px-4 py-3">{item?.rejectedQuantity ?? 0}</td>
                                        <td className="px-4 py-3">{item?.conditionStatus ?? "-"}</td>
                                        <td className="px-4 py-3 text-slate-500">{item?.note ?? "-"}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </Card>

                <Card>
                    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h3 className="font-semibold text-slate-900">Discrepancy Reports</h3>
                            <p className="mt-1 text-sm text-slate-500">
                                MINOR does not block inventory. MAJOR holds the affected product until responsibility is resolved.
                            </p>
                        </div>
                        <div className="text-sm text-slate-500">
                            Open: <span className="font-semibold text-slate-800">{unresolved.length}</span>
                        </div>
                    </div>

                    {discrepancies.length === 0 ? (
                        <div className="rounded-xl bg-emerald-50 px-4 py-4 text-sm text-emerald-700">
                            No discrepancy reports for this Store Return.
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {discrepancies.map((report) => {
                                const open = ["OPEN", "INVESTIGATING"].includes(
                                    String(report?.status ?? "").toUpperCase()
                                );
                                const assignedToStore = report?.responsibleParty === "STORE_MANAGER";
                                const busy = busyId === report.id;

                                return (
                                    <div key={report.id} className="rounded-2xl border border-slate-200 p-4">
                                        <div className="flex flex-wrap items-start justify-between gap-3">
                                            <div>
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <span className="font-semibold text-slate-900">
                                                        {report?.discrepancyType ?? "OTHER"}
                                                    </span>
                                                    <SeverityBadge report={report} />
                                                    <StatusBadge status={String(report?.status ?? "-").toUpperCase()} />
                                                </div>
                                                <p className="mt-2 text-sm text-slate-600">
                                                    {report?.description ?? "-"}
                                                </p>
                                                <p className="mt-2 text-xs text-slate-400">
                                                    Product #{report?.productId ?? "-"} · Responsible: {report?.responsibleParty ?? "-"}
                                                </p>
                                            </div>
                                        </div>

                                        {report?.resolutionNote && (
                                            <div className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600">
                                                {report.resolutionNote}
                                            </div>
                                        )}

                                        {open && !assignedToStore && (
                                            <div className="mt-4 space-y-3">
                                                <textarea
                                                    rows={2}
                                                    value={reasons[report.id] ?? ""}
                                                    onChange={(event) =>
                                                        setReasons((current) => ({
                                                            ...current,
                                                            [report.id]: event.target.value,
                                                        }))
                                                    }
                                                    placeholder="Reason / responsibility note..."
                                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                                />

                                                <div className="flex flex-wrap gap-2">
                                                    <Button
                                                        disabled={busy}
                                                        onClick={() => handleWarehouseResolve(report)}
                                                    >
                                                        Warehouse accepts responsibility
                                                    </Button>
                                                    <Button
                                                        variant="outline"
                                                        disabled={busy}
                                                        onClick={() => handleSendStore(report)}
                                                    >
                                                        Send to Store Manager
                                                    </Button>
                                                </div>
                                            </div>
                                        )}

                                        {open && assignedToStore && (
                                            <div className="mt-4 rounded-xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                                                Waiting for Store Manager confirmation.
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </Card>

                {String(data?.status ?? "").toUpperCase() === "PENDING_CONFIRMATION" && (
                    <Card>
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <h3 className="font-semibold text-slate-900">Final Warehouse confirmation</h3>
                                <p className="mt-1 text-sm text-slate-500">
                                    Confirm only when no unresolved MAJOR discrepancy remains.
                                </p>
                                {unresolvedBlocking.length > 0 && (
                                    <p className="mt-2 text-sm font-medium text-red-600">
                                        {unresolvedBlocking.length} MAJOR discrepancy report(s) still block confirmation.
                                    </p>
                                )}
                            </div>
                            <Button
                                disabled={confirming || unresolvedBlocking.length > 0}
                                onClick={handleConfirmReturn}
                            >
                                {confirming ? "Confirming..." : "Confirm Store Return"}
                            </Button>
                        </div>
                    </Card>
                )}
            </div>
        </MainLayout>
    );
}
