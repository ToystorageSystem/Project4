import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
} from "react-router-dom";

import {
    getDamagedGoodsReport,
    handleDamagedGoodsItem,
    startDamagedGoodsInspection,
} from "../../../api/inventories/damaged/damagedGoods.js";

import AlertMessage
    from "../../../components/warehouse/damaged/AlertMessage.jsx";

import StatusBadge
    from "../../../components/warehouse/damaged/StatusBadge.jsx";

import DamagedGoodsReportInfo
    from "./DamagedGoodsReportInfo.jsx";

import DamagedGoodsItemCard
    from "./DamagedGoodsItemCard.jsx";


export default function DamagedGoodsDetailMain() {

    const {
        reportId,
    } = useParams();


    const navigate =
        useNavigate();


    const [report, setReport] =
        useState(null);


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");


    const [
        successMessage,
        setSuccessMessage,
    ] = useState("");


    const [
        processingItemId,
        setProcessingItemId,
    ] = useState(null);


    const [
        startingInspection,
        setStartingInspection,
    ] = useState(false);


    // =====================================================
    // LOAD
    // =====================================================

    const loadReport =
        async () => {

            try {

                setLoading(true);
                setError("");


                const data =
                    await getDamagedGoodsReport(
                        reportId
                    );


                setReport(
                    data
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to load damaged goods report."
                    )
                );

            } finally {

                setLoading(false);
            }
        };


    useEffect(() => {

        loadReport();

    }, [reportId]);


    // =====================================================
    // START
    // =====================================================

    const handleStartInspection =
        async () => {

            const confirmed =
                window.confirm(
                    "Start inspection for this damaged goods report?"
                );


            if (!confirmed) {
                return;
            }


            try {

                setStartingInspection(true);

                setError("");
                setSuccessMessage("");


                const data =
                    await startDamagedGoodsInspection(
                        reportId
                    );


                setReport(
                    data
                );


                setSuccessMessage(
                    "Inspection started successfully."
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to start inspection."
                    )
                );

            } finally {

                setStartingInspection(false);
            }
        };


    // =====================================================
    // HANDLE ITEM
    // =====================================================

    const handleItem =
        async ({
                   itemId,
                   confirmedQuantity,
                   disposition,
                   resolutionNote,
               }) => {

            let confirmMessage =
                "Confirm this damaged goods resolution?";


            if (
                disposition
                === "RETURN_TO_SUPPLIER"
            ) {

                confirmMessage =
                    "Confirm returning this damaged quantity to the supplier?";
            }


            if (
                disposition
                === "DISPOSE"
            ) {

                confirmMessage =
                    "Confirm disposal? This action will reduce physical inventory.";
            }


            const confirmed =
                window.confirm(
                    confirmMessage
                );


            if (!confirmed) {
                return;
            }


            try {

                setProcessingItemId(
                    itemId
                );

                setError("");
                setSuccessMessage("");


                const data =
                    await handleDamagedGoodsItem({

                        reportId,

                        itemId,

                        confirmedQuantity,

                        disposition,

                        resolutionNote,
                    });


                setReport(
                    data
                );


                setSuccessMessage(
                    "Damaged goods item processed successfully."
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to process damaged goods item."
                    )
                );

            } finally {

                setProcessingItemId(
                    null
                );
            }
        };


    // =====================================================
    // LOADING
    // =====================================================

    if (loading) {

        return (
            <div
                className="
                    py-16
                    text-center
                    text-sm
                    text-slate-500
                "
            >
                Loading damaged goods report...
            </div>
        );
    }


    if (!report) {

        return (
            <AlertMessage
                type="error"
                message={
                    error
                    || "Damaged goods report not found."
                }
            />
        );
    }


    return (
        <div className="w-full">

            {/* HEADER */}
            <div
                className="
                    mb-6
                    flex
                    flex-col
                    gap-4
                    lg:flex-row
                    lg:items-center
                    lg:justify-between
                "
            >

                <div>

                    <button
                        type="button"

                        onClick={() =>
                            navigate(
                                "/warehouse/damaged-goods"
                            )
                        }

                        className="
                            mb-3
                            text-sm
                            font-medium
                            text-slate-500
                            transition
                            hover:text-[#f25d19]
                        "
                    >
                        ← Back to Damaged Goods
                    </button>


                    <h1
                        className="
                            text-2xl
                            font-bold
                            text-slate-900
                        "
                    >
                        Damaged Goods Report
                    </h1>


                    <p
                        className="
                            mt-1
                            text-sm
                            text-slate-500
                        "
                    >
                        {report.reportCode}
                    </p>

                </div>


                <div
                    className="
                        flex
                        items-center
                        gap-3
                    "
                >

                    <StatusBadge
                        status={
                            report.status
                        }
                    />


                    {report.status ===
                        "REPORTED" && (

                            <button
                                type="button"

                                disabled={
                                    startingInspection
                                }

                                onClick={
                                    handleStartInspection
                                }

                                className="
                                rounded-xl
                                bg-[#f25d19]
                                px-5
                                py-2.5
                                text-sm
                                font-semibold
                                text-white
                                transition
                                hover:bg-[#d94f12]
                                disabled:cursor-not-allowed
                                disabled:opacity-50
                            "
                            >
                                {startingInspection
                                    ? "Starting..."
                                    : "Start Inspection"}
                            </button>

                        )}

                </div>

            </div>


            <AlertMessage
                type="error"
                message={error}
            />


            <AlertMessage
                type="success"
                message={
                    successMessage
                }
            />


            {/* REPORT */}
            <DamagedGoodsReportInfo
                report={
                    report
                }
            />


            {/* ITEMS HEADER */}
            <div className="mb-4">

                <h2
                    className="
                        text-lg
                        font-semibold
                        text-slate-900
                    "
                >
                    Damaged Products
                </h2>


                <p
                    className="
                        mt-1
                        text-sm
                        text-slate-500
                    "
                >
                    Confirm the actual damaged quantity and select the appropriate resolution.
                </p>

            </div>


            {/* ITEMS */}
            <div className="space-y-4">

                {report.items?.length > 0
                    ? report.items.map(
                        (item) => (

                            <DamagedGoodsItemCard

                                key={
                                    item.itemId
                                }

                                item={
                                    item
                                }

                                reportStatus={
                                    report.status
                                }

                                processing={
                                    processingItemId
                                    === item.itemId
                                }

                                onHandleItem={
                                    handleItem
                                }
                            />

                        ))

                    : (

                        <div
                            className="
                                rounded-2xl
                                border
                                border-slate-200
                                bg-white
                                px-5
                                py-12
                                text-center
                                text-sm
                                text-slate-500
                            "
                        >
                            No damaged products found.
                        </div>

                    )}

            </div>

        </div>
    );
}


function getErrorMessage(
    error,
    fallback
) {

    const data =
        error?.response?.data;


    if (
        typeof data === "string"
    ) {
        return data;
    }


    return (
        data?.message
        || data?.error
        || fallback
    );
}