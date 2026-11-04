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
    { key: "NCC", label: "Nhận từ NCC" },
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
            setError(e.response?.data?.message ?? "Không tải được discrepancy NCC.");
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
                    throw new Error("Accepted quantity không hợp lệ.");
                }
                if (!reason) {
                    throw new Error("Bạn cần nhập lý do xử lý.");
                }
                if (report?.productId == null) {
                    throw new Error("Discrepancy này chưa có productId.");
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
                    throw new Error("Bạn cần nhập lý do kiểm lại.");
                }
                await requestDiscrepancyRecount(id, reason);
            }

            if (action === "escalate") {
                if (!reason) {
                    throw new Error("Bạn cần nhập lý do chuyển Business Manager.");
                }
                await escalateDiscrepancy(id, reason);
            }

            setSuccess("Đã cập nhật discrepancy NCC.");
            await load();
        } catch (e) {
            setError(
                e.response?.data?.message ??
                    e.message ??
                    "Không thể xử lý discrepancy NCC."
            );
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <LoadingPage />;

    return (
        <div className="space-y-4">
            <div className="rounded-2xl border border-orange-100 bg-orange-50 p-4 text-sm text-slate-700">
                <b>NCC / GOODS_RECEIPT:</b> discrepancy phát sinh khi nhận hàng từ nhà cung cấp.
                MINOR không chặn luồng; MAJOR cần Warehouse Manager xử lý trước khi tiếp tục.
            </div>

            {error && <Alert type="danger">{error}</Alert>}
            {success && <Alert type="success">{success}</Alert>}

            {!reports.length ? (
                <EmptyState>Không có discrepancy NCC.</EmptyState>
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
                                            NCC / GOODS_RECEIPT
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
                                        <p className="text-xs text-slate-400">Loại lỗi</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.discrepancyType ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Phụ trách</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.responsibleParty ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Blocking</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.blocking ? "Có" : "Không"}</p>
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
                                            placeholder="Lý do xử lý / kiểm lại / chuyển Business..."
                                            className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                        />
                                        <div className="flex flex-wrap gap-2">
                                            {normalize(report.status) === "OPEN" && (
                                                <Button disabled={busy} onClick={() => runAction(report, "start")}>Bắt đầu</Button>
                                            )}
                                            <Button disabled={busy} onClick={() => runAction(report, "accept")}>Chấp nhận SL</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runAction(report, "recount")}>Kiểm lại</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runAction(report, "escalate")}>Chuyển Business</Button>
                                        </div>
                                    </div>
                                )}

                                {business && !resolved && (
                                    <div className="rounded-xl border border-amber-100 bg-amber-50 p-3 text-sm text-amber-800">
                                        Đã chuyển Business Manager xử lý.
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
            setError(e.response?.data?.message ?? "Không tải được Store Return.");
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
            setError("Bạn cần nhập lý do xử lý Store Return.");
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

            setSuccess("Đã cập nhật discrepancy Store Return.");
            await load();
        } catch (e) {
            setError(e.response?.data?.message ?? e.message ?? "Không thể xử lý Store Return.");
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
            setSuccess("Đã xác nhận Store Return.");
            await load();
        } catch (e) {
            setError(e.response?.data?.message ?? "Store Return vẫn còn MAJOR chưa xử lý.");
        } finally {
            setBusyId(null);
        }
    };

    if (loading) return <LoadingPage />;

    return (
        <div className="space-y-4">
            <div className="rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm text-slate-700">
                <b>STORE_RETURN:</b> discrepancy phát sinh khi Store trả hàng về Warehouse. Warehouse Manager có thể nhận trách nhiệm tại kho hoặc chuyển Store Manager xác nhận.
            </div>

            {error && <Alert type="danger">{error}</Alert>}
            {success && <Alert type="success">{success}</Alert>}

            {!flat.length ? (
                <EmptyState>Không có discrepancy Store Return.</EmptyState>
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
                                        <p className="text-xs text-slate-400">Loại lỗi</p>
                                        <p className="mt-1 font-semibold text-slate-800">{report.discrepancyType ?? "-"}</p>
                                    </div>
                                    <div className="rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs text-slate-400">Phụ trách</p>
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
                                            placeholder="Lý do / kết quả trao đổi với Store..."
                                            className="rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-500"
                                        />
                                        <div className="flex flex-wrap gap-2">
                                            <Button disabled={busy} onClick={() => runReportAction(storeReturn, report, "warehouse")}>Kho xác nhận</Button>
                                            <Button variant="outline" disabled={busy} onClick={() => runReportAction(storeReturn, report, "store")}>Gửi Store Manager</Button>
                                        </div>
                                    </div>
                                )}

                                {normalize(storeReturn.status) === "PENDING_CONFIRMATION" && (
                                    <div className="border-t border-slate-100 pt-3">
                                        <Button
                                            disabled={busyId === `return-${storeReturn.returnId}`}
                                            onClick={() => runConfirm(storeReturn)}
                                        >
                                            Xác nhận hoàn tất Store Return
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
                    description="Tách riêng discrepancy nhận từ NCC và Store Return để Warehouse Manager xử lý đúng workflow."
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
