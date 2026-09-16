import {
    useEffect,
    useMemo,
    useState,
} from "react";

import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import StatusBadge from "../../../components/ui/StatusBadge";

const number = (value) =>
        typeof value === "number"
                ? value
                : 0;


function formatDateTime(value) {

    if (!value) {
        return "-";
    }

    const date =
            new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    return date.toLocaleString(
            "en-GB",
            {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
            }
    );
}


function formatDuration(
        startedAt,
        completedAt,
        now
) {

    if (!startedAt) {
        return "-";
    }

    const start =
            new Date(startedAt);

    const end =
            completedAt
                    ? new Date(completedAt)
                    : now;

    if (
        Number.isNaN(start.getTime()) ||
        Number.isNaN(end.getTime())
    ) {
        return "-";
    }

    const totalSeconds =
            Math.max(
                    0,
                    Math.floor(
                            (
                                end.getTime() -
                                start.getTime()
                            ) / 1000
                    )
            );

    const hours =
            Math.floor(
                    totalSeconds / 3600
            );

    const minutes =
            Math.floor(
                    (totalSeconds % 3600) / 60
            );

    const seconds =
            totalSeconds % 60;

    if (hours > 0) {
        return `${hours}h ${minutes}m ${seconds}s`;
    }

    if (minutes > 0) {
        return `${minutes}m ${seconds}s`;
    }

    return `${seconds}s`;
}


function hasIssue(item) {

    if (!item?.inspected) {
        return false;
    }

    const result =
            String(
                    item?.inspectionResult ??
                    item?.result ??
                    ""
            ).toUpperCase();

    return (
        number(
                item?.damagedQuantity
        ) > 0 ||
        number(
                item?.shortageQuantity
        ) > 0 ||
        number(
                item?.surplusQuantity
        ) > 0 ||
        [
            "DAMAGED",
            "SHORTAGE",
            "SURPLUS",
            "PARTIAL",
            "WRONG_PRODUCT",
            "MISMATCH",
        ].includes(result)
    );
}


function SummaryItem({
    label,
    value,
}) {

    return (
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">

            <p className="text-xs text-slate-400">
                {label}
            </p>

            <p className="mt-1 text-xl font-semibold text-slate-800">
                {value}
            </p>

        </div>
    );
}


export default function ReceivingProgressDetail({
    data,
    onBack,
}) {

    const [now, setNow] =
            useState(
                    () => new Date()
            );


    /*
     * Duration is calculated locally every second.
     *
     * No API call.
     * No browser reload.
     */
    useEffect(() => {

        if (
            !data?.receivingStartedAt ||
            data?.inspectionCompletedAt
        ) {
            return undefined;
        }

        const timer =
                window.setInterval(
                        () => {
                            setNow(
                                    new Date()
                            );
                        },
                        1000
                );

        return () => {
            window.clearInterval(
                    timer
            );
        };

    }, [
        data?.receivingStartedAt,
        data?.inspectionCompletedAt,
    ]);


    const products =
            data?.products ??
            data?.items ??
            [];


    const issues =
            useMemo(
                    () =>
                        products.filter(
                                hasIssue
                        ),
                    [products]
            );


    if (!data) {
        return null;
    }


    const expected =
            number(
                    data.totalExpectedQuantity
            );

    const actual =
            number(
                    data.totalActualQuantity
            );

    const totalProducts =
            number(
                    data.totalProducts
            );

    const inspectedProducts =
            number(
                    data.inspectedProducts
            );

    const remainingProducts =
            typeof data.remainingProducts ===
            "number"
                    ? data.remainingProducts
                    : Math.max(
                            totalProducts -
                            inspectedProducts,
                            0
                    );


    const percent =
            typeof data.progressPercent ===
            "number"
                    ? Math.min(
                            100,
                            Math.max(
                                    0,
                                    Math.round(
                                            data.progressPercent
                                    )
                            )
                    )
                    : totalProducts > 0
                        ? Math.round(
                                (
                                    inspectedProducts /
                                    totalProducts
                                ) * 100
                        )
                        : 0;


    const startedAt =
            data.receivingStartedAt;

    const completedAt =
            data.inspectionCompletedAt;


    return (
        <div className="space-y-5">

            <Card className="overflow-hidden p-0">

                <div className="h-1 bg-[#f25d19]" />

                <div className="flex items-center justify-between border-b border-slate-100 p-5">

                    <div>

                        <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                            Live Receiving Inspection
                        </p>

                        <h2 className="mt-1 text-xl font-semibold text-slate-900">
                            {data.receiptCode ??
                                `Receipt #${data.receiptId ?? "-"}`}
                        </h2>

                    </div>


                    <div className="flex items-center gap-2">

                        <span className="flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
                            <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-500" />
                            Live
                        </span>

                        <StatusBadge
                            status={data.status}
                        />

                        <Button
                            variant="ghost"
                            onClick={onBack}
                        >
                            Back
                        </Button>

                    </div>

                </div>


                <div className="space-y-5 p-5">

                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">

                        <SummaryItem
                            label="Expected Qty"
                            value={expected}
                        />

                        <SummaryItem
                            label="Actual Qty"
                            value={actual}
                        />

                        <SummaryItem
                            label="Remaining Qty"
                            value={
                                Math.max(
                                        expected -
                                        actual,
                                        0
                                )
                            }
                        />

                        <SummaryItem
                            label="Damaged Qty"
                            value={
                                number(
                                        data.totalDamagedQuantity
                                )
                            }
                        />

                        <SummaryItem
                            label="Issues"
                            value={
                                issues.length
                            }
                        />

                    </div>


                    <div>

                        <div className="mb-2 flex items-center justify-between text-sm">

                            <span className="font-medium text-slate-700">

                                Inspection progress

                                <span className="ml-2 text-xs text-slate-400">
                                    {inspectedProducts}
                                    /
                                    {totalProducts}
                                    {" "}products
                                </span>

                            </span>

                            <span className="font-semibold text-[#d94f12]">
                                {percent}%
                            </span>

                        </div>


                        <div className="h-3 overflow-hidden rounded-full bg-slate-100">

                            <div
                                className="h-full rounded-full bg-[#f25d19] transition-all duration-300"
                                style={{
                                    width: `${percent}%`,
                                }}
                            />

                        </div>

                    </div>

                </div>

            </Card>


            <Card>

                <div className="grid gap-5 lg:grid-cols-3">

                    <div className="rounded-2xl border border-orange-100 bg-[#fff8f4] p-5">

                        <p className="text-xs font-semibold uppercase tracking-wide text-[#f25d19]">
                            Warehouse Staff
                        </p>

                        <p className="mt-3 text-lg font-semibold text-slate-900">
                            {data.staffName ??
                                "Not assigned"}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                            Staff ID:{" "}
                            {data.staffId ?? "-"}
                        </p>

                    </div>


                    <div className="rounded-2xl border border-slate-100 bg-white p-5">

                        <p className="text-xs text-slate-400">
                            Inspection Started
                        </p>

                        <p className="mt-2 font-semibold text-slate-800">
                            {formatDateTime(
                                    startedAt
                            )}
                        </p>

                    </div>


                    <div className="rounded-2xl border border-slate-100 bg-white p-5">

                        <p className="text-xs text-slate-400">
                            Inspection Completed
                        </p>

                        <p className="mt-2 font-semibold text-slate-800">
                            {completedAt
                                    ? formatDateTime(
                                            completedAt
                                    )
                                    : "In progress"}
                        </p>


                        <p className="mt-3 text-xs text-slate-400">
                            Duration
                        </p>

                        <p className="mt-1 font-medium text-[#d94f12]">
                            {formatDuration(
                                    startedAt,
                                    completedAt,
                                    now
                            )}
                        </p>

                    </div>

                </div>

            </Card>


            <Card className="overflow-hidden p-0">

                <div className="flex items-center justify-between border-b border-slate-100 p-5">

                    <div>

                        <h3 className="font-semibold text-slate-900">
                            Product Inspection Progress
                        </h3>

                        <p className="mt-1 text-sm text-slate-500">
                            Updates automatically when Warehouse Staff saves inspection results.
                        </p>

                    </div>

                    <div className="flex gap-2 text-xs">

                        <span className="rounded-full bg-emerald-100 px-3 py-1.5 font-medium text-emerald-700">
                            Inspected {inspectedProducts}
                        </span>

                        <span className="rounded-full bg-slate-100 px-3 py-1.5 font-medium text-slate-600">
                            Remaining {remainingProducts}
                        </span>

                    </div>

                </div>


                <div className="overflow-x-auto">

                    <table className="min-w-full divide-y divide-slate-200 text-sm">

                        <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">

                            <tr>
                                <th className="px-4 py-3">Product</th>
                                <th className="px-4 py-3">Barcode</th>
                                <th className="px-4 py-3">Expected</th>
                                <th className="px-4 py-3">Actual</th>
                                <th className="px-4 py-3">Accepted</th>
                                <th className="px-4 py-3">Damaged</th>
                                <th className="px-4 py-3">Shortage</th>
                                <th className="px-4 py-3">Surplus</th>
                                <th className="px-4 py-3">Result</th>
                                <th className="px-4 py-3">Inspected At</th>
                            </tr>

                        </thead>


                        <tbody className="divide-y divide-slate-100 bg-white">

                            {products.map(
                                    (
                                        item,
                                        index
                                    ) => {

                                const issue =
                                        hasIssue(
                                                item
                                        );

                                return (
                                    <tr
                                        key={
                                            item.productId ??
                                            index
                                        }
                                        className={
                                            issue
                                                    ? "bg-red-50/60"
                                                    : ""
                                        }
                                    >

                                        <td className="px-4 py-3">

                                            <p className="font-medium text-slate-800">
                                                {item.productName ??
                                                    `Product #${item.productId ?? "-"}`}
                                            </p>

                                            <p className="mt-0.5 text-xs text-slate-400">
                                                {item.productCode ??
                                                    "-"}
                                            </p>

                                        </td>

                                        <td className="px-4 py-3">
                                            {item.barcode ??
                                                "-"}
                                        </td>

                                        <td className="px-4 py-3">
                                            {number(
                                                    item.expectedQuantity
                                            )}
                                        </td>

                                        <td className="px-4 py-3">
                                            {item.inspected
                                                    ? number(
                                                            item.actualQuantity
                                                    )
                                                    : "-"}
                                        </td>

                                        <td className="px-4 py-3">
                                            {item.inspected
                                                    ? number(
                                                            item.acceptedQuantity
                                                    )
                                                    : "-"}
                                        </td>

                                        <td className="px-4 py-3">
                                            {item.inspected
                                                    ? number(
                                                            item.damagedQuantity
                                                    )
                                                    : "-"}
                                        </td>

                                        <td className="px-4 py-3">
                                            {item.inspected
                                                    ? number(
                                                            item.shortageQuantity
                                                    )
                                                    : "-"}
                                        </td>

                                        <td className="px-4 py-3">
                                            {item.inspected
                                                    ? number(
                                                            item.surplusQuantity
                                                    )
                                                    : "-"}
                                        </td>

                                        <td className="px-4 py-3">

                                            <span
                                                className={
                                                    "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold " +
                                                    (
                                                        !item.inspected
                                                                ? "bg-slate-100 text-slate-500"
                                                                : issue
                                                                    ? "bg-red-100 text-red-700"
                                                                    : "bg-emerald-100 text-emerald-700"
                                                    )
                                                }
                                            >
                                                {item.inspected
                                                        ? item.inspectionResult ??
                                                          "MATCHED"
                                                        : "PENDING"}
                                            </span>

                                        </td>

                                        <td className="whitespace-nowrap px-4 py-3">
                                            {item.inspectedAt
                                                    ? formatDateTime(
                                                            item.inspectedAt
                                                    )
                                                    : "-"}
                                        </td>

                                    </tr>
                                );
                            })}


                            {!products.length && (

                                <tr>

                                    <td
                                        colSpan="10"
                                        className="px-4 py-10 text-center text-slate-400"
                                    >
                                        No product inspection data yet.
                                    </td>

                                </tr>
                            )}

                        </tbody>

                    </table>

                </div>

            </Card>




        </div>
    );
}
