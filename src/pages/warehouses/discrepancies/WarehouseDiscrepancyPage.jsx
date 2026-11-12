import { useCallback, useEffect, useMemo, useState } from "react";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import Alert from "../../../components/ui/Alert";
import LoadingPage from "../../../components/feedback/LoadingPage";

import {
    getReceivingDiscrepancies,
    startHandlingDiscrepancy,
    updateAcceptedQuantity,
    requestDiscrepancyRecount,
    escalateDiscrepancy,
} from "../../../api/inventories/discrepancy/receivingDiscrepancy.js";

import {
    getStoreReturns,
    resolveStoreReturnAsWarehouse,
    sendStoreReturnDiscrepancyToStore,
    confirmStoreReturn,
} from "../../../api/stores/returns/storeReturnInspection.js";

const sourceTabs = [
    { key: "NCC", label: "Supplier receiving" },
    { key: "STORE_RETURN", label: "Store Return" },
];

const normalize = (value) => String(value ?? "").trim().toUpperCase();

const SeverityBadge = ({ severity, blocking }) => {
    const isMajor = normalize(severity) === "MAJOR" || blocking === true;

    return (
        <span
            className={
                "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold " +
                (isMajor
                    ? "bg-red-100 text-red-700"
                    : "bg-amber-100 text-amber-700")
            }
        >
            {isMajor ? "MAJOR" : "MINOR"}
        </span>
    );
};

const StatusBadge = ({ status }) => {
    const value = normalize(status || "OPEN");
    const cls =
        value === "RESOLVED"
            ? "bg-emerald-100 text-emerald-700"
            : value === "INVESTIGATING"
              ? "bg-blue-100 text-blue-700"
              : value === "CANCELLED"
                ? "bg-slate-100 text-slate-600"
                : "bg-orange-100 text-orange-700";

    return (
        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${cls}`}>
            {value}
        </span>
    );
};

function EmptyState({ children }) {
    return (
        <Card>
            <div className="py-12 text-center text-sm text-slate-500">{children}</div>
        </Card>
    );
}

function NccDiscrepancyTab() {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [drafts, setDrafts] = useState({});

    const load = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const data = await getReceivingDiscrepancies();
            const list = Array.isArray(data) ? data : [];

            setReports(
                list.filter(
                    (report) =>
                        normalize(report?.referenceType) === "GOODS_RECEIPT" ||
                        report?.goodsReceiptId != null
                )
            );
        } catch (e) {
            setError(e.response?.data?.message ?? "Unable to load supplier discrepancies.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const updateDraft = (id, key, value) => {
        setDrafts((current) => ({
            ...current,
            [id]: {
                ...current[id],
                [key]: value,
            },
        }));
    };

    const runAction = async (report, action) => {
        const id = report?.id;
        if (!id) return;

        const draft = drafts[id] ?? {};
        const reason = String(draft.reason ?? "").trim();

        try {
            setBusyId(id);
            setError("");
            setSuccess("");

            if (action === "start") {
                await startHandlingDiscrepancy(id);
            }

            if (action === "accept") {
                const accepted = Number(draft.acceptedQuantity ?? 0);

                if (!Number.isFinite(accepted) || accepted < 0) {
                    throw new Error("Invalid accepted quantity.");
                }
                if (!reason) {
                    throw new Error("Enter a reason for this decision.");
                }
                if (report?.productId == null) {
                    throw new Error("This discrepancy has no product ID.");
                }

                if (normalize(report?.status) === "OPEN") {
                    await startHandlingDiscrepancy(id);
                }

                await updateAcceptedQuantity(
                    id,
                    report.productId,
                    accepted,
                    reason
                );
            }

            if (action === "recount") {
                if (!reason) {
                    throw new Error("Enter a reason for the recount.");
                }
                await requestDiscrepancyRecount(id, reason);
            }

            if (action === "escalate") {
                if (!reason) {
                    throw new Error("Enter a reason for escalating to the Business Manager.");
                }
                await escalateDiscrepancy(id, reason);
            }

            setSuccess("Supplier discrepancy updated.");
            await load();
        } catch (e) {
            setError(
                e.response?.data?.message ??
                    e.message ??
                    "Unable to process this supplier discrepancy."
            );
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <LoadingPage />;

    return (
        <div className="space-y-4">

            {error && <Alert type="danger">{error}</Alert>}
            {success && <Alert type="success">{success}</Alert>}

            {!reports.length ? (
                <EmptyState>No supplier discrepancies.</EmptyState>
            ) : (
                reports.map((report) => {
                    const id = report.id;
                    const draft = drafts[id] ?? {};
                    const resolved = normalize(report.status) === "RESOLVED";
                    const business = normalize(report.responsibleParty) === "BUSINESS_MANAGER";
                    const busy = busyId === id;

                    return (
                        <Card key={id}>
                            <div className="space-y-4">
                                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                                    <div>
                                        <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">
                                            SUPPLIER / GOODS_RECEIPT
                                        </p>
                                        <h3 className="mt-1 text-lg font-semibold text-slate-900">
                                            {report.reportCode ?? `Discrepancy #${id}`}
                                        </h3>
                                        <p className="mt-1 text-sm text-slate-500">
                                            Receipt #{report.referenceId ?? report.goodsReceiptId ?? "-"} · Product #{report.productId ?? "-"}
                                        </p>
                                    </div>

                                    <div className="flex flex-wrap gap-2">
                                        <SeverityBadge severity={report.severity} blocking={report.blocking} />
                                        <StatusBadge status={report.status} />
                                    </div>
                                </div>

                                <div className="grid gap-3 md:grid-cols-3">
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Issue type</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.discrepancyType ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Responsible party</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.responsibleParty ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Blocking</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.blocking ? "Yes" : "No"}</p>
                                    </div>
                                </div>

                                {report.description && (
                                    <p className="text-sm leading-6 text-slate-600">{report.description}</p>
                                )}

                                {!resolved && !business && (
                                    <div className="grid gap-3 lg:grid-cols-[180px_1fr_auto]">
                                        <input
                                            type="number"
                                            min="0"
                                            value={draft.acceptedQuantity ?? ""}
                                            onChange={(e) => updateDraft(id, "acceptedQuantity", e.target.value)}
                                            placeholder="Accepted qty"
                                            className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                        />
                                        <input
                                            type="text"
                                            value={draft.reason ?? ""}
                                            onChange={(e) => updateDraft(id, "reason", e.target.value)}
                                            placeholder="Reason for decision, recount or escalation…"
                                            className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                        />
                                        <div className="flex flex-wrap gap-2">
                                            {normalize(report.status) === "OPEN" && (
                                                <Button disabled={busy} onClick={() => runAction(report, "start")}>Start</Button>
                                            )}
                                            <Button disabled={busy} onClick={() => runAction(report, "accept")}>Accept quantity</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runAction(report, "recount")}>Recount</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runAction(report, "escalate")}>Escalate to Business</Button>
                                        </div>
                                    </div>
                                )}

                                {business && !resolved && (
                                    <div className="rounded-xl border border-amber-100 bg-amber-50 p-3 text-sm text-amber-800">
                                        Escalated to the Business Manager.
                                    </div>
                                )}
                            </div>
                        </Card>
                    );
                })
            )}
        </div>
    );
}

function StoreReturnTab() {
    const [returns, setReturns] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [drafts, setDrafts] = useState({});

    const load = useCallback(async () => {
        try {
            setLoading(true);
            setError("");
            const data = await getStoreReturns();
            setReturns(Array.isArray(data) ? data : []);
        } catch (e) {
            setError(e.response?.data?.message ?? "Unable to load the store return.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const flat = useMemo(
        () =>
            returns.flatMap((storeReturn) =>
                (storeReturn?.discrepancies ?? [])
                    .filter((report) => normalize(report?.referenceType || "STORE_RETURN") === "STORE_RETURN")
                    .map((report) => ({ storeReturn, report }))
            ),
        [returns]
    );

    const updateReason = (id, value) => {
        setDrafts((current) => ({ ...current, [id]: value }));
    };

    const runReportAction = async (storeReturn, report, action) => {
        const reason = String(drafts[report.id] ?? "").trim();
        if (!reason) {
            setError("Enter a reason for the store return decision.");
            return;
        }

        try {
            setBusyId(report.id);
            setError("");
            setSuccess("");

            if (action === "warehouse") {
                await resolveStoreReturnAsWarehouse(storeReturn.returnId, report.id, reason);
            }
            if (action === "store") {
                await sendStoreReturnDiscrepancyToStore(storeReturn.returnId, report.id, reason);
            }

            setSuccess("Store return discrepancy updated.");
            await load();
        } catch (e) {
            setError(e.response?.data?.message ?? e.message ?? "Unable to process the store return.");
        } finally {
            setBusyId(null);
        }
    };

    const runConfirm = async (storeReturn) => {
        try {
            setBusyId(`return-${storeReturn.returnId}`);
            setError("");
            setSuccess("");
            await confirmStoreReturn(storeReturn.returnId);
            setSuccess("Store return confirmed.");
            await load();
        } catch (e) {
            setError(e.response?.data?.message ?? "This store return still has unresolved MAJOR issues.");
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <LoadingPage />;

    return (
        <div className="space-y-4">

            {error && <Alert type="danger">{error}</Alert>}
            {success && <Alert type="success">{success}</Alert>}

            {!flat.length ? (
                <EmptyState>No store return discrepancies.</EmptyState>
            ) : (
                flat.map(({ storeReturn, report }) => {
                    const resolved = normalize(report.status) === "RESOLVED";
                    const busy = busyId === report.id;

                    return (
                        <Card key={`${storeReturn.returnId}-${report.id}`}>
                            <div className="space-y-4">
                                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                                    <div>
                                        <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">STORE RETURN</p>
                                        <h3 className="mt-1 text-lg font-semibold text-slate-900">
                                            {report.reportCode ?? `Discrepancy #${report.id}`}
                                        </h3>
                                        <p className="mt-1 text-sm text-slate-500">
                                            {storeReturn.returnCode ?? `Return #${storeReturn.returnId}`} · Product #{report.productId ?? "-"}
                                        </p>
                                    </div>

                                    <div className="flex flex-wrap gap-2">
                                        <SeverityBadge severity={report.severity} blocking={report.blocking} />
                                        <StatusBadge status={report.status} />
                                    </div>
                                </div>

                                <div className="grid gap-3 md:grid-cols-4">
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Store</p>
                                        <p className="mt-1 font-semibold text-slate-800">{storeReturn.storeName ?? `#${storeReturn.storeId ?? "-"}`}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Issue type</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.discrepancyType ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Responsible party</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.responsibleParty ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Return status</p>
                                        <p className="mt-1 font-semibold text-slate-800">{storeReturn.status ?? "-"}</p>
                                    </div>
                                </div>

                                {report.description && <p className="text-sm text-slate-600">{report.description}</p>}

                                {!resolved && (
                                    <div className="grid gap-3 lg:grid-cols-[1fr_auto]">
                                        <input
                                            value={drafts[report.id] ?? ""}
                                            onChange={(e) => updateReason(report.id, e.target.value)}
                                            placeholder="Reason or outcome of the discussion with the store…"
                                            className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-500"
                                        />
                                        <div className="flex flex-wrap gap-2">
                                            <Button disabled={busy} onClick={() => runReportAction(storeReturn, report, "warehouse")}>Warehouse accepts</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runReportAction(storeReturn, report, "store")}>Send to Store Manager</Button>
                                        </div>
                                    </div>
                                )}

                                {normalize(storeReturn.status) === "PENDING_CONFIRMATION" && (
                                    <div className="border-t border-slate-100 pt-3">
                                        <Button
                                            disabled={busyId === `return-${storeReturn.returnId}`}
                                            onClick={() => runConfirm(storeReturn)}
                                        >
                                            Confirm store return completion
                                        </Button>
                                    </div>
                                )}
                            </div>
                        </Card>
                    );
                })
            )}
        </div>
    );
}

export default function WarehouseDiscrepancyPage() {
    const [tab, setTab] = useState("NCC");

    return (
        <MainLayout>
            <div className="space-y-6">
                <PageHeader
                    title="Discrepancy Reports"
                    description="Review supplier and store return discrepancies in their respective workflows."
                />

                <div className="grid grid-cols-2 gap-2 rounded-2xl bg-slate-100 p-1.5 md:w-[520px]">
                    {sourceTabs.map((item) => (
                        <button
                            key={item.key}
                            type="button"
                            onClick={() => setTab(item.key)}
                            className={
                                "rounded-xl px-4 py-2.5 text-sm font-semibold transition " +
                                (tab === item.key
                                    ? "bg-white text-slate-900 shadow-sm"
                                    : "text-slate-500 hover:text-slate-800")
                            }
                        >
                            {item.label}
                        </button>
                    ))}
                </div>

                {tab === "NCC" ? <NccDiscrepancyTab /> : <StoreReturnTab />}
            </div>
        </MainLayout>
    );
}
