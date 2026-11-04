import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
} from "react-router-dom";

import {
    confirmPacking,
    getPackingResult,
} from "../../../api/packages/packing/managerPacking.js";

import PackingStatusBadge
    from "./PackingStatusBadge";

import PackingSummaryCards
    from "./PackingSummaryCards";

import PackingPackageCards
    from "./PackingPackageCards.jsx";


export default function PackingConfirmationDetailMain() {
    const { transferId } = useParams();

    const navigate = useNavigate();

    const [data, setData] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [confirming, setConfirming] =
        useState(false);

    const [error, setError] =
        useState("");


    const loadData =
        useCallback(async () => {
            try {
                setLoading(true);
                setError("");

                const result =
                    await getPackingResult(
                        transferId
                    );

                setData(result);
            } catch (err) {
                setError(
                    getErrorMessage(
                        err,
                        "Failed to load packing information."
                    )
                );
            } finally {
                setLoading(false);
            }
        }, [transferId]);


    useEffect(() => {
        loadData();
    }, [loadData]);


    const packages =
        useMemo(
            () =>
                Array.isArray(
                    data?.packages
                )
                    ? data.packages
                    : [],
            [data]
        );


    const confirmed =
        data?.transferStatus === "PACKED" ||
        Boolean(data?.manifestId);


    const canConfirm =
        data?.transferStatus === "PACKING" &&
        data?.allPackagesPacked === true &&
        data?.allPackagesSealed === true &&
        data?.quantityMatched === true &&
        packages.length > 0;


    const handleConfirm =
        async () => {
            if (!canConfirm) {
                return;
            }

            const accepted =
                window.confirm(
                    "Confirm packing for this transfer?\n\n" +
                    "All packages will be marked CHECKED and a shipment manifest will be created."
                );

            if (!accepted) {
                return;
            }

            try {
                setConfirming(true);
                setError("");

                const result =
                    await confirmPacking(
                        transferId
                    );

                setData(result);
            } catch (err) {
                setError(
                    getErrorMessage(
                        err,
                        "Failed to confirm packing."
                    )
                );
            } finally {
                setConfirming(false);
            }
        };


    if (loading) {
        return <LoadingState />;
    }


    if (!data) {
        return (
            <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
                {error ||
                    "Packing information not found."}
            </div>
        );
    }


    return (
        <div className="mx-auto w-full max-w-[1600px] space-y-6">

            {/* TOP */}
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

                <div>
                    <button
                        type="button"
                        onClick={() =>
                            navigate(
                                "/warehouse/packing"
                            )
                        }
                        className="mb-3 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-[#f25d19]"
                    >
                        ← Back
                    </button>

                    <div className="flex flex-wrap items-center gap-3">
                        <h1 className="text-2xl font-bold text-slate-900">
                            {data.transferCode ||
                                `Transfer #${data.transferId}`}
                        </h1>

                        <PackingStatusBadge
                            status={
                                data.transferStatus
                            }
                        />
                    </div>

                    <p className="mt-2 text-sm text-slate-500">
                        Review package contents and verify all packing conditions before shipment.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={loadData}
                    className="h-10 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
                >
                    Refresh
                </button>
            </div>


            {error && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}


            {/* SUMMARY */}
            <PackingSummaryCards
                data={data}
            />


            {/* MAIN CONTENT */}
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">

                {/* LEFT */}
                <div className="min-w-0 space-y-6">

                    {/* CHECKS */}
                    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">

                        <div className="border-b border-slate-100 px-6 py-5">
                            <h2 className="text-lg font-bold text-slate-900">
                                Packing Validation
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                The transfer can only be confirmed when all checks are valid.
                            </p>
                        </div>

                        <div className="grid grid-cols-1 gap-3 p-6 md:grid-cols-3">
                            <ValidationItem
                                title="Packages"
                                description="All packages packed"
                                valid={
                                    data.allPackagesPacked
                                }
                            />

                            <ValidationItem
                                title="Seal"
                                description="All packages sealed"
                                valid={
                                    data.allPackagesSealed
                                }
                            />

                            <ValidationItem
                                title="Quantity"
                                description="Packed quantity matched"
                                valid={
                                    data.quantityMatched
                                }
                            />
                        </div>
                    </section>


                    {/* PACKAGES */}
                    <section>
                        <div className="mb-4 flex items-end justify-between">
                            <div>
                                <h2 className="text-lg font-bold text-slate-900">
                                    Package Details
                                </h2>

                                <p className="mt-1 text-sm text-slate-500">
                                    Click a package to view the products inside.
                                </p>
                            </div>

                            <span className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600">
                                {packages.length} packages
                            </span>
                        </div>

                        <PackingPackageCards
                            packages={packages}
                        />
                    </section>
                </div>


                {/* RIGHT CONTROL PANEL */}
                <aside className="space-y-4 xl:sticky xl:top-6 xl:self-start">

                    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

                        <div className="bg-slate-900 px-5 py-5 text-white">
                            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                                Manager Control
                            </p>

                            <h3 className="mt-2 text-lg font-bold">
                                Packing Confirmation
                            </h3>
                        </div>


                        <div className="space-y-5 p-5">

                            <ControlRow
                                label="Transfer"
                                value={
                                    data.transferCode ||
                                    `#${data.transferId}`
                                }
                            />

                            <ControlRow
                                label="Status"
                                custom={
                                    <PackingStatusBadge
                                        status={
                                            data.transferStatus
                                        }
                                    />
                                }
                            />

                            <ControlRow
                                label="Expected"
                                value={
                                    data.expectedQuantity ??
                                    0
                                }
                            />

                            <ControlRow
                                label="Packed"
                                value={
                                    data.packedQuantity ??
                                    0
                                }
                            />


                            <div className="border-t border-slate-100 pt-5">

                                {!confirmed ? (
                                    <>
                                        <button
                                            type="button"
                                            onClick={
                                                handleConfirm
                                            }
                                            disabled={
                                                !canConfirm ||
                                                confirming
                                            }
                                            className="
                                                w-full
                                                rounded-xl
                                                bg-[#f25d19]
                                                px-4
                                                py-3
                                                text-sm
                                                font-semibold
                                                text-white
                                                transition
                                                hover:bg-[#d94f12]
                                                disabled:cursor-not-allowed
                                                disabled:bg-slate-300
                                            "
                                        >
                                            {confirming
                                                ? "Confirming..."
                                                : "Confirm Packing"}
                                        </button>

                                        {!canConfirm && (
                                            <p className="mt-3 text-center text-xs leading-5 text-slate-400">
                                                Resolve all validation issues before confirmation.
                                            </p>
                                        )}
                                    </>
                                ) : (
                                    <div className="rounded-xl bg-emerald-50 p-4">
                                        <div className="flex items-center gap-3">
                                            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-100 font-bold text-emerald-700">
                                                ✓
                                            </div>

                                            <div>
                                                <p className="text-sm font-bold text-emerald-800">
                                                    Confirmed
                                                </p>

                                                <p className="text-xs text-emerald-600">
                                                    Packing verification completed
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>


                    {/* MANIFEST */}
                    {data.manifestId && (
                        <div className="rounded-2xl border border-orange-200 bg-[#fff8f4] p-5">

                            <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#d94f12]">
                                Shipment Manifest
                            </p>

                            <p className="mt-2 text-lg font-bold text-slate-900">
                                {data.manifestCode}
                            </p>

                            <p className="mt-1 text-xs leading-5 text-slate-500">
                                Manifest created after packing confirmation.
                            </p>


                            <div className="mt-4 space-y-2">

                                <button
                                    type="button"
                                    onClick={() =>
                                        navigate(
                                            `/warehouse/manifests/transfer/${data.transferId}`
                                        )
                                    }
                                    className="w-full rounded-xl border border-[#f25d19] bg-white px-4 py-2.5 text-sm font-semibold text-[#f25d19] transition hover:bg-orange-50"
                                >
                                    View Manifest
                                </button>

                                <button
                                    type="button"
                                    onClick={() =>
                                        navigate(
                                            `/warehouse/manifests/transfer/${data.transferId}`
                                        )
                                    }
                                    className="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800"
                                >
                                    Print Shipment Manifest
                                </button>

                            </div>
                        </div>
                    )}
                </aside>
            </div>
        </div>
    );
}


function ValidationItem({
                            title,
                            description,
                            valid,
                        }) {
    return (
        <div
            className={`
                rounded-xl
                border
                p-4
                ${
                valid
                    ? "border-emerald-200 bg-emerald-50"
                    : "border-red-200 bg-red-50"
            }
            `}
        >
            <div className="flex items-center gap-3">

                <div
                    className={`
                        flex
                        h-9
                        w-9
                        items-center
                        justify-center
                        rounded-full
                        text-sm
                        font-bold
                        ${
                        valid
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-red-100 text-red-600"
                    }
                    `}
                >
                    {valid ? "✓" : "!"}
                </div>

                <div>
                    <p className="text-sm font-bold text-slate-900">
                        {title}
                    </p>

                    <p className="mt-0.5 text-xs text-slate-500">
                        {description}
                    </p>
                </div>
            </div>
        </div>
    );
}


function ControlRow({
                        label,
                        value,
                        custom,
                    }) {
    return (
        <div className="flex items-center justify-between gap-4">
            <span className="text-sm text-slate-500">
                {label}
            </span>

            {custom || (
                <span className="text-sm font-semibold text-slate-900">
                    {value}
                </span>
            )}
        </div>
    );
}


function LoadingState() {
    return (
        <div className="flex min-h-[450px] flex-col items-center justify-center gap-3">

            <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-[#f25d19]" />

            <p className="text-sm text-slate-500">
                Loading packing review...
            </p>
        </div>
    );
}


function getErrorMessage(
    error,
    fallback
) {
    const response =
        error?.response?.data;

    if (typeof response === "string") {
        return response;
    }

    return (
        response?.message ||
        response?.error ||
        fallback
    );
}