import {
    useState,
} from "react";

import Button
    from "../../../components/ui/Button";

import Alert
    from "../../../components/ui/Alert";

import {
    confirmStoreReturn,
    resolveStoreReturnAsWarehouse,
    sendStoreReturnToStoreManager,
} from "../../../api/stores/returns/storeReturnInspection";

import ReceivingStatusBadge, {
    normalizeStatus,
} from "./ReceivingStatusBadge";


const formatDateTime = (value) => {

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
        return "-";
    }

    return date.toLocaleString(
        "en-GB"
    );
};


export default function StoreReturnDetail({
                                              data,
                                              onBack,
                                              onRefresh,
                                          }) {

    const [
        busyId,
        setBusyId,
    ] = useState(null);

    const [
        confirming,
        setConfirming,
    ] = useState(false);

    const [
        reasons,
        setReasons,
    ] = useState({});

    const [
        error,
        setError,
    ] = useState("");

    const [
        success,
        setSuccess,
    ] = useState("");


    const returnId =
        data?.returnId;


    // =====================================================
    // DISCREPANCIES
    // =====================================================

    const discrepancies =
        Array.isArray(
            data?.discrepancies
        )
            ? data.discrepancies
            : [];


    const unresolved =
        discrepancies.filter(
            (
                report
            ) =>
                [
                    "OPEN",
                    "INVESTIGATING",
                ]
                    .includes(
                        normalizeStatus(
                            report?.status
                        )
                    )
        );


    // =====================================================
    // CONFIRM RULE
    // =====================================================

    const canConfirm =
        normalizeStatus(
            data?.status
        )
        === "PENDING_CONFIRMATION"

        &&

        unresolved.length === 0;


    // =====================================================
    // FIND DISCREPANCY FOR ITEM
    // =====================================================

    const findDiscrepancy =
        (
            item
        ) => {

            const productId =
                Number(
                    item?.productId
                );


            return discrepancies.find(
                (
                    report
                ) =>
                    Number(
                        report?.productId
                    )
                    === productId
            );
        };


    // =====================================================
    // WAREHOUSE RESPONSIBLE
    // =====================================================

    const handleWarehouse =
        async (
            report
        ) => {

            const reason =
                reasons[
                    report.id
                    ]
                    ?.trim();


            if (!reason) {

                setError(
                    "Please enter a reason before resolving the discrepancy."
                );

                return;
            }


            try {

                setBusyId(
                    report.id
                );

                setError("");
                setSuccess("");


                await resolveStoreReturnAsWarehouse(
                    returnId,
                    report.id,
                    reason
                );


                setSuccess(
                    "Discrepancy assigned to Warehouse and resolved."
                );


                if (onRefresh) {
                    await onRefresh();
                }

            } catch (
                exception
                ) {

                setError(
                    exception.response
                        ?.data
                        ?.message
                    ??
                    "Unable to resolve discrepancy."
                );

            } finally {

                setBusyId(
                    null
                );
            }
        };


    // =====================================================
    // SEND TO STORE MANAGER
    // =====================================================

    const handleSendStore =
        async (
            report
        ) => {

            const reason =
                reasons[
                    report.id
                    ]
                    ?.trim();


            if (!reason) {

                setError(
                    "Please enter a reason before sending the case to Store Manager."
                );

                return;
            }


            try {

                setBusyId(
                    report.id
                );

                setError("");
                setSuccess("");


                await sendStoreReturnToStoreManager(
                    returnId,
                    report.id,
                    reason
                );


                setSuccess(
                    "Discrepancy sent to Store Manager for investigation."
                );


                if (onRefresh) {
                    await onRefresh();
                }

            } catch (
                exception
                ) {

                setError(
                    exception.response
                        ?.data
                        ?.message
                    ??
                    "Unable to send discrepancy to Store Manager."
                );

            } finally {

                setBusyId(
                    null
                );
            }
        };


    // =====================================================
    // CONFIRM STORE RETURN
    // =====================================================

    const handleConfirm =
        async () => {

            if (!returnId) {
                return;
            }


            if (
                !window.confirm(
                    "Confirm this Store Return? Inventory will be updated based on the inspection result."
                )
            ) {
                return;
            }


            try {

                setConfirming(
                    true
                );

                setError("");
                setSuccess("");


                await confirmStoreReturn(
                    returnId
                );


                setSuccess(
                    "Store Return confirmed successfully."
                );


                if (onRefresh) {
                    await onRefresh();
                }

            } catch (
                exception
                ) {

                setError(
                    exception.response
                        ?.data
                        ?.message
                    ??
                    "Unable to confirm Store Return."
                );

            } finally {

                setConfirming(
                    false
                );
            }
        };


    // =====================================================
    // RENDER
    // =====================================================

    return (
        <div className="space-y-5">


            {/* ========================================== */}
            {/* HEADER ACTIONS                             */}
            {/* ========================================== */}

            <div className="flex flex-wrap items-center justify-between gap-3">

                <Button
                    variant="outline"
                    onClick={onBack}
                >
                    ← Back
                </Button>


                <div className="flex flex-wrap items-center gap-2">

                    <ReceivingStatusBadge
                        status={
                            data?.status
                        }
                    />


                    <Button
                        onClick={
                            handleConfirm
                        }
                        disabled={
                            !canConfirm
                            ||
                            confirming
                        }
                    >
                        {
                            confirming
                                ? "Confirming..."
                                : "Confirm Store Return"
                        }
                    </Button>

                </div>

            </div>


            {/* ========================================== */}
            {/* ALERT                                      */}
            {/* ========================================== */}

            {
                error && (

                    <Alert type="danger">
                        {error}
                    </Alert>

                )
            }


            {
                success && (

                    <Alert type="success">
                        {success}
                    </Alert>

                )
            }


            {
                !data?.discrepancies && (

                    <Alert type="warning">

                        Backend response currently does not contain a
                        discrepancies array.

                        Product inspection can still be viewed, but
                        discrepancy actions require discrepancies to be
                        returned by:

                        {" "}

                        GET /warehouse/store-returns/{returnId}

                    </Alert>

                )
            }


            {
                unresolved.length > 0 && (

                    <Alert type="warning">

                        This Store Return still has{" "}

                        {unresolved.length}

                        {" "}

                        unresolved discrepancy report(s).

                        Resolve them before confirming inventory.

                    </Alert>

                )
            }


            {/* ========================================== */}
            {/* RETURN INFORMATION                         */}
            {/* ========================================== */}

            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

                <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">

                    <div>

                        <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                            Store → Warehouse
                        </p>


                        <h2 className="mt-1 text-2xl font-bold text-slate-900">

                            {
                                data?.returnCode
                                ??
                                `Return #${returnId ?? "-"}`
                            }

                        </h2>


                        <p className="mt-2 text-sm text-slate-500">

                            {
                                data?.storeName
                                ??
                                `Store #${data?.storeId ?? "-"}`
                            }

                            {" → "}

                            {
                                data?.warehouseName
                                ??
                                `Warehouse #${data?.warehouseId ?? "-"}`
                            }

                        </p>

                    </div>


                    <div className="text-sm text-slate-500">

                        <p>

                            Type:{" "}

                            <span className="font-semibold text-slate-800">

                                {
                                    data?.returnType
                                    ??
                                    "-"
                                }

                            </span>

                        </p>


                        <p className="mt-1">

                            Inspection submitted:{" "}

                            <span className="font-medium text-slate-700">

                                {
                                    formatDateTime(
                                        data?.inspectionSubmittedAt
                                    )
                                }

                            </span>

                        </p>


                        <p className="mt-1">

                            Received At:{" "}

                            <span className="font-medium text-slate-700">

                                {
                                    formatDateTime(
                                        data?.receivedAt
                                    )
                                }

                            </span>

                        </p>

                    </div>

                </div>


                {/* ====================================== */}
                {/* SUMMARY                                */}
                {/* ====================================== */}

                <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-4">

                    <div className="rounded-xl bg-slate-50 p-4">

                        <p className="text-xs text-slate-400">
                            Requested
                        </p>

                        <p className="mt-1 text-xl font-bold text-slate-900">

                            {
                                data?.totalRequestedQuantity
                                ??
                                0
                            }

                        </p>

                    </div>


                    <div className="rounded-xl bg-blue-50 p-4">

                        <p className="text-xs text-blue-500">
                            Received
                        </p>

                        <p className="mt-1 text-xl font-bold text-blue-700">

                            {
                                data?.totalReceivedQuantity
                                ??
                                0
                            }

                        </p>

                    </div>


                    <div className="rounded-xl bg-green-50 p-4">

                        <p className="text-xs text-green-500">
                            Approved
                        </p>

                        <p className="mt-1 text-xl font-bold text-green-700">

                            {
                                data?.totalApprovedQuantity
                                ??
                                0
                            }

                        </p>

                    </div>


                    <div className="rounded-xl bg-red-50 p-4">

                        <p className="text-xs text-red-500">
                            Rejected
                        </p>

                        <p className="mt-1 text-xl font-bold text-red-700">

                            {
                                data?.totalRejectedQuantity
                                ??
                                0
                            }

                        </p>

                    </div>

                </div>

            </div>


            {/* ========================================== */}
            {/* ITEMS                                      */}
            {/* ========================================== */}

            <div className="space-y-4">

                {
                    (
                        data?.items
                        ??
                        []
                    )
                        .map(
                            (
                                item
                            ) => {

                                const report =
                                    findDiscrepancy(
                                        item
                                    );


                                const reportStatus =
                                    normalizeStatus(
                                        report?.status
                                    );


                                const storeOwned =
                                    String(
                                        report
                                            ?.responsibleParty
                                        ??
                                        ""
                                    )
                                        .toUpperCase()
                                    === "STORE_MANAGER";


                                const warehouseOwned =
                                    String(
                                        report
                                            ?.responsibleParty
                                        ??
                                        ""
                                    )
                                        .toUpperCase()
                                    === "WAREHOUSE_MANAGER";


                                return (

                                    <div
                                        key={
                                            item?.itemId
                                            ??
                                            item?.productId
                                        }
                                        className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
                                    >


                                        {/* ============================== */}
                                        {/* PRODUCT HEADER                 */}
                                        {/* ============================== */}

                                        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">

                                            <div>

                                                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                                                    Product
                                                </p>


                                                <h3 className="mt-1 text-lg font-semibold text-slate-900">

                                                    {
                                                        item?.productName
                                                        ??
                                                        `Product #${item?.productId ?? "-"}`
                                                    }

                                                </h3>

                                            </div>


                                            <ReceivingStatusBadge
                                                status={
                                                    item?.conditionStatus
                                                }
                                            />

                                        </div>


                                        {/* ============================== */}
                                        {/* QUANTITY                       */}
                                        {/* ============================== */}

                                        <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-5">


                                            <div className="rounded-xl bg-slate-50 p-3">

                                                <p className="text-xs text-slate-400">
                                                    Requested
                                                </p>

                                                <p className="mt-1 font-semibold text-slate-800">

                                                    {
                                                        item?.requestedQuantity
                                                        ??
                                                        0
                                                    }

                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-slate-50 p-3">

                                                <p className="text-xs text-slate-400">
                                                    Issued
                                                </p>

                                                <p className="mt-1 font-semibold text-slate-800">

                                                    {
                                                        item?.issuedQuantity
                                                        ??
                                                        0
                                                    }

                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-blue-50 p-3">

                                                <p className="text-xs text-blue-500">
                                                    Received
                                                </p>

                                                <p className="mt-1 font-semibold text-blue-700">

                                                    {
                                                        item?.receivedQuantity
                                                        ??
                                                        0
                                                    }

                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-green-50 p-3">

                                                <p className="text-xs text-green-500">
                                                    Approved
                                                </p>

                                                <p className="mt-1 font-semibold text-green-700">

                                                    {
                                                        item?.approvedQuantity
                                                        ??
                                                        0
                                                    }

                                                </p>

                                            </div>


                                            <div className="rounded-xl bg-red-50 p-3">

                                                <p className="text-xs text-red-500">
                                                    Rejected
                                                </p>

                                                <p className="mt-1 font-semibold text-red-700">

                                                    {
                                                        item?.rejectedQuantity
                                                        ??
                                                        0
                                                    }

                                                </p>

                                            </div>

                                        </div>


                                        {/* ============================== */}
                                        {/* LOCATION                       */}
                                        {/* ============================== */}

                                        {
                                            (
                                                item?.fromLocationName
                                                ||
                                                item?.fromLocationId
                                            )
                                            && (

                                                <div className="mt-4 rounded-xl border border-slate-100 bg-slate-50 p-3">

                                                    <p className="text-xs text-slate-400">
                                                        Store Location
                                                    </p>

                                                    <p className="mt-1 text-sm font-medium text-slate-700">

                                                        {
                                                            item?.fromLocationName
                                                            ??
                                                            `Location #${item?.fromLocationId}`
                                                        }

                                                    </p>

                                                </div>

                                            )
                                        }


                                        {/* ============================== */}
                                        {/* NOTE                           */}
                                        {/* ============================== */}

                                        {
                                            item?.note && (

                                                <div className="mt-4 rounded-xl bg-slate-50 p-3">

                                                    <p className="text-xs text-slate-400">
                                                        Inspection note
                                                    </p>

                                                    <p className="mt-1 text-sm leading-6 text-slate-700">
                                                        {item.note}
                                                    </p>

                                                </div>

                                            )
                                        }


                                        {/* ============================== */}
                                        {/* EVIDENCE                       */}
                                        {/* ============================== */}

                                        {
                                            item?.evidenceImageUrl && (

                                                <div className="mt-4">

                                                    <p className="mb-2 text-xs text-slate-400">
                                                        Evidence
                                                    </p>

                                                    <a
                                                        href={
                                                            item.evidenceImageUrl
                                                        }
                                                        target="_blank"
                                                        rel="noreferrer"
                                                        className="inline-flex text-sm font-semibold text-[#f25d19] hover:text-[#d94f12]"
                                                    >
                                                        View evidence image →
                                                    </a>

                                                </div>

                                            )
                                        }


                                        {/* ============================== */}
                                        {/* DISCREPANCY                    */}
                                        {/* ============================== */}

                                        {
                                            report && (

                                                <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50/60 p-4">


                                                    <div className="flex flex-wrap items-start justify-between gap-3">

                                                        <div>

                                                            <p className="text-xs font-semibold uppercase tracking-wider text-amber-600">
                                                                Discrepancy
                                                            </p>


                                                            <p className="mt-1 font-semibold text-slate-900">

                                                                {
                                                                    report?.discrepancyType
                                                                    ??
                                                                    "-"
                                                                }

                                                            </p>

                                                        </div>


                                                        <ReceivingStatusBadge
                                                            status={
                                                                report?.status
                                                            }
                                                        />

                                                    </div>


                                                    {
                                                        report?.description
                                                        && (

                                                            <p className="mt-3 text-sm leading-6 text-slate-600">

                                                                {
                                                                    report.description
                                                                }

                                                            </p>

                                                        )
                                                    }


                                                    {
                                                        report?.responsibleParty
                                                        && (

                                                            <div className="mt-3 text-xs text-slate-500">

                                                                Responsible Party:{" "}

                                                                <span className="font-semibold text-slate-700">

                                                                    {
                                                                        report.responsibleParty
                                                                            .replaceAll(
                                                                                "_",
                                                                                " "
                                                                            )
                                                                    }

                                                                </span>

                                                            </div>

                                                        )
                                                    }


                                                    {/* ================== */}
                                                    {/* RESOLVED           */}
                                                    {/* ================== */}

                                                    {
                                                        reportStatus
                                                        === "RESOLVED"
                                                            ? (

                                                                <div className="mt-4 rounded-xl border border-green-100 bg-green-50 p-3 text-sm text-green-700">

                                                                    <p className="font-semibold">
                                                                        Resolved
                                                                    </p>


                                                                    {
                                                                        report?.resolutionNote
                                                                        && (

                                                                            <p className="mt-1 leading-6">

                                                                                {
                                                                                    report.resolutionNote
                                                                                }

                                                                            </p>

                                                                        )
                                                                    }

                                                                </div>

                                                            )


                                                            // ==================
                                                            // STORE MANAGER
                                                            // ==================

                                                            : storeOwned
                                                                ? (

                                                                    <div className="mt-4 rounded-xl border border-blue-100 bg-blue-50 p-3 text-sm text-blue-700">

                                                                        <p className="font-semibold">
                                                                            Store Manager Investigation
                                                                        </p>


                                                                        <p className="mt-1 leading-6">

                                                                            This discrepancy has been sent to Store Manager for investigation.

                                                                        </p>


                                                                        {
                                                                            report?.resolutionNote
                                                                            && (

                                                                                <p className="mt-2 leading-6">

                                                                                    Reason:{" "}

                                                                                    {
                                                                                        report.resolutionNote
                                                                                    }

                                                                                </p>

                                                                            )
                                                                        }

                                                                    </div>

                                                                )


                                                                // ==================
                                                                // WAREHOUSE REVIEW
                                                                // ==================

                                                                : (

                                                                    <div className="mt-4 space-y-3">


                                                                        {
                                                                            warehouseOwned
                                                                            && (

                                                                                <p className="text-xs text-slate-500">

                                                                                    Warehouse Manager must determine responsibility for this discrepancy.

                                                                                </p>

                                                                            )
                                                                        }


                                                                        <textarea
                                                                            value={
                                                                                reasons[
                                                                                    report.id
                                                                                    ]
                                                                                ??
                                                                                ""
                                                                            }
                                                                            onChange={
                                                                                (
                                                                                    event
                                                                                ) =>
                                                                                    setReasons(
                                                                                        (
                                                                                            current
                                                                                        ) => ({
                                                                                            ...current,

                                                                                            [
                                                                                                report.id
                                                                                                ]:
                                                                                            event.target.value,
                                                                                        })
                                                                                    )
                                                                            }
                                                                            rows={3}
                                                                            placeholder="Enter Warehouse Manager review reason..."
                                                                            className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                                                                        />


                                                                        <div className="flex flex-wrap gap-2">


                                                                            <Button
                                                                                variant="outline"
                                                                                disabled={
                                                                                    busyId
                                                                                    === report.id
                                                                                }
                                                                                onClick={() =>
                                                                                    handleSendStore(
                                                                                        report
                                                                                    )
                                                                                }
                                                                            >

                                                                                {
                                                                                    busyId
                                                                                    === report.id
                                                                                        ? "Processing..."
                                                                                        : "Send to Store Manager"
                                                                                }

                                                                            </Button>


                                                                            <Button
                                                                                disabled={
                                                                                    busyId
                                                                                    === report.id
                                                                                }
                                                                                onClick={() =>
                                                                                    handleWarehouse(
                                                                                        report
                                                                                    )
                                                                                }
                                                                            >

                                                                                {
                                                                                    busyId
                                                                                    === report.id
                                                                                        ? "Processing..."
                                                                                        : "Warehouse Responsible"
                                                                                }

                                                                            </Button>

                                                                        </div>

                                                                    </div>

                                                                )
                                                    }

                                                </div>

                                            )
                                        }

                                    </div>
                                );
                            }
                        )
                }

            </div>


            {/* ========================================== */}
            {/* EMPTY ITEMS                                */}
            {/* ========================================== */}

            {
                !(
                    data?.items
                    ??
                    []
                ).length
                && (

                    <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">

                        <p className="font-semibold text-slate-700">
                            No Store Return items
                        </p>

                        <p className="mt-2 text-sm text-slate-500">
                            This Store Return does not contain any product inspection results.
                        </p>

                    </div>

                )
            }

        </div>
    );
}