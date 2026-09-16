import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";

import ReceivingIncidentReportPagination
    from "./ReceivingIncidentReportPagination.jsx";


const formatDateTime =
    (value) => {

        if (!value) {
            return "-";
        }

        const date =
            new Date(value);

        return Number.isNaN(
            date.getTime()
        )
            ? "-"
            : date.toLocaleString(
                "en-GB"
            );
    };


export default function ReceivingIncidentReportList({
                                                        items = [],
                                                        page = 0,
                                                        totalPages = 0,
                                                        totalElements = 0,
                                                        onView,
                                                        onPrevious,
                                                        onNext,
                                                    }) {
    return (
        <div className="flex min-h-0 flex-1 flex-col">

            <div className="flex-1">

                {!items.length ? (

                    <EmptyState
                        title="No receiving incident reports"
                        description="No Warehouse Accept incident report has been generated yet."
                    />

                ) : (

                    <div className="grid gap-4 xl:grid-cols-2">

                        {items.map(
                            (item) => (

                                <Card
                                    key={
                                        item.id
                                    }
                                    className="overflow-hidden p-0 transition hover:-translate-y-0.5 hover:shadow-lg"
                                >

                                    <div className="h-1 bg-[#f25d19]" />


                                    <div className="space-y-4 p-5">

                                        <div className="flex items-start justify-between gap-3">

                                            <div>

                                                <div className="flex items-center gap-2">

                                                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#fff1e9] text-[#f25d19]">

                                                        <svg
                                                            viewBox="0 0 24 24"
                                                            fill="none"
                                                            stroke="currentColor"
                                                            className="h-4 w-4"
                                                        >
                                                            <path
                                                                strokeLinecap="round"
                                                                strokeLinejoin="round"
                                                                strokeWidth="2"
                                                                d="M12 3 2 21h20L12 3ZM12 9v5M12 18h.01"
                                                            />
                                                        </svg>

                                                    </div>

                                                    <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                                                        Receiving Incident Report
                                                    </p>

                                                </div>


                                                <h3 className="mt-3 text-lg font-semibold text-slate-900">
                                                    {item.reportCode}
                                                </h3>


                                                <p className="mt-1 text-sm text-slate-500">
                                                    Receipt:{" "}
                                                    {item.receiptCode}
                                                </p>

                                            </div>

                                        </div>


                                        <div className="grid gap-3 sm:grid-cols-3">

                                            <div className="rounded-xl bg-slate-50 p-3">

                                                <p className="text-xs text-slate-400">
                                                    Issue products
                                                </p>

                                                <p className="mt-1 text-lg font-semibold text-slate-800">
                                                    {item.totalIssueProducts ?? 0}
                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-slate-50 p-3">

                                                <p className="text-xs text-slate-400">
                                                    Warehouse Staff
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                                    {item.warehouseStaffName ?? "-"}
                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-slate-50 p-3">

                                                <p className="text-xs text-slate-400">
                                                    Warehouse Manager
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-800">
                                                    {item.warehouseManagerName ?? "-"}
                                                </p>

                                            </div>

                                        </div>


                                        <div className="rounded-xl border border-orange-100 bg-[#fff8f4] p-3">

                                            <div className="flex items-center gap-2">

                                                <svg
                                                    viewBox="0 0 24 24"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    className="h-4 w-4 text-[#f25d19]"
                                                >
                                                    <path
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth="2"
                                                        d="M4 6h16M4 12h16M4 18h10"
                                                    />
                                                </svg>

                                                <p className="text-xs font-medium text-[#c94810]">
                                                    Reasons
                                                </p>

                                            </div>


                                            <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-600">

                                                {(item.items ?? []).map(
                                                    (detail) => (

                                                        <li
                                                            key={
                                                                detail.id
                                                            }
                                                        >
                                                            {detail.productName}:{" "}
                                                            {detail.reason}
                                                        </li>

                                                    )
                                                )}

                                            </ul>

                                        </div>


                                        <div className="flex items-center justify-between border-t border-slate-100 pt-4">

                                            <div className="flex items-center gap-2 text-xs text-slate-400">

                                                <svg
                                                    viewBox="0 0 24 24"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    className="h-4 w-4"
                                                >
                                                    <circle
                                                        cx="12"
                                                        cy="12"
                                                        r="9"
                                                        strokeWidth="2"
                                                    />

                                                    <path
                                                        strokeLinecap="round"
                                                        strokeWidth="2"
                                                        d="M12 7v5l3 2"
                                                    />
                                                </svg>

                                                {formatDateTime(
                                                    item.createdAt
                                                )}

                                            </div>


                                            <Button
                                                onClick={() =>
                                                    onView?.(
                                                        item
                                                    )
                                                }
                                            >
                                                View / Edit
                                            </Button>

                                        </div>

                                    </div>

                                </Card>

                            )
                        )}

                    </div>

                )}

            </div>


            <div className="mt-auto pt-6">

                <ReceivingIncidentReportPagination
                    page={page}
                    totalPages={
                        totalPages
                    }
                    totalElements={
                        totalElements
                    }
                    onPrevious={
                        onPrevious
                    }
                    onNext={
                        onNext
                    }
                />

            </div>

        </div>
    );
}