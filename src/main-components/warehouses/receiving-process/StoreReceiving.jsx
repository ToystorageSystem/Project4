import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import Alert
    from "../../../components/ui/Alert";

import LoadingPage
    from "../../../components/feedback/LoadingPage";

import {
    getStoreReturn,
    getStoreReturns,
} from "../../../api/stores/returns/storeReturnInspection";

import ReceivingStageCard
    from "./ReceivingStageCard";

import StoreReturnList
    from "./StoreReturnList";

import StoreReturnDetail
    from "./StoreReturnDetail";

import {
    normalizeStatus,
} from "./ReceivingStatusBadge";


const STAGES = {

    WAITING:
        "WAITING",

    INSPECTING:
        "INSPECTING",

    ACTION_REQUIRED:
        "ACTION_REQUIRED",

    PROCESSING:
        "PROCESSING",

    COMPLETED:
        "COMPLETED",
};


export default function StoreReceiving() {

    const [
        items,
        setItems,
    ] = useState([]);

    const [
        selected,
        setSelected,
    ] = useState(null);

    const [
        selectedStage,
        setSelectedStage,
    ] = useState(
        STAGES.WAITING
    );

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        detailLoading,
        setDetailLoading,
    ] = useState(false);

    const [
        error,
        setError,
    ] = useState("");

    const [
        keyword,
        setKeyword,
    ] = useState("");


    // =====================================================
    // LOAD LIST
    // =====================================================

    const loadList =
        useCallback(
            async () => {

                try {

                    setLoading(
                        true
                    );

                    setError("");


                    const data =
                        await getStoreReturns();


                    setItems(
                        Array.isArray(
                            data
                        )
                            ? data
                            : []
                    );

                } catch (
                    exception
                    ) {

                    setError(
                        exception.response
                            ?.data
                            ?.message
                        ??
                        "Unable to load Store Returns."
                    );

                } finally {

                    setLoading(
                        false
                    );
                }
            },
            []
        );


    // =====================================================
    // DETAIL
    // =====================================================

    const loadDetail =
        async (
            returnId
        ) => {

            try {

                setDetailLoading(
                    true
                );

                setError("");


                const data =
                    await getStoreReturn(
                        returnId
                    );


                setSelected(
                    data
                );


                return data;

            } catch (
                exception
                ) {

                setError(
                    exception.response
                        ?.data
                        ?.message
                    ??
                    "Unable to load Store Return."
                );


                return null;

            } finally {

                setDetailLoading(
                    false
                );
            }
        };


    const openReturn =
        async (
            item
        ) => {

            const returnId =
                item?.returnId;


            if (!returnId) {
                return;
            }


            await loadDetail(
                returnId
            );
        };


    // =====================================================
    // REFRESH
    // =====================================================

    const refreshDetail =
        async () => {

            const returnId =
                selected?.returnId;


            if (!returnId) {
                return;
            }


            const refreshed =
                await loadDetail(
                    returnId
                );


            await loadList();


            if (
                normalizeStatus(
                    refreshed?.status
                )
                === "RECEIVED"
            ) {

                setSelectedStage(
                    STAGES.COMPLETED
                );
            }
        };


    // =====================================================
    // DISCREPANCY HELPERS
    // =====================================================

    const getDiscrepancies =
        (
            item
        ) => {

            if (
                !Array.isArray(
                    item?.discrepancies
                )
            ) {
                return [];
            }


            return item.discrepancies;
        };


    const hasOpenDiscrepancy =
        (
            item
        ) =>
            getDiscrepancies(
                item
            )
                .some(
                    (
                        report
                    ) =>
                        normalizeStatus(
                            report?.status
                        )
                        === "OPEN"
                );


    const hasInvestigatingDiscrepancy =
        (
            item
        ) =>
            getDiscrepancies(
                item
            )
                .some(
                    (
                        report
                    ) =>
                        normalizeStatus(
                            report?.status
                        )
                        === "INVESTIGATING"
                );


    // =====================================================
    // FILTER
    // =====================================================

    const filteredByStage =
        useMemo(
            () => {

                return items.filter(
                    (
                        item
                    ) => {

                        const status =
                            normalizeStatus(
                                item?.status
                            );


                        switch (
                            selectedStage
                            ) {

                            case STAGES.WAITING:

                                return status
                                    === "SHIPPED";


                            case STAGES.INSPECTING:

                                return status
                                    === "INSPECTING";


                            case STAGES.ACTION_REQUIRED:

                                return (
                                    status
                                    === "PENDING_CONFIRMATION"

                                    &&

                                    !hasInvestigatingDiscrepancy(
                                        item
                                    )
                                );


                            case STAGES.PROCESSING:

                                return hasInvestigatingDiscrepancy(
                                    item
                                );


                            case STAGES.COMPLETED:

                                return status
                                    === "RECEIVED";


                            default:

                                return true;
                        }
                    }
                );

            },
            [
                items,
                selectedStage,
            ]
        );


    // =====================================================
    // COUNTS
    // =====================================================

    const counts =
        useMemo(
            () => {

                const waiting =
                    items.filter(
                        (
                            item
                        ) =>
                            normalizeStatus(
                                item?.status
                            )
                            === "SHIPPED"
                    ).length;


                const inspecting =
                    items.filter(
                        (
                            item
                        ) =>
                            normalizeStatus(
                                item?.status
                            )
                            === "INSPECTING"
                    ).length;


                const processing =
                    items.filter(
                        hasInvestigatingDiscrepancy
                    ).length;


                const actionRequired =
                    items.filter(
                        (
                            item
                        ) => {

                            const status =
                                normalizeStatus(
                                    item?.status
                                );


                            return (
                                status
                                === "PENDING_CONFIRMATION"

                                &&

                                !hasInvestigatingDiscrepancy(
                                    item
                                )
                            );
                        }
                    ).length;


                const completed =
                    items.filter(
                        (
                            item
                        ) =>
                            normalizeStatus(
                                item?.status
                            )
                            === "RECEIVED"
                    ).length;


                return {
                    waiting,
                    inspecting,
                    actionRequired,
                    processing,
                    completed,
                };

            },
            [
                items,
            ]
        );


    // =====================================================
    // INIT
    // =====================================================

    useEffect(
        () => {

            loadList();

        },
        [
            loadList,
        ]
    );


    // =====================================================
    // DETAIL VIEW
    // =====================================================

    if (
        selected
    ) {

        return (
            <div className="space-y-5">

                {
                    error && (
                        <Alert type="danger">
                            {error}
                        </Alert>
                    )
                }


                {
                    detailLoading && (
                        <Alert type="info">
                            Loading Store Return...
                        </Alert>
                    )
                }


                <StoreReturnDetail
                    data={
                        selected
                    }
                    onBack={() =>
                        setSelected(
                            null
                        )
                    }
                    onRefresh={
                        refreshDetail
                    }
                />

            </div>
        );
    }


    // =====================================================
    // LOADING
    // =====================================================

    if (
        loading
    ) {

        return (
            <LoadingPage />
        );
    }


    // =====================================================
    // LIST VIEW
    // =====================================================

    return (
        <div className="space-y-6">


            {
                error && (
                    <Alert type="danger">
                        {error}
                    </Alert>
                )
            }


            <div>

                <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                    Store → Warehouse
                </p>

                <h2 className="mt-1 text-xl font-bold text-slate-900">
                    Store Return Receiving
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                    Track returned goods through inspection, review, handling and completion.
                </p>

            </div>


            {/* ========================================== */}
            {/* STAGES                                     */}
            {/* ========================================== */}

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">

                <ReceivingStageCard
                    title="Waiting for Inspection"
                    description="Returned goods are waiting for Warehouse Staff."
                    count={
                        counts.waiting
                    }
                    active={
                        selectedStage
                        === STAGES.WAITING
                    }
                    onClick={() =>
                        setSelectedStage(
                            STAGES.WAITING
                        )
                    }
                    icon={
                        <ClockIcon />
                    }
                />


                <ReceivingStageCard
                    title="Inspecting"
                    description="Warehouse Staff is checking quantity and condition."
                    count={
                        counts.inspecting
                    }
                    active={
                        selectedStage
                        === STAGES.INSPECTING
                    }
                    onClick={() =>
                        setSelectedStage(
                            STAGES.INSPECTING
                        )
                    }
                    icon={
                        <InspectIcon />
                    }
                />


                <ReceivingStageCard
                    title="Action Required"
                    description="Warehouse Manager review is required."
                    count={
                        counts.actionRequired
                    }
                    active={
                        selectedStage
                        === STAGES.ACTION_REQUIRED
                    }
                    onClick={() =>
                        setSelectedStage(
                            STAGES.ACTION_REQUIRED
                        )
                    }
                    icon={
                        <WarningIcon />
                    }
                />


                <ReceivingStageCard
                    title="Processing"
                    description="Discrepancies are being investigated or handled."
                    count={
                        counts.processing
                    }
                    active={
                        selectedStage
                        === STAGES.PROCESSING
                    }
                    onClick={() =>
                        setSelectedStage(
                            STAGES.PROCESSING
                        )
                    }
                    icon={
                        <ProcessIcon />
                    }
                />


                <ReceivingStageCard
                    title="Completed"
                    description="Return has been confirmed and completed."
                    count={
                        counts.completed
                    }
                    active={
                        selectedStage
                        === STAGES.COMPLETED
                    }
                    onClick={() =>
                        setSelectedStage(
                            STAGES.COMPLETED
                        )
                    }
                    icon={
                        <CompletedIcon />
                    }
                />

            </div>


            {/* ========================================== */}
            {/* CURRENT STAGE                              */}
            {/* ========================================== */}

            <div className="flex flex-wrap items-center justify-between gap-3">

                <div>

                    <p className="text-xs text-slate-400">
                        Current Stage
                    </p>

                    <h3 className="mt-1 text-lg font-semibold text-slate-900">
                        {
                            getStageTitle(
                                selectedStage
                            )
                        }
                    </h3>

                </div>


                <button
                    type="button"
                    onClick={
                        loadList
                    }
                    className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
                >
                    Refresh
                </button>

            </div>


            <StoreReturnList
                items={
                    filteredByStage
                }
                onView={
                    openReturn
                }
                keyword={
                    keyword
                }
                setKeyword={
                    setKeyword
                }
            />

        </div>
    );
}


// =====================================================
// STAGE TITLE
// =====================================================

function getStageTitle(
    stage
) {

    switch (
        stage
        ) {

        case STAGES.WAITING:
            return "Waiting for Inspection";

        case STAGES.INSPECTING:
            return "Currently Inspecting";

        case STAGES.ACTION_REQUIRED:
            return "Manager Action Required";

        case STAGES.PROCESSING:
            return "Currently Processing";

        case STAGES.COMPLETED:
            return "Completed Store Returns";

        default:
            return "Store Returns";
    }
}


// =====================================================
// ICONS
// =====================================================

function ClockIcon() {

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
                d="M12 7v5l3 2"
            />
        </svg>
    );
}


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