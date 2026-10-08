import { useEffect, useMemo, useState } from "react";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";
import StatusBadge from "../../../components/ui/StatusBadge";

import {
    confirmStoreReturn,
    getStoreReturn,
    getStoreReturns,
    resolveStoreReturnAsWarehouse,
    sendStoreReturnDiscrepancyToStore,
} from "../../../api/stores/returns/storeReturnInspection.js";

const number = (value) =>
    typeof value === "number" && Number.isFinite(value) ? value : 0;

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
            {major ? "MAJOR" : "MINOR"}
        </span>
    );
};

function StoreReturnDetail({ data, onBack, onRefresh }) {
    const [reasonDrafts, setReasonDrafts] = useState({});
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const discrepancies = data?.discrepancies ?? [];
    const items = data?.items ?? [];

    const unresolvedBlocking = discrepancies.some(
        (report) =>
            report?.blocking === true &&
            ["OPEN", "INVESTIGATING"].includes(report?.status)
    );

    const runAction = async (report, mode) => {
        const reason = String(reasonDrafts[report.id] ?? "").trim();

        if (!reason) {
            setError("Reason is required.");
            return;
        }

        try {
            setBusy(true);
            setError("");
            setSuccess("");

            if (mode === "warehouse") {
                await resolveStoreReturnAsWarehouse(data.returnId, report.id, reason);
                setSuccess("Discrepancy resolved by Warehouse Manager.");
            } else {
                await sendStoreReturnDiscrepancyToStore(data.returnId, report.id, reason);
                setSuccess("Discrepancy sent to Store Manager for confirmation.");
            }

            await onRefresh();
        } catch (e) {
            setError(
                e.response?.data?.message ??
                e.message ??
                "Unable to process discrepancy."
            );
        } finally {
            setBusy(false);
        }
    };

    const handleConfirm = async () => {
        if (
            !window.confirm(
                "Confirm this Store Return? Unposted inventory will be released according to item condition."
            )
        ) {
            return;
        }

        try {
            setBusy(true);
            setError("");
            setSuccess("");

            await confirmStoreReturn(data.returnId);
            setSuccess("Store Return confirmed successfully.");
            await onRefresh();
        } catch (e) {
            setError(
                e.response?.data?.message ??
                e.message ??
                "Unable to confirm Store Return."
            );
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="space-y-5">
            <Card className="overflow-hidden p-0">
                <div className="h-1 bg-[#f25d19]" />
                <div className="flex flex-col gap-4 border-b border-slate-100 p-5 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                            Store Return Review
                        </p>
                        <h2 className="mt-1 text-xl font-semibold text-slate-900">
                            {data?.returnCode ?? `Return #${data?.returnId ?? "-"}`}
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            {data?.storeName ?? "Store"} → {data?.warehouseName ?? "Warehouse"}
                        </p>
                    </div>

                    <div className="flex items-center gap-2">
                        <StatusBadge status={data?.status} />
                        <Button variant="ghost" onClick={onBack}>Back</Button>
                    </div>
                </div>

                <div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-4">
                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">Requested</p>
                        <p className="mt-1 text-xl font-semibold">{number(data?.totalRequestedQuantity)}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">Received</p>
                        <p className="mt-1 text-xl font-semibold">{number(data?.totalReceivedQuantity)}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">Approved</p>
                        <p className="mt-1 text-xl font-semibold">{number(data?.totalApprovedQuantity)}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">Rejected</p>
                        <p className="mt-1 text-xl font-semibold">{number(data?.totalRejectedQuantity)}</p>
                    </div>
                </div>
            </Card>

            {error && <Alert type="danger">{error}</Alert>}
            {success && <Alert type="success">{success}</Alert>}

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
                        <tbody className="divide-y divide-slate-100">
                            {items.map((item) => (
                                <tr key={item?.itemId ?? item?.productId}>
                                    <td className="px-4 py-3 font-medium text-slate-800">
                                        {item?.productName ?? `Product #${item?.productId ?? "-"}`}
                                    </td>
                                    <td className="px-4 py-3">{number(item?.issuedQuantity)}</td>
                                    <td className="px-4 py-3">{number(item?.receivedQuantity)}</td>
                                    <td className="px-4 py-3">{number(item?.approvedQuantity)}</td>
                                    <td className="px-4 py-3">{number(item?.rejectedQuantity)}</td>
                                    <td className="px-4 py-3">{item?.conditionStatus ?? "-"}</td>
                                    <td className="px-4 py-3 text-slate-500">{item?.note ?? "-"}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </Card>

            <Card className="overflow-hidden p-0">
                <div className="border-b border-slate-100 p-5">
                    <h3 className="font-semibold text-slate-900">Discrepancies</h3>
                    <p className="mt-1 text-sm text-slate-500">
                        MINOR does not block completion. MAJOR remains on hold until responsibility is resolved.
                    </p>
                </div>

                {!discrepancies.length ? (
                    <div className="p-8 text-center text-sm text-slate-400">
                        No discrepancy reports for this return.
                    </div>
                ) : (
                    <div className="divide-y divide-slate-100">
                        {discrepancies.map((report) => {
                            const unresolved = ["OPEN", "INVESTIGATING"].includes(report?.status);
                            const waitingStore = report?.responsibleParty === "STORE_MANAGER";

                            return (
                                <div key={report.id} className="p-5">
                                    <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                                        <div className="space-y-2">
                                            <div className="flex flex-wrap items-center gap-2">
                                                <span className="font-semibold text-slate-900">
                                                    {report?.reportCode ?? `DR #${report?.id}`}
                                                </span>
                                                <SeverityBadge report={report} />
                                                <StatusBadge status={report?.status} />
                                            </div>

                                            <p className="text-sm text-slate-700">
                                                {report?.discrepancyType ?? "OTHER"} · Product #{report?.productId ?? "-"}
                                            </p>
                                            <p className="text-sm text-slate-500">
                                                {report?.description ?? "-"}
                                            </p>
                                            <p className="text-xs text-slate-400">
                                                Responsible: {report?.responsibleParty ?? "-"}
                                            </p>
                                        </div>

                                        {unresolved && !waitingStore && (
                                            <div className="min-w-[320px] space-y-2">
                                                <input
                                                    type="text"
                                                    value={reasonDrafts[report.id] ?? ""}
                                                    onChange={(e) =>
                                                        setReasonDrafts((current) => ({
                                                            ...current,
                                                            [report.id]: e.target.value,
                                                        }))
                                                    }
                                                    placeholder="Reason / responsibility note"
                                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                                />

                                                <div className="flex flex-wrap gap-2">
                                                    <Button
                                                        size="sm"
                                                        disabled={busy}
                                                        onClick={() => runAction(report, "warehouse")}
                                                    >
                                                        Warehouse accepts
                                                    </Button>

                                                    <Button
                                                        size="sm"
                                                        variant="outline"
                                                        disabled={busy}
                                                        onClick={() => runAction(report, "store")}
                                                    >
                                                        Send to Store
                                                    </Button>
                                                </div>
                                            </div>
                                        )}

                                        {waitingStore && (
                                            <div className="rounded-xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
                                                Waiting for Store Manager confirmation.
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </Card>

            {data?.status === "PENDING_CONFIRMATION" && (
                <Card>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                            <h3 className="font-semibold text-slate-900">Final confirmation</h3>
                            <p className="mt-1 text-sm text-slate-500">
                                {unresolvedBlocking
                                    ? "MAJOR discrepancy is still unresolved."
                                    : "No unresolved MAJOR discrepancy remains."}
                            </p>
                        </div>

                        <Button
                            disabled={busy || unresolvedBlocking}
                            onClick={handleConfirm}
                        >
                            Confirm Store Return
                        </Button>
                    </div>
                </Card>
            )}
        </div>
    );
}

export default function StoreReturnManagerPage() {
    const [items, setItems] = useState([]);
    const [selected, setSelected] = useState(null);
    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const loadList = async () => {
        try {
            setLoading(true);
            setError("");
            const data = await getStoreReturns();
            setItems(Array.isArray(data) ? data : []);
        } catch (e) {
            setError(
                e.response?.data?.message ??
                "Unable to load Store Returns."
            );
        } finally {
            setLoading(false);
        }
    };

    const loadDetail = async (returnId) => {
        try {
            setDetailLoading(true);
            setError("");
            setSelected(await getStoreReturn(returnId));
        } catch (e) {
            setError(
                e.response?.data?.message ??
                "Unable to load Store Return detail."
            );
        } finally {
            setDetailLoading(false);
        }
    };

    useEffect(() => {
        loadList();
    }, []);

    const filtered = useMemo(() => {
        const keyword = search.trim().toLowerCase();
        if (!keyword) return items;

        return items.filter((item) =>
            [
                item?.returnCode,
                item?.storeName,
                item?.warehouseName,
                item?.status,
            ]
                .filter(Boolean)
                .some((value) => String(value).toLowerCase().includes(keyword))
        );
    }, [items, search]);

    const refreshSelected = async () => {
        if (!selected?.returnId) return;
        await loadDetail(selected.returnId);
        await loadList();
    };

    return (
        <MainLayout>
            <div className="space-y-6">
                <PageHeader
                    title="Store Returns"
                    description="Warehouse Manager reviews returned goods, discrepancies and responsibility decisions."
                    actions={<Button variant="outline" onClick={loadList}>Refresh</Button>}
                />

                {error && <Alert type="danger">{error}</Alert>}
                {detailLoading && <Alert type="info">Loading Store Return detail...</Alert>}

                {selected ? (
                    <StoreReturnDetail
                        data={selected}
                        onBack={() => setSelected(null)}
                        onRefresh={refreshSelected}
                    />
                ) : (
                    <>
                        <input
                            type="text"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Search return code, store or status..."
                            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />

                        {loading ? (
                            <LoadingPage />
                        ) : (
                            <div className="grid gap-4 xl:grid-cols-2">
                                {filtered.map((item) => (
                                    <Card key={item?.returnId}>
                                        <div className="flex items-start justify-between gap-4">
                                            <div>
                                                <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">
                                                    Store Return
                                                </p>
                                                <h3 className="mt-1 text-lg font-semibold text-slate-900">
                                                    {item?.returnCode ?? `Return #${item?.returnId}`}
                                                </h3>
                                                <p className="mt-2 text-sm text-slate-500">
                                                    {item?.storeName ?? "Store"} → {item?.warehouseName ?? "Warehouse"}
                                                </p>
                                            </div>
                                            <StatusBadge status={item?.status} />
                                        </div>

                                        <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                                            <div className="rounded-xl bg-slate-50 p-3">
                                                <p className="text-xs text-slate-400">Received</p>
                                                <p className="mt-1 font-semibold text-slate-800">
                                                    {number(item?.totalReceivedQuantity)}
                                                </p>
                                            </div>
                                            <div className="rounded-xl bg-slate-50 p-3">
                                                <p className="text-xs text-slate-400">Discrepancies</p>
                                                <p className="mt-1 font-semibold text-slate-800">
                                                    {item?.discrepancies?.length ?? 0}
                                                </p>
                                            </div>
                                        </div>

                                        <Button
                                            className="mt-4 w-full"
                                            variant="outline"
                                            onClick={() => loadDetail(item.returnId)}
                                        >
                                            Review return
                                        </Button>
                                    </Card>
                                ))}

                                {!filtered.length && (
                                    <div className="xl:col-span-2 rounded-2xl border border-dashed border-slate-200 bg-white p-10 text-center text-sm text-slate-400">
                                        No Store Returns found.
                                    </div>
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>
        </MainLayout>
    );
}
