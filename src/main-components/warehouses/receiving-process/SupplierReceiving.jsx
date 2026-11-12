import {
    useCallback,
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import Alert
    from "../../../components/ui/Alert";

import LoadingPage
    from "../../../components/feedback/LoadingPage";

import ReceivingStageCard
    from "./ReceivingStageCard";

import {
    getReceivingMonitoringList,
    getWaitingReviewList,
    getCompletedReceivingList,
} from "../../../api/receipts/receiving/receivingMonitor";

import {
    getReceivingDiscrepancies,
} from "../../../api/inventories/discrepancy/receivingDiscrepancy";


export default function SupplierReceiving() {

    const navigate =
        useNavigate();


    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        error,
        setError,
    ] = useState("");


    const [
        counts,
        setCounts,
    ] = useState({
        inspecting: 0,
        actionRequired: 0,
        processing: 0,
        completed: 0,
    });


    // =====================================================
    // LOAD COUNTS
    // =====================================================

    const loadCounts =
        useCallback(
            async () => {

                try {

                    setLoading(true);
                    setError("");


                    const [
                        inspectingData,
                        waitingReviewData,
                        discrepancyData,
                        completedData,
                    ] =
                        await Promise.all([

                            getReceivingMonitoringList({
                                page: 0,
                                size: 1,
                            }),

                            getWaitingReviewList({
                                page: 0,
                                size: 1,
                            }),

                            getReceivingDiscrepancies(),

                            getCompletedReceivingList({
                                page: 0,
                                size: 1,
                            }),

                        ]);


                    const processingCount =
                        Array.isArray(
                            discrepancyData
                        )
                            ? discrepancyData.filter(
                                (
                                    item
                                ) => {

                                    const status =
                                        String(
                                            item?.status ?? ""
                                        )
                                            .toUpperCase();


                                    return (
                                        status === "OPEN"
                                        ||
                                        status === "INVESTIGATING"
                                    );
                                }
                            ).length
                            : 0;


                    setCounts({

                        inspecting:
                            inspectingData?.totalElements
                            ?? 0,

                        actionRequired:
                            waitingReviewData?.totalElements
                            ?? 0,

                        processing:
                        processingCount,

                        completed:
                            completedData?.totalElements
                            ?? 0,
                    });


                } catch (
                    exception
                    ) {

                    setError(
                        exception.response
                            ?.data
                            ?.message
                        ??
                        "Unable to load supplier receiving summary."
                    );

                } finally {

                    setLoading(false);
                }
            },
            []
        );


    useEffect(
        () => {

            loadCounts();

        },
        [
            loadCounts,
        ]
    );


    if (loading) {

        return (
            <LoadingPage />
        );
    }


    return (
        <div className="space-y-6">


            {
                error && (

                    <Alert type="danger">
                        {error}
                    </Alert>

                )
            }


            <div className="flex flex-wrap items-start justify-between gap-4">

                <div>

                    <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                        Supplier → Warehouse
                    </p>

                    <h2 className="mt-1 text-xl font-bold text-slate-900">
                        Supplier Receiving
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                        Track supplier receipts through inspection, review, discrepancy handling and completion.
                    </p>

                </div>


                <button
                    type="button"
                    onClick={
                        loadCounts
                    }
                    className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
                >
                    Refresh
                </button>

            </div>


            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">


                {/* ====================================== */}
                {/* INSPECTING                             */}
                {/* ====================================== */}

                <ReceivingStageCard
                    title="Inspecting"
                    description="Warehouse Staff is checking supplier receiving goods."
                    count={
                        counts.inspecting
                    }
                    onClick={() =>
                        navigate(
                            "/warehouse/receiving/in-progress"
                        )
                    }
                    icon={
                        <InspectIcon />
                    }
                />


                {/* ====================================== */}
                {/* ACTION REQUIRED                        */}
                {/* ====================================== */}

                <ReceivingStageCard
                    title="Action Required"
                    description="Inspection has finished and Warehouse Manager review is required."
                    count={
                        counts.actionRequired
                    }
                    onClick={() =>
                        navigate(
                            "/warehouse/receiving/waiting-review"
                        )
                    }
                    icon={
                        <WarningIcon />
                    }
                />


                {/* ====================================== */}
                {/* PROCESSING                             */}
                {/* ====================================== */}

                <ReceivingStageCard
                    title="Processing"
                    description="Receiving discrepancies are currently being handled."
                    count={
                        counts.processing
                    }
                    onClick={() =>
                        navigate(
                            "/warehouse/receiving/incident-reports"
                        )
                    }
                    icon={
                        <ProcessIcon />
                    }
                />


                {/* ====================================== */}
                {/* COMPLETED                              */}
                {/* ====================================== */}

                <ReceivingStageCard
                    title="Completed"
                    description="Receiving receipts confirmed and completed."
                    count={
                        counts.completed
                    }
                    onClick={() =>
                        navigate(
                            "/warehouse/receiving/completed"
                        )
                    }
                    icon={
                        <CompletedIcon />
                    }
                />

            </div>

        </div>
    );
}


// =====================================================
// ICONS
// =====================================================

function InspectIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M5 4h14v16H5V4Zm3 4h8M8 12h5"
            />

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="m14 16 2 2 4-4"
            />

        </svg>
    );
}


function WarningIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M12 3 2 21h20L12 3Z"
            />

            <path
                strokeLinecap="round"
                strokeWidth="2"
                d="M12 9v5"
            />

        </svg>
    );
}


function ProcessIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M20 7h-4V3M4 17h4v4"
            />

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M17 4a8 8 0 0 0-12 4M7 20a8 8 0 0 0 12-4"
            />

        </svg>
    );
}


function CompletedIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
        >

            <circle
                cx="12"
                cy="12"
                r="9"
                strokeWidth="2"
            />

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="m8 12 3 3 5-6"
            />

        </svg>
    );
}