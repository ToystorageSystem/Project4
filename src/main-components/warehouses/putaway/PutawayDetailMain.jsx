import {
    useCallback,
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
} from "react-router-dom";

import {
    getPutawayPlan,
} from "../../../api/warehouses/putaway/putawayPlanning.js";

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


function InfoItem({
                      label,
                      value,
                  }) {
    return (
        <div>

            <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                {label}
            </p>

            <p className="mt-1 text-sm font-semibold text-slate-800">
                {value ?? "-"}
            </p>

        </div>
    );
}


function getErrorMessage(error) {
    const data =
        error?.response?.data;

    if (typeof data === "string") {
        return data;
    }

    return (
        data?.message ??
        error?.message ??
        "Unable to load putaway task."
    );
}


export default function PutawayDetailMain() {
    const navigate =
        useNavigate();

    const {
        taskId,
    } = useParams();


    const [
        task,
        setTask,
    ] = useState(null);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        error,
        setError,
    ] = useState("");


    const loadTask =
        useCallback(
            async () => {

                try {

                    setLoading(true);
                    setError("");


                    const data =
                        await getPutawayPlan(
                            taskId
                        );


                    setTask(data);

                } catch (err) {

                    console.error(
                        "Load putaway detail error:",
                        err
                    );

                    setError(
                        getErrorMessage(err)
                    );

                } finally {

                    setLoading(false);

                }
            },
            [taskId]
        );


    useEffect(() => {
        loadTask();
    }, [loadTask]);


    if (loading) {
        return (
            <div className="flex min-h-[400px] items-center justify-center">

                <div className="text-center">

                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-[#f25d19]" />

                    <p className="mt-3 text-sm text-slate-500">
                        Loading putaway task...
                    </p>

                </div>

            </div>
        );
    }


    if (error) {
        return (
            <div className="space-y-4">

                <button
                    type="button"
                    onClick={() =>
                        navigate(
                            "/warehouse/putaway"
                        )
                    }
                    className="text-sm font-semibold text-[#f25d19]"
                >
                    ← Back to Putaway
                </button>


                <div className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700">
                    {error}
                </div>

            </div>
        );
    }


    if (!task) {
        return null;
    }


    const progress =
        Math.min(
            100,
            Math.max(
                0,
                Number(
                    task.progressPercent
                ) || 0
            )
        );


    const items =
        Array.isArray(task.items)
            ? task.items
            : [];


    return (
        <div className="space-y-6">

            {/* BACK */}

            <button
                type="button"
                onClick={() =>
                    navigate(
                        "/warehouse/putaway"
                    )
                }
                className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-[#f25d19]"
            >
                <span>←</span>
                Back to Putaway
            </button>


            {/* HEADER */}

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

                <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">

                    <div>

                        <div className="flex flex-wrap items-center gap-3">

                            <h1 className="text-2xl font-bold text-slate-900">
                                {task.putawayCode}
                            </h1>

                            <PutawayStatusBadge
                                status={task.status}
                            />

                        </div>


                        <p className="mt-2 text-sm text-slate-500">
                            Putaway task monitoring and storage progress.
                        </p>

                    </div>


                    <button
                        type="button"
                        onClick={loadTask}
                        className="inline-flex h-10 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-600 transition hover:border-[#f25d19] hover:text-[#f25d19]"
                    >
                        Refresh
                    </button>

                </div>


                <div className="mt-6 grid gap-5 border-t border-slate-100 pt-6 sm:grid-cols-2 xl:grid-cols-4">

                    <InfoItem
                        label="Goods Receipt"
                        value={
                            task.goodsReceiptId
                                ? `#${task.goodsReceiptId}`
                                : "-"
                        }
                    />

                    <InfoItem
                        label="Warehouse"
                        value={
                            task.warehouseId
                                ? `Warehouse #${task.warehouseId}`
                                : "-"
                        }
                    />

                    <InfoItem
                        label="Assigned Staff"
                        value={
                            task.assignedToName ??
                            "Not assigned"
                        }
                    />

                    <InfoItem
                        label="Created By"
                        value={
                            task.createdByName ??
                            "-"
                        }
                    />

                    <InfoItem
                        label="Created At"
                        value={
                            formatDate(
                                task.createdAt
                            )
                        }
                    />

                    <InfoItem
                        label="Completed At"
                        value={
                            formatDate(
                                task.completedAt
                            )
                        }
                    />

                    <InfoItem
                        label="Items"
                        value={
                            `${task.completedItems ?? 0} / ${task.totalItems ?? 0}`
                        }
                    />

                    <InfoItem
                        label="Quantity"
                        value={
                            `${task.putawayQuantity ?? 0} / ${task.totalQuantity ?? 0}`
                        }
                    />

                </div>

            </div>


            {/* PROGRESS */}

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

                <div className="flex items-center justify-between">

                    <div>

                        <h2 className="text-base font-bold text-slate-900">
                            Overall Progress
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Current putaway completion progress.
                        </p>

                    </div>


                    <p className="text-2xl font-bold text-[#f25d19]">
                        {progress.toFixed(0)}%
                    </p>

                </div>


                <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-100">

                    <div
                        className="h-full rounded-full bg-[#f25d19] transition-all"
                        style={{
                            width:
                                `${progress}%`,
                        }}
                    />

                </div>

            </div>


            {/* ITEMS */}

            <div>

                <div className="mb-4">

                    <h2 className="text-lg font-bold text-slate-900">
                        Putaway Items
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        Products and destination storage information.
                    </p>

                </div>


                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

                    <div className="overflow-x-auto">

                        <table className="min-w-full divide-y divide-slate-200">

                            <thead className="bg-slate-50">

                            <tr>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Product
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Quantity
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    From
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Destination
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Zone
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Shelf
                                </th>

                                <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    Status
                                </th>

                            </tr>

                            </thead>


                            <tbody className="divide-y divide-slate-100">

                            {items.map(
                                (item) => (

                                    <tr
                                        key={item.id}
                                        className="hover:bg-[#fffaf7]"
                                    >

                                        <td className="px-5 py-4">

                                            <p className="font-semibold text-slate-800">
                                                {item.productName ?? "-"}
                                            </p>

                                            <p className="mt-1 text-xs text-slate-400">
                                                Product ID: {item.productId ?? "-"}
                                            </p>

                                        </td>


                                        <td className="px-5 py-4">

                                            <p className="text-sm font-semibold text-slate-700">
                                                {item.putawayQuantity ?? 0}
                                                {" / "}
                                                {item.expectedQuantity ?? 0}
                                            </p>

                                        </td>


                                        <td className="px-5 py-4 text-sm text-slate-600">

                                            {item.fromLocationName ?? "-"}

                                        </td>


                                        <td className="px-5 py-4 text-sm text-slate-600">

                                            {item.toLocationName ?? (
                                                <span className="text-amber-600">
                                                        Not selected
                                                    </span>
                                            )}

                                        </td>


                                        <td className="px-5 py-4 text-sm text-slate-600">

                                            {item.zone ?? "-"}

                                        </td>


                                        <td className="px-5 py-4 text-sm text-slate-600">

                                            {item.shelf ?? "-"}

                                        </td>


                                        <td className="px-5 py-4">

                                            <PutawayStatusBadge
                                                status={item.status}
                                            />

                                        </td>

                                    </tr>
                                )
                            )}

                            </tbody>

                        </table>


                        {!items.length && (

                            <div className="px-6 py-12 text-center text-sm text-slate-500">
                                No putaway items found.
                            </div>

                        )}

                    </div>

                </div>

            </div>

        </div>
    );
}