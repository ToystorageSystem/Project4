import { useEffect, useMemo, useState } from "react";

import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import StatusBadge from "../../../components/ui/StatusBadge";

import {
    getReceivingDiscrepanciesByReceipt,
    startHandlingDiscrepancy,
    updateAcceptedQuantity,
    escalateDiscrepancy,
} from "../../../api/inventories/discrepancy/receivingDiscrepancy.js";

const number = (value) => (typeof value === "number" ? value : Number(value ?? 0) || 0);

const formatDateTime = (value) => {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("en-GB");
};

const getInspectionResult = (item) => {
    const result = item?.inspectionResult ?? item?.inspectedResult ?? item?.result;
    if (result) return String(result).toUpperCase();
    if (number(item?.damagedQuantity) > 0) return "DAMAGED";
    if (number(item?.shortageQuantity) > 0) return "SHORTAGE";
    if (number(item?.surplusQuantity) > 0) return "SURPLUS";

    const expected = number(item?.expectedQuantity);
    const actual = number(item?.actualQuantity);
    if (actual < expected) return "SHORTAGE";
    if (actual > expected) return "SURPLUS";
    return "MATCHED";
};

const hasIssue = (item) => getInspectionResult(item) !== "MATCHED";

const SeverityBadge = ({ report }) => {
    if (!report) return null;
    const major = String(report?.severity ?? "MINOR").toUpperCase() === "MAJOR" || report?.blocking === true;

    return (
        <span
            className={
                "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold " +
                (major ? "bg-red-100 text-red-700" : "bg-amber-100 text-amber-700")
            }
        >
            {major ? "MAJOR" : "MINOR"}
        </span>
    );
};

const SummaryItem = ({ label, value }) => (
    <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
        <p className="text-xs text-slate-400">{label}</p>
        <p className="mt-1 text-xl font-semibold text-slate-800">{value}</p>
    </div>
);

export default function ReceivingReviewDetail({
    data,
    confirming = false,
    onBack,
    onConfirm,
    onRequestReinspection,
}) {
    const [reports, setReports] = useState([]);
    const [reportsLoading, setReportsLoading] = useState(false);
    const [acceptedDrafts, setAcceptedDrafts] = useState({});
    const [reasonDrafts, setReasonDrafts] = useState({});
    const [busy, setBusy] = useState(false);
    const [actionError, setActionError] = useState("");
    const [actionSuccess, setActionSuccess] = useState("");

    const items = data?.products ?? data?.items ?? data?.inspectionItems ?? [];
    const status = String(data?.status ?? "INSPECTED").toUpperCase();
    const readOnly = status === "COMPLETED";
    const receiptId = data?.receiptId ?? data?.id;

    const issues = useMemo(() => items.filter(hasIssue), [items]);

    const totalProducts = data?.totalProducts ?? items.length;
    const expected = data?.totalExpectedQuantity ?? items.reduce((sum, item) => sum + number(item?.expectedQuantity), 0);
    const actual = data?.totalActualQuantity ?? items.reduce((sum, item) => sum + number(item?.actualQuantity), 0);
    const damaged = data?.totalDamagedQuantity ?? items.reduce((sum, item) => sum + number(item?.damagedQuantity), 0);
    const shortage = data?.totalShortageQuantity ?? items.reduce((sum, item) => sum + number(item?.shortageQuantity), 0);

    const loadReports = async () => {
        if (!receiptId) {
            setReports([]);
            return;
        }

        try {
            setReportsLoading(true);
            const response = await getReceivingDiscrepanciesByReceipt(receiptId);
            setReports(Array.isArray(response) ? response : []);
        } catch (e) {
            console.error("Unable to load discrepancy reports", e);
            setReports([]);
        } finally {
            setReportsLoading(false);
        }
    };

    useEffect(() => {
        loadReports();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [receiptId]);

    if (!data) return null;

    const getReportForItem = (item) => {
        const productId = item?.productId ?? item?.id;
        const issueType = getInspectionResult(item);

        return (
            reports.find(
                (report) =>
                    Number(report?.productId) === Number(productId) &&
                    String(report?.discrepancyType ?? "").toUpperCase() === issueType
            ) ?? null
        );
    };

    const resolveItem = async (item) => {
        const productId = item?.productId ?? item?.id;
        const report = getReportForItem(item);
        const reason = String(reasonDrafts[productId] ?? "").trim();
        const accepted = Number(
            acceptedDrafts[productId] ?? item?.acceptedQuantity ?? 0
        );

        if (!report?.id) {
            setActionError(`Không tìm thấy ${getInspectionResult(item)} discrepancy của sản phẩm này.`);
            return;
        }
        if (!reason) {
            setActionError("Resolution reason is required.");
            return;
        }
        if (!Number.isFinite(accepted) || accepted < 0) {
            setActionError("Accepted quantity is invalid.");
            return;
        }
        if (String(report.status).toUpperCase() === "RESOLVED") {
            setActionError("This discrepancy has already been resolved.");
            return;
        }
        if (String(report.responsibleParty).toUpperCase() === "BUSINESS_MANAGER") {
            setActionError("This discrepancy has already been escalated to Business Manager.");
            return;
        }

        try {
            setBusy(true);
            setActionError("");
            setActionSuccess("");

            if (String(report.status).toUpperCase() === "OPEN") {
                await startHandlingDiscrepancy(report.id);
            }

            await updateAcceptedQuantity(
                report.id,
                productId,
                accepted,
                reason
            );

            setActionSuccess("Accepted quantity updated and discrepancy resolved.");
            await loadReports();
        } catch (e) {
            setActionError(
                e.response?.data?.message ?? e.message ?? "Unable to resolve discrepancy."
            );
        } finally {
            setBusy(false);
        }
    };

    const escalateItem = async (item) => {
        const productId = item?.productId ?? item?.id;
        const report = getReportForItem(item);
        const reason = String(reasonDrafts[productId] ?? "").trim();

        if (!report?.id) {
            setActionError(`Không tìm thấy ${getInspectionResult(item)} discrepancy của sản phẩm này.`);
            return;
        }
        if (!reason) {
            setActionError("Escalation reason is required.");
            return;
        }

        try {
            setBusy(true);
            setActionError("");
            setActionSuccess("");
            await escalateDiscrepancy(report.id, reason);
            setActionSuccess("Discrepancy escalated to Business Manager.");
            await loadReports();
        } catch (e) {
            setActionError(
                e.response?.data?.message ?? e.message ?? "Unable to escalate discrepancy."
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
                            {readOnly ? "Receiving History" : "Receiving Review - NCC"}
                        </p>
                        <h2 className="mt-1 text-xl font-semibold text-slate-900">
                            {data?.receiptCode ?? `Receipt #${receiptId ?? "-"}`}
                        </h2>
                    </div>
                    <div className="flex items-center gap-2">
                        <StatusBadge status={status} />
                        <Button variant="ghost" onClick={onBack}>Back</Button>
                    </div>
                </div>

                <div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-6">
                    <SummaryItem label="Products" value={totalProducts} />
                    <SummaryItem label="Expected Qty" value={expected} />
                    <SummaryItem label="Actual Qty" value={actual} />
                    <SummaryItem label="Damaged Qty" value={damaged} />
                    <SummaryItem label="Shortage Qty" value={shortage} />
                    <SummaryItem label="Issues" value={issues.length} />
                </div>
            </Card>

            <Card>
                <div className="grid gap-4 lg:grid-cols-3">
                    <div className="rounded-2xl border border-orange-100 bg-[#fff8f4] p-5">
                        <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">Warehouse Staff</p>
                        <p className="mt-3 text-lg font-semibold text-slate-900">
                            {data?.staffName ?? data?.receivedByName ?? "-"}
                        </p>
                    </div>
                    <div className="rounded-2xl border border-slate-100 bg-white p-5">
                        <p className="text-xs text-slate-400">Inspection Started</p>
                        <p className="mt-2 font-semibold text-slate-800">
                            {formatDateTime(data?.receivingStartedAt ?? data?.receivedAt)}
                        </p>
                    </div>
                    <div className="rounded-2xl border border-slate-100 bg-white p-5">
                        <p className="text-xs text-slate-400">Inspection Completed</p>
                        <p className="mt-2 font-semibold text-slate-800">
                            {formatDateTime(data?.inspectionCompletedAt ?? data?.inspectedAt)}
                        </p>
                    </div>
                </div>
            </Card>

            {actionError && (
                <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {actionError}
                </div>
            )}
            {actionSuccess && (
                <div className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                    {actionSuccess}
                </div>
            )}

            <Card className="overflow-hidden p-0">
                <div className="border-b border-slate-100 p-5">
                    <h3 className="font-semibold text-slate-900">NCC discrepancy review</h3>
                    <p className="mt-1 text-sm text-slate-500">
                        MINOR không chặn luồng. MAJOR/blocking cần xử lý hoặc chuyển Business Manager.
                    </p>
                </div>

                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200 text-sm">
                        <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                            <tr>
                                <th className="px-4 py-3">Product</th>
                                <th className="px-4 py-3">Expected</th>
                                <th className="px-4 py-3">Actual</th>
                                <th className="px-4 py-3">Result</th>
                                <th className="px-4 py-3">Severity</th>
                                <th className="px-4 py-3">Accepted</th>
                                <th className="px-4 py-3">Manager action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 bg-white">
                            {items.map((item, index) => {
                                const productId = item?.productId ?? item?.id ?? index;
                                const issue = hasIssue(item);
                                const report = issue ? getReportForItem(item) : null;
                                const resolved = String(report?.status ?? "").toUpperCase() === "RESOLVED";
                                const escalated = String(report?.responsibleParty ?? "").toUpperCase() === "BUSINESS_MANAGER";

                                return (
                                    <tr key={productId} className={issue ? "bg-red-50/30" : ""}>
                                        <td className="px-4 py-3">
                                            <p className="font-medium text-slate-800">{item?.productName ?? `Product #${productId}`}</p>
                                            <p className="mt-0.5 text-xs text-slate-400">#{productId}</p>
                                        </td>
                                        <td className="px-4 py-3">{number(item?.expectedQuantity)}</td>
                                        <td className="px-4 py-3">{number(item?.actualQuantity)}</td>
                                        <td className="px-4 py-3">{getInspectionResult(item)}</td>
                                        <td className="px-4 py-3">
                                            {issue ? (reportsLoading ? "Loading..." : <SeverityBadge report={report} />) : "-"}
                                        </td>
                                        <td className="px-4 py-3">
                                            {issue && !resolved && !escalated ? (
                                                <input
                                                    type="number"
                                                    min="0"
                                                    value={acceptedDrafts[productId] ?? item?.acceptedQuantity ?? ""}
                                                    onChange={(e) =>
                                                        setAcceptedDrafts((current) => ({ ...current, [productId]: e.target.value }))
                                                    }
                                                    className="w-24 rounded-lg border border-slate-200 px-2 py-1.5"
                                                />
                                            ) : (
                                                number(item?.acceptedQuantity)
                                            )}
                                        </td>
                                        <td className="min-w-[320px] px-4 py-3">
                                            {!issue ? (
                                                <span className="text-emerald-600">Matched</span>
                                            ) : resolved ? (
                                                <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-3 text-sm text-emerald-700">
                                                    ✓ Resolved {report?.resolutionNote ? `- ${report.resolutionNote}` : ""}
                                                </div>
                                            ) : escalated ? (
                                                <div className="rounded-xl border border-amber-100 bg-amber-50 p-3 text-sm text-amber-700">
                                                    Waiting for Business Manager
                                                </div>
                                            ) : report ? (
                                                <div className="space-y-2">
                                                    <textarea
                                                        rows={2}
                                                        placeholder="Resolution / escalation reason..."
                                                        value={reasonDrafts[productId] ?? ""}
                                                        onChange={(e) =>
                                                            setReasonDrafts((current) => ({ ...current, [productId]: e.target.value }))
                                                        }
                                                        className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                                                    />
                                                    <div className="flex flex-wrap gap-2">
                                                        <Button disabled={busy} onClick={() => resolveItem(item)}>Save accepted qty</Button>
                                                        <Button variant="outline" disabled={busy} onClick={() => escalateItem(item)}>Escalate Business</Button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <div className="rounded-xl border border-red-100 bg-red-50 p-3 text-xs text-red-700">
                                                    Discrepancy report missing for this product.
                                                </div>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </Card>

            {!readOnly && (
                <Card>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                            <h3 className="font-semibold text-slate-900">Manager decision - NCC</h3>
                            <p className="mt-1 text-sm text-slate-500">
                                Confirm khi không còn MAJOR/blocking chưa xử lý; nếu cần thì yêu cầu re-inspection.
                            </p>
                        </div>
                        <div className="flex flex-wrap gap-3">
                            <Button variant="outline" onClick={() => onRequestReinspection?.(data)} disabled={confirming}>
                                Request re-inspection
                            </Button>
                            <Button onClick={() => onConfirm?.(data)} disabled={confirming}>
                                {confirming ? "Confirming..." : "Confirm receiving"}
                            </Button>
                        </div>
                    </div>
                </Card>
            )}
        </div>
    );
}
