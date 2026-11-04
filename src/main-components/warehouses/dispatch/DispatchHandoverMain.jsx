import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
} from "react-router-dom";

import DispatchHandover
    from "../../../components/warehouse/dispatch/DispatchHandover.jsx";

import {
    confirmShipment,
    getShipment,
} from "../../../api/packages/packing/managerPacking.js";


export default function DispatchHandoverMain() {

    const { transferId } =
        useParams();


    const navigate =
        useNavigate();


    const [data, setData] =
        useState(null);


    const [loading, setLoading] =
        useState(true);


    const [confirming, setConfirming] =
        useState(false);


    const [error, setError] =
        useState("");


    const [successMessage, setSuccessMessage] =
        useState("");


    // =====================================================
    // LOAD
    // =====================================================

    useEffect(() => {

        const load =
            async () => {

                try {

                    setLoading(true);
                    setError("");


                    const response =
                        await getShipment(
                            transferId
                        );


                    setData(
                        response
                    );

                } catch (err) {

                    setError(
                        getErrorMessage(
                            err,
                            "Failed to load dispatch information."
                        )
                    );


                    setData(
                        null
                    );

                } finally {

                    setLoading(false);

                }
            };


        load();

    }, [
        transferId,
    ]);


    // =====================================================
    // PACKAGES
    // =====================================================

    const packages =
        useMemo(
            () => {

                return Array.isArray(
                    data?.packages
                )
                    ? data.packages
                    : [];

            },
            [
                data,
            ]
        );


    // =====================================================
    // TOTAL QUANTITY
    // =====================================================

    const totalQuantity =
        useMemo(
            () => {

                return packages.reduce(
                    (
                        total,
                        pack
                    ) => {

                        return (
                            total +
                            (
                                Number(
                                    pack?.totalQuantity
                                ) || 0
                            )
                        );

                    },
                    0
                );

            },
            [
                packages,
            ]
        );


    // =====================================================
    // SHIPPED?
    // =====================================================

    const alreadyShipped =
        data?.transferStatus ===
        "SHIPPED"
        || Boolean(
            data?.handedOverAt
        );


    // =====================================================
    // PACKAGE CHECK
    // =====================================================

    const packagesReady =
        packages.length > 0
        && packages.every(
            (pack) => {

                return (
                    pack.status ===
                    "CHECKED"
                    && Boolean(
                        pack.sealNumber
                    )
                );

            }
        );


    // =====================================================
    // CAN CONFIRM
    // =====================================================

    const canConfirmHandover =
        data?.transferStatus ===
        "PACKED"
        && Boolean(
            data?.manifestId
        )
        && Boolean(
            data?.deliveryId
        )
        && Boolean(
            data?.driverId
        )
        && data?.deliveryStatus ===
        "ACCEPTED"
        && packagesReady
        && !alreadyShipped;


    // =====================================================
    // CONFIRM
    // =====================================================

    const handleConfirm =
        async () => {

            if (!canConfirmHandover) {
                return;
            }


            const accepted =
                window.confirm(
                    "Confirm that all packages have left the warehouse and have been handed over to the delivery person?"
                );


            if (!accepted) {
                return;
            }


            try {

                setConfirming(
                    true
                );


                setError(
                    ""
                );


                setSuccessMessage(
                    ""
                );


                const response =
                    await confirmShipment(
                        transferId,
                        data.deliveryId
                    );


                setData(
                    response
                );


                setSuccessMessage(
                    "Shipment dispatched and handed over successfully."
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to confirm dispatch and handover."
                    )
                );

            } finally {

                setConfirming(
                    false
                );

            }
        };


    // =====================================================
    // PRINT
    // =====================================================

    const handlePrintDispatch =
        () => {

            if (!alreadyShipped) {
                return;
            }


            window.print();
        };


    // =====================================================
    // VIEW MANIFEST
    // =====================================================

    const handleViewManifest =
        () => {

            navigate(
                `/warehouse/manifests/transfer/${transferId}`
            );
        };


    // =====================================================
    // BACK
    // =====================================================

    const handleBack =
        () => {

            navigate(-1);
        };


    // =====================================================
    // LOADING
    // =====================================================

    if (loading) {

        return (
            <div
                className="
                    flex
                    min-h-[420px]
                    items-center
                    justify-center
                "
            >

                <div
                    className="
                        flex
                        flex-col
                        items-center
                        gap-3
                    "
                >

                    <div
                        className="
                            h-10
                            w-10
                            animate-spin
                            rounded-full
                            border-4
                            border-slate-200
                            border-t-[#f25d19]
                        "
                    />


                    <p
                        className="
                            text-sm
                            text-slate-500
                        "
                    >
                        Loading dispatch...
                    </p>

                </div>

            </div>
        );
    }


    // =====================================================
    // NOT FOUND
    // =====================================================

    if (!data) {

        return (
            <div
                className="
                    rounded-2xl
                    border
                    border-red-200
                    bg-red-50
                    p-6
                "
            >

                <h2
                    className="
                        font-bold
                        text-red-700
                    "
                >
                    Unable to load dispatch
                </h2>


                <p
                    className="
                        mt-2
                        text-sm
                        text-red-600
                    "
                >
                    {
                        error ||
                        "Dispatch information was not found."
                    }
                </p>


                <button
                    type="button"
                    onClick={
                        handleBack
                    }
                    className="
                        mt-5
                        rounded-xl
                        border
                        border-red-300
                        bg-white
                        px-4
                        py-2.5
                        text-sm
                        font-semibold
                        text-red-700
                    "
                >
                    Back
                </button>

            </div>
        );
    }


    // =====================================================
    // RENDER
    // =====================================================

    return (
        <DispatchHandover
            data={
                data
            }

            packages={
                packages
            }

            totalQuantity={
                totalQuantity
            }

            alreadyShipped={
                alreadyShipped
            }

            canConfirmHandover={
                canConfirmHandover
            }

            confirming={
                confirming
            }

            error={
                error
            }

            successMessage={
                successMessage
            }

            onBack={
                handleBack
            }

            onViewManifest={
                handleViewManifest
            }

            onConfirm={
                handleConfirm
            }

            onPrintDispatch={
                handlePrintDispatch
            }
        />
    );
}


// =====================================================
// ERROR
// =====================================================

function getErrorMessage(
    error,
    fallback
) {

    const response =
        error?.response?.data;


    if (
        typeof response ===
        "string"
    ) {
        return response;
    }


    return (
        response?.message
        || response?.error
        || fallback
    );
}