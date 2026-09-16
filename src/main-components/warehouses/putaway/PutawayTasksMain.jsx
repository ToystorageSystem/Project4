import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    getPutawayPlans,
} from "../../../api/warehouses/putaway/putawayPlanning.js";

import PutawaySummaryCards
    from "./PutawaySummaryCards";

import PutawayTaskTable
    from "./PutawayTaskTable";


const STATUS_OPTIONS = [
    {
        value: "ALL",
        label: "All Status",
    },
    {
        value: "AVAILABLE",
        label: "Available",
    },
    {
        value: "IN_PROGRESS",
        label: "In Progress",
    },
    {
        value: "COMPLETED",
        label: "Completed",
    },
    {
        value: "CANCELLED",
        label: "Cancelled",
    },
];


function getErrorMessage(error) {
    const data =
        error?.response?.data;

    if (typeof data === "string") {
        return data;
    }

    return (
        data?.message ??
        error?.message ??
        "Unable to load putaway tasks."
    );
}


export default function PutawayTasksMain() {
    const navigate =
        useNavigate();


    const [
        tasks,
        setTasks,
    ] = useState([]);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        refreshing,
        setRefreshing,
    ] = useState(false);


    const [
        error,
        setError,
    ] = useState("");


    const [
        search,
        setSearch,
    ] = useState("");


    const [
        status,
        setStatus,
    ] = useState("ALL");


    const loadTasks =
        useCallback(
            async (
                isRefresh = false
            ) => {

                try {

                    if (isRefresh) {
                        setRefreshing(true);
                    } else {
                        setLoading(true);
                    }

                    setError("");


                    const data =
                        await getPutawayPlans();


                    setTasks(
                        Array.isArray(data)
                            ? data
                            : []
                    );

                } catch (err) {

                    console.error(
                        "Load putaway plans error:",
                        err
                    );

                    setError(
                        getErrorMessage(err)
                    );

                } finally {

                    setLoading(false);
                    setRefreshing(false);

                }
            },
            []
        );


    useEffect(() => {
        loadTasks();
    }, [loadTasks]);


    const filteredTasks =
        useMemo(
            () => {

                const keyword =
                    search
                        .trim()
                        .toLowerCase();


                return tasks.filter(
                    (task) => {

                        const matchesStatus =
                            status === "ALL" ||
                            task?.status === status;


                        if (!matchesStatus) {
                            return false;
                        }


                        if (!keyword) {
                            return true;
                        }


                        const searchable =
                            [
                                task?.putawayCode,
                                task?.goodsReceiptId,
                                task?.assignedToName,
                                task?.createdByName,
                                task?.status,
                            ]
                                .filter(Boolean)
                                .join(" ")
                                .toLowerCase();


                        return searchable.includes(
                            keyword
                        );
                    }
                );
            },
            [
                tasks,
                search,
                status,
            ]
        );


    const handleView =
        (task) => {

            navigate(
                `/warehouse/putaway/${task.id}`
            );
        };


    return (
        <div className="space-y-6">

            {/* HEADER */}

            <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">

                <div>

                    <div className="flex items-center gap-2">

                        <div className="h-6 w-1 rounded-full bg-[#f25d19]" />

                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#f25d19]">
                            Warehouse Manager
                        </p>

                    </div>


                    <h1 className="mt-2 text-2xl font-bold text-slate-900">
                        Putaway Management
                    </h1>


                    <p className="mt-1 text-sm text-slate-500">
                        Monitor putaway tasks, staff assignment and warehouse storage progress.
                    </p>

                </div>


                <button
                    type="button"
                    onClick={() =>
                        loadTasks(true)
                    }
                    disabled={refreshing}
                    className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#f25d19] px-5 text-sm font-semibold text-white transition hover:bg-[#d94f12] disabled:cursor-not-allowed disabled:opacity-60"
                >

                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        className={
                            "h-4 w-4 " +
                            (
                                refreshing
                                    ? "animate-spin"
                                    : ""
                            )
                        }
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M20 11a8 8 0 1 0 2 5M20 4v7h-7"
                        />
                    </svg>

                    {refreshing
                        ? "Refreshing..."
                        : "Refresh"}

                </button>

            </div>


            {/* SUMMARY */}

            <PutawaySummaryCards
                tasks={tasks}
            />


            {/* ERROR */}

            {error && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}


            {/* FILTER */}

            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">

                <div className="flex flex-col gap-3 lg:flex-row">

                    <div className="relative flex-1">

                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                        >
                            <circle
                                cx="11"
                                cy="11"
                                r="7"
                                strokeWidth="2"
                            />

                            <path
                                strokeLinecap="round"
                                strokeWidth="2"
                                d="m20 20-3.5-3.5"
                            />
                        </svg>


                        <input
                            type="text"
                            value={search}
                            onChange={
                                (event) =>
                                    setSearch(
                                        event.target.value
                                    )
                            }
                            placeholder="Search task code, receipt, staff..."
                            className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />

                    </div>


                    <select
                        value={status}
                        onChange={
                            (event) =>
                                setStatus(
                                    event.target.value
                                )
                        }
                        className="h-11 min-w-[190px] rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-700 outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                    >

                        {STATUS_OPTIONS.map(
                            (option) => (
                                <option
                                    key={option.value}
                                    value={option.value}
                                >
                                    {option.label}
                                </option>
                            )
                        )}

                    </select>

                </div>

            </div>


            {/* CONTENT */}

            {loading ? (

                <div className="flex min-h-[300px] items-center justify-center rounded-2xl border border-slate-200 bg-white">

                    <div className="text-center">

                        <div className="mx-auto h-9 w-9 animate-spin rounded-full border-4 border-slate-200 border-t-[#f25d19]" />

                        <p className="mt-3 text-sm text-slate-500">
                            Loading putaway tasks...
                        </p>

                    </div>

                </div>

            ) : (

                <PutawayTaskTable
                    tasks={filteredTasks}
                    onView={handleView}
                />

            )}

        </div>
    );
}