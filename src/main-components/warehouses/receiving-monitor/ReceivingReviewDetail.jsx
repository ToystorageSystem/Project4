import {
    useEffect,
    useMemo,
    useState,
} from "react";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import StatusBadge from "../../../components/ui/StatusBadge";
import { getReceivingDiscrepancies, startHandlingDiscrepancy, updateAcceptedQuantity, escalateDiscrepancy } from "../../../api/inventories/discrepancy/receivingDiscrepancy";

const number = (value) => typeof value === "number" ? value : 0;
const formatDateTime = (value) => { if (!value) return "-"; const d = new Date(value); return Number.isNaN(d.getTime()) ? "-" : d.toLocaleString("en-GB"); };
const hasIssue = (item) => { const r = String(item?.inspectionResult ?? item?.inspectedResult ?? item?.result ?? "").toUpperCase(); return number(item?.damagedQuantity)>0 || number(item?.shortageQuantity)>0 || number(item?.surplusQuantity)>0 || ["DAMAGED","SHORTAGE","SURPLUS","PARTIAL","WRONG_PRODUCT","MISMATCH"].includes(r); };
const getInspectionResult = (item) => {

    const result =
        item?.inspectionResult ??
        item?.inspectedResult ??
        item?.result;

    if (result) {
        return String(result).toUpperCase();
    }

    if (number(item?.damagedQuantity) > 0) {
        return "DAMAGED";
    }

    if (number(item?.shortageQuantity) > 0) {
        return "SHORTAGE";
    }

    if (number(item?.surplusQuantity) > 0) {
        return "SURPLUS";
    }

    const expected =
        number(item?.expectedQuantity);

    const actual =
        number(item?.actualQuantity);

    if (actual < expected) {
        return "SHORTAGE";
    }

    if (actual > expected) {
        return "SURPLUS";
    }

    return "MATCHED";
};


const getIssueType = (item) => {

    return getInspectionResult(
        item
    );
};
const SummaryItem = ({label,value}) => <div className="rounded-xl border border-slate-100 bg-slate-50 p-4"><p className="text-xs text-slate-400">{label}</p><p className="mt-1 text-xl font-semibold text-slate-800">{value}</p></div>;

export default function ReceivingReviewDetail({ data, confirming=false, onBack, onConfirm, onRequestReinspection }) {
    if (!data) return null;
    const items = data?.products ?? data?.items ?? data?.inspectionItems ?? [];
    const status = String(data?.status ?? "INSPECTED").toUpperCase();
    const readOnly = status === "COMPLETED";
    const issues = useMemo(() => items.filter(hasIssue), [items]);
    const [acceptedDrafts, setAcceptedDrafts] = useState({}), [reasonDrafts, setReasonDrafts] = useState({}), [busy, setBusy] = useState(false), [actionError, setActionError] = useState(""), [actionSuccess, setActionSuccess] = useState("");
    const totalProducts = data?.totalProducts ?? items.length;
    const expected = data?.totalExpectedQuantity ?? items.reduce((s, i) => s + number(i?.expectedQuantity), 0);
    const actual = data?.totalActualQuantity ?? items.reduce((s, i) => s + number(i?.actualQuantity), 0);
    const damaged = data?.totalDamagedQuantity ?? items.reduce((s, i) => s + number(i?.damagedQuantity), 0);
    const shortage = data?.totalShortageQuantity ?? items.reduce((s, i) => s + number(i?.shortageQuantity), 0);
    const [reports, setReports] =
        useState([]);

    const [reportsLoading, setReportsLoading] =
        useState(false);

    const getReportForItem =
        (item) => {

            const issueType =
                getIssueType(item);

            return (
                reports.find(
                    (report) =>
                        String(
                            report?.discrepancyType ??
                            ""
                        ).toUpperCase() ===
                        issueType
                ) ??
                null
            );
        };

    const resolveItem =
        async (item) => {

            const productId =
                item?.productId ??
                item?.id;

            const reason =
                String(
                    reasonDrafts[
                        productId
                        ] ?? ""
                ).trim();

            const accepted =
                Number(
                    acceptedDrafts[
                        productId
                        ] ??
                    item?.acceptedQuantity ??
                    0
                );

            if (!reason) {

                setActionError(
                    "Resolution reason is required."
                );

                return;
            }

            if (
                !Number.isFinite(
                    accepted
                ) ||
                accepted < 0
            ) {

                setActionError(
                    "Accepted quantity is invalid."
                );

                return;
            }

            const report =
                getReportForItem(
                    item
                );

            if (!report?.id) {

                setActionError(
                    `No ${getIssueType(item)} discrepancy report was found for this product.`
                );

                return;
            }

            if (
                report.status ===
                "RESOLVED"
            ) {

                setActionError(
                    "This discrepancy has already been resolved."
                );

                return;
            }

            if (
                report.responsibleParty ===
                "BUSINESS_MANAGER"
            ) {

                setActionError(
                    "This discrepancy has already been escalated to Business Manager."
                );

                return;
            }

            try {

                setBusy(true);
                setActionError("");
                setActionSuccess("");

                if (
                    report.status ===
                    "OPEN"
                ) {

                    await startHandlingDiscrepancy(
                        report.id
                    );
                }

                const result =
                    await updateAcceptedQuantity(
                        report.id,
                        productId,
                        accepted,
                        reason
                    );

                /*
                 * Update UI ngay lập tức.
                 * Không cần reload cả page.
                 */
                setReports(
                    (current) =>
                        current.map(
                            (currentReport) =>
                                currentReport.id ===
                                report.id
                                    ? {
                                        ...currentReport,
                                        ...result,

                                        status:
                                            "RESOLVED",

                                        resolutionNote:
                                        reason,
                                    }
                                    : currentReport
                        )
                );

                setActionSuccess(
                    "Accepted quantity updated and discrepancy resolved."
                );

            } catch (e) {

                setActionError(
                    e.response?.data?.message ??
                    e.message ??
                    "Unable to resolve discrepancy."
                );

            } finally {

                setBusy(false);
            }
        };

    const escalateItem =
        async (item) => {

            const productId =
                item?.productId ??
                item?.id;

            const reason =
                String(
                    reasonDrafts[
                        productId
                        ] ?? ""
                ).trim();

            if (!reason) {

                setActionError(
                    "Escalation reason is required."
                );

                return;
            }

            const report =
                getReportForItem(
                    item
                );

            if (!report?.id) {

                setActionError(
                    `No ${getIssueType(item)} discrepancy report was found for this product.`
                );

                return;
            }

            if (
                report.status ===
                "RESOLVED"
            ) {

                setActionError(
                    "Resolved discrepancy cannot be escalated."
                );

                return;
            }

            if (
                report.responsibleParty ===
                "BUSINESS_MANAGER"
            ) {

                setActionError(
                    "This discrepancy has already been escalated."
                );

                return;
            }

            try {

                setBusy(true);
                setActionError("");
                setActionSuccess("");

                const result =
                    await escalateDiscrepancy(
                        report.id,
                        reason
                    );

                setReports(
                    (current) =>
                        current.map(
                            (currentReport) =>
                                currentReport.id ===
                                report.id
                                    ? {
                                        ...currentReport,
                                        ...result,

                                        status:
                                            "INVESTIGATING",

                                        responsibleParty:
                                            "BUSINESS_MANAGER",

                                        resolutionNote:
                                        reason,
                                    }
                                    : currentReport
                        )
                );

                setActionSuccess(
                    "Discrepancy escalated to Business Manager."
                );

            } catch (e) {

                setActionError(
                    e.response?.data?.message ??
                    e.message ??
                    "Unable to escalate discrepancy."
                );

            } finally {

                setBusy(false);
            }
        };
    useEffect(() => {

        if (!data) {
            return;
        }

        const loadReports =
            async () => {

                try {

                    setReportsLoading(
                        true
                    );

                    const response =
                        await getReceivingDiscrepancies();

                    const receiptId =
                        data?.receiptId ??
                        data?.id;

                    const receiptReports =
                        (
                            Array.isArray(response)
                                ? response
                                : []
                        ).filter(
                            (report) =>
                                Number(
                                    report?.goodsReceiptId
                                ) ===
                                Number(
                                    receiptId
                                )
                        );

                    setReports(
                        receiptReports
                    );

                } catch (e) {

                    console.error(
                        "Unable to load discrepancy reports",
                        e
                    );

                    setReports([]);

                } finally {

                    setReportsLoading(
                        false
                    );
                }
            };

        loadReports();

    }, [
        data?.receiptId,
        data?.id,
    ]);
    return <div className="space-y-5">
        <Card className="overflow-hidden p-0">
            <div className="h-1 bg-[#f25d19]"/>
            <div
                className="flex flex-col gap-4 border-b border-slate-100 p-5 sm:flex-row sm:items-center sm:justify-between">
                <div><p
                    className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">{readOnly ? "Receiving History" : "Receiving Review"}</p>
                    <h2 className="mt-1 text-xl font-semibold text-slate-900">{data?.receiptCode ?? `Receipt #${data?.receiptId ?? data?.id ?? "-"}`}</h2>
                </div>
                <div className="flex items-center gap-2"><StatusBadge status={status}/><Button variant="ghost"
                                                                                               onClick={onBack}>Back</Button>
                </div>
            </div>
            <div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-6"><SummaryItem label="Products"
                                                                                       value={totalProducts}/><SummaryItem
                label="Expected Qty" value={expected}/><SummaryItem label="Actual Qty" value={actual}/><SummaryItem
                label="Damaged Qty" value={damaged}/><SummaryItem label="Shortage Qty" value={shortage}/><SummaryItem
                label="Issues" value={issues.length}/></div>
        </Card>
        <Card>
            <div className="grid gap-4 lg:grid-cols-3">
                <div className="rounded-2xl border border-orange-100 bg-[#fff8f4] p-5"><p
                    className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">Warehouse Staff</p><p
                    className="mt-3 text-lg font-semibold text-slate-900">{data?.staffName ?? data?.receivedByName ?? "-"}</p>
                    <p className="mt-1 text-sm text-slate-500">Staff
                        ID: {data?.staffId ?? data?.receivedById ?? "-"}</p></div>
                <div className="rounded-2xl border border-slate-100 bg-white p-5"><p
                    className="text-xs text-slate-400">Inspection Started</p><p
                    className="mt-2 font-semibold text-slate-800">{formatDateTime(data?.receivingStartedAt ?? data?.receivedAt)}</p>
                </div>
                <div className="rounded-2xl border border-slate-100 bg-white p-5"><p
                    className="text-xs text-slate-400">Inspection Completed</p><p
                    className="mt-2 font-semibold text-slate-800">{formatDateTime(data?.inspectionCompletedAt ?? data?.inspectedAt)}</p>
                </div>
            </div>
        </Card>
        {actionError && <div
            className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{actionError}</div>}{actionSuccess &&
        <div
            className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{actionSuccess}</div>}
        <Card className="overflow-hidden p-0">
            <div className="border-b border-slate-100 p-5"><h3 className="font-semibold text-slate-900">Inspection
                Result</h3><p className="mt-1 text-sm text-slate-500">Actual quantity remains the Staff inspection
                result. Warehouse Manager may change Accepted quantity with a required reason.</p></div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                    <tr>
                        <th className="px-4 py-3">Product</th>
                        <th className="px-4 py-3">Expected</th>
                        <th className="px-4 py-3">Actual</th>
                        <th className="px-4 py-3">Accepted</th>
                        <th className="px-4 py-3">Damaged</th>
                        <th className="px-4 py-3">Shortage</th>
                        <th className="px-4 py-3">Surplus</th>
                        <th className="px-4 py-3">Result</th>
                        {!readOnly && <th className="px-4 py-3">Resolution</th>}</tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">{items.map((item, index) => {
                        const issue =
                            hasIssue(item);

                        const productId =
                            item?.productId ??
                            item?.id ??
                            index;

                        const result =
                            getInspectionResult(
                                item
                            );

                        const report =
                            getReportForItem(
                                item
                            );

                        const resolved =
                            report?.status ===
                            "RESOLVED";

                        const escalated =
                            report?.responsibleParty ===
                            "BUSINESS_MANAGER";

                        const actionable =
                            issue &&
                            !resolved &&
                            !escalated;
                        return <tr key={productId} className={issue ? "bg-red-50/50" : ""}>
                            <td className="px-4 py-3"><p
                                className="font-medium text-slate-800">{item?.productName ?? item?.name ?? `Product #${productId}`}</p>
                                <p className="mt-0.5 text-xs text-slate-400">{item?.productCode ?? item?.productsCode ?? "-"}</p>
                            </td>
                            <td className="px-4 py-3">{number(item?.expectedQuantity)}</td>
                            <td className="px-4 py-3 font-medium">{number(item?.actualQuantity)}</td>
                            <td className="px-4 py-3">

                                {!readOnly && actionable ? (

                                    <input
                                        type="number"
                                        min="0"

                                        value={
                                            acceptedDrafts[
                                                productId
                                                ] ??
                                            number(
                                                item?.acceptedQuantity
                                            )
                                        }

                                        onChange={(e) =>
                                            setAcceptedDrafts(
                                                (current) => ({
                                                    ...current,

                                                    [productId]:
                                                    e.target.value,
                                                })
                                            )
                                        }

                                        className="w-24 rounded-lg border border-slate-200 px-2 py-1.5 outline-none focus:border-[#f25d19]"
                                    />

                                ) : (

                                    number(
                                        item?.acceptedQuantity
                                    )

                                )}

                            </td>
                            <td className="px-4 py-3">{number(item?.damagedQuantity)}</td>
                            <td className="px-4 py-3">{number(item?.shortageQuantity)}</td>
                            <td className="px-4 py-3">{number(item?.surplusQuantity)}</td>
                            <td className="px-4 py-3"><span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${issue ? "bg-red-100 text-red-700" : "bg-emerald-100 text-emerald-700"}`}>{result}</span>
                            </td>
                            {!readOnly && (

                                <td className="min-w-80 px-4 py-3">

                                    {!issue ? (

                                        <span className="text-xs font-medium text-emerald-600">
                No action required
            </span>

                                    ) : reportsLoading ? (

                                        <span className="text-xs text-slate-400">
                Loading resolution...
            </span>

                                    ) : resolved ? (

                                        <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-3">

                                            <p className="text-sm font-semibold text-emerald-700">
                                                ✓ Resolved
                                            </p>

                                            {report?.resolutionNote && (

                                                <p className="mt-1 text-xs leading-5 text-slate-500">
                                                    {report.resolutionNote}
                                                </p>

                                            )}

                                            {report?.resolvedByName && (

                                                <p className="mt-2 text-xs text-slate-400">
                                                    Resolved by{" "}
                                                    {report.resolvedByName}
                                                </p>

                                            )}

                                        </div>

                                    ) : escalated ? (

                                        <div className="rounded-xl border border-amber-100 bg-amber-50 p-3">

                                            <p className="text-sm font-semibold text-amber-700">
                                                Waiting for Business Manager
                                            </p>

                                            {report?.resolutionNote && (

                                                <p className="mt-1 text-xs leading-5 text-slate-500">
                                                    {report.resolutionNote}
                                                </p>

                                            )}

                                        </div>

                                    ) : report ? (

                                        <div className="space-y-2">

                <textarea
                    rows={2}

                    placeholder="Resolution / escalation reason..."

                    value={
                        reasonDrafts[
                            productId
                            ] ?? ""
                    }

                    onChange={(e) =>
                        setReasonDrafts(
                            (current) => ({
                                ...current,

                                [productId]:
                                e.target.value,
                            })
                        )
                    }

                    className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-[#f25d19]"
                />

                                            <div className="flex flex-wrap gap-2">

                                                <Button
                                                    disabled={busy}

                                                    onClick={() =>
                                                        resolveItem(
                                                            item
                                                        )
                                                    }
                                                >
                                                    Save accepted qty
                                                </Button>

                                                <Button
                                                    variant="outline"

                                                    disabled={busy}

                                                    onClick={() =>
                                                        escalateItem(
                                                            item
                                                        )
                                                    }
                                                >
                                                    Escalate Business
                                                </Button>

                                            </div>

                                        </div>

                                    ) : (

                                        <div className="rounded-xl border border-red-100 bg-red-50 p-3">

                                            <p className="text-xs font-semibold text-red-700">
                                                Discrepancy report missing
                                            </p>

                                            <p className="mt-1 text-xs text-slate-500">
                                                No {getIssueType(item)} report was found for this product.
                                            </p>

                                        </div>

                                    )}

                                </td>

                            )}
                        </tr>;
                    })}</tbody>
                </table>
            </div>
        </Card>

        {!readOnly && <Card>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div><h3 className="font-semibold text-slate-900">Manager decision</h3><p
                    className="mt-1 text-sm text-slate-500">Confirm only after all discrepancies are resolved, or
                    request another inspection attempt.</p></div>
                <div className="flex flex-wrap gap-3"><Button variant="outline"
                                                              onClick={() => onRequestReinspection?.(data)}
                                                              disabled={confirming}>Request
                    re-inspection</Button><Button onClick={() => onConfirm?.(data)}
                                                  disabled={confirming}>{confirming ? "Confirming..." : "Confirm receiving"}</Button>
                </div>
            </div>
        </Card>}
    </div>;

}
