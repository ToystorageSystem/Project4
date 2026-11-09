import PutawayStatusBadge
    from "./PutawayStatusBadge";


function formatDate(value) {
    if (!value) {
        return "-";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }

    return date.toLocaleString(
        "en-GB",
        {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }
    );
}


function ProgressBar({
                         value = 0,
                     }) {
    const safeValue =
        Math.min(
            100,
            Math.max(
                0,
                Number(value) || 0
            )
        );


    return (
        <div className="min-w-[130px]">

            <div className="mb-1 flex items-center justify-between text-xs">

                <span className="text-slate-500">
                    Progress
                </span>

                <span className="font-semibold text-slate-700">
                    {safeValue.toFixed(0)}%
                </span>

            </div>


            <div className="h-2 overflow-hidden rounded-full bg-slate-100">

                <div
                    className="h-full rounded-full bg-[#f25d19] transition-all"
                    style={{
                        width:
                            `${safeValue}%`,
                    }}
                />

            </div>

        </div>
    );
}


export default function PutawayTaskTable({
                                             tasks = [],
                                             onView,
                                         }) {
    if (!tasks.length) {
        return (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">

                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">

                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        className="h-6 w-6"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M4 7h16v13H4V7Zm3-3h10l3 3H4l3-3"
                        />
                    </svg>

                </div>


                <h3 className="mt-4 text-sm font-semibold text-slate-800">
                    No putaway tasks
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                    There are no putaway tasks matching the current filter.
                </p>

            </div>
        );
    }


    return (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

            <div className="overflow-x-auto">

                <table className="min-w-full divide-y divide-slate-200">

                    <thead className="bg-slate-50">

                    <tr>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Task
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Status
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Assigned Staff
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Items
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Quantity
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Progress
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Created
                        </th>

                        <th className="px-5 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Action
                        </th>

                    </tr>

                    </thead>


                    <tbody className="divide-y divide-slate-100">

                    {tasks.map(
                        (task) => (
                            <tr
                                key={task.id}
                                className="transition hover:bg-[#fffaf7]"
                            >

                                <td className="px-5 py-4">

                                    <p className="font-semibold text-slate-900">
                                        {task.putawayCode ?? "-"}
                                    </p>

                                    <p className="mt-1 text-xs text-slate-400">
                                        Receipt #{task.goodsReceiptId ?? "-"}
                                    </p>

                                </td>


                                <td className="px-5 py-4">

                                    <PutawayStatusBadge
                                        status={task.status}
                                    />

                                </td>


                                <td className="px-5 py-4">

                                    <p className="text-sm font-medium text-slate-700">
                                        {task.assignedToName ?? "Not assigned"}
                                    </p>

                                    {task.assignedTo && (
                                        <p className="mt-1 text-xs text-slate-400">
                                            ID: {task.assignedTo}
                                        </p>
                                    )}

                                </td>


                                <td className="px-5 py-4 text-sm text-slate-600">

                                    {task.completedItems ?? 0}
                                    {" / "}
                                    {task.totalItems ?? 0}

                                </td>


                                <td className="px-5 py-4 text-sm text-slate-600">

                                    {task.putawayQuantity ?? 0}
                                    {" / "}
                                    {task.totalQuantity ?? 0}

                                </td>


                                <td className="px-5 py-4">

                                    <ProgressBar
                                        value={
                                            task.progressPercent
                                        }
                                    />

                                </td>


                                <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-500">

                                    {formatDate(
                                        task.createdAt
                                    )}

                                </td>


                                <td className="px-5 py-4 text-right">

                                    <button
                                        type="button"
                                        onClick={() =>
                                            onView?.(
                                                task
                                            )
                                        }
                                        className="rounded-xl border border-[#f25d19] px-3 py-2 text-xs font-semibold text-[#f25d19] transition hover:bg-[#f25d19] hover:text-white"
                                    >
                                        View
                                    </button>

                                </td>

                            </tr>
                        )
                    )}

                    </tbody>

                </table>

            </div>

        </div>
    );
}