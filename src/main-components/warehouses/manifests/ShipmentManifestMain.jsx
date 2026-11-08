import {
    useEffect,
    useState,
} from "react";

import {
    useLocation,
    useNavigate,
    useParams,
} from "react-router-dom";

import Barcode from "react-barcode";

import {
    getShipment,
} from "../../../api/packages/packing/managerPacking.js";


export default function ShipmentManifestMain() {

    const { transferId } =
        useParams();

    const navigate =
        useNavigate();

    const location =
        useLocation();

    const [data, setData] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState("");


    // =====================================================
    // LOAD MANIFEST
    // =====================================================

    useEffect(() => {

        const loadShipment =
            async () => {

                try {

                    setLoading(true);
                    setError("");

                    const result =
                        await getShipment(
                            transferId
                        );

                    setData(
                        result
                    );

                } catch (err) {

                    setData(
                        null
                    );

                    setError(
                        getErrorMessage(
                            err,
                            "Failed to load shipment manifest."
                        )
                    );

                } finally {

                    setLoading(false);

                }
            };


        loadShipment();

    }, [
        transferId,
    ]);


    // =====================================================
    // AUTO PRINT
    // =====================================================

    useEffect(() => {

        if (!data) {
            return;
        }

        const params =
            new URLSearchParams(
                location.search
            );

        const shouldPrint =
            params.get("print")
            === "true";

        if (!shouldPrint) {
            return;
        }

        const timer =
            window.setTimeout(
                () => {

                    window.print();

                },
                300
            );

        return () => {

            window.clearTimeout(
                timer
            );

        };

    }, [
        data,
        location.search,
    ]);


    // =====================================================
    // LOADING
    // =====================================================

    if (loading) {

        return (
            <div
                className="
                    flex
                    min-h-[400px]
                    items-center
                    justify-center
                    print:hidden
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
                        Loading shipment manifest...
                    </p>

                </div>

            </div>
        );
    }


    // =====================================================
    // ERROR
    // =====================================================

    if (!data) {

        return (
            <div
                className="
                    rounded-xl
                    border
                    border-red-200
                    bg-red-50
                    p-5
                    text-sm
                    text-red-700
                    print:hidden
                "
            >

                <p className="font-bold">
                    Unable to load manifest
                </p>

                <p className="mt-1">
                    {
                        error ||
                        "Shipment manifest not found."
                    }
                </p>

                <button
                    type="button"
                    onClick={() =>
                        navigate(
                            "/warehouse/shipment-history"
                        )
                    }
                    className="
                        mt-4
                        rounded-xl
                        border
                        border-red-300
                        bg-white
                        px-4
                        py-2
                        font-semibold
                        text-red-700
                        hover:bg-red-100
                    "
                >
                    Back to Shipment History
                </button>

            </div>
        );
    }


    // =====================================================
    // DATA
    // =====================================================

    const packages =
        Array.isArray(
            data?.packages
        )
            ? data.packages
            : [];


    const totalQuantity =
        packages.reduce(
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


    const fromWarehouseName =
        data?.fromWarehouseName ||
        "-";


    const toWarehouseName =
        data?.toWarehouseName ||
        "-";


    const transferCode =
        data?.transferCode ||
        "-";


    // =====================================================
    // MAIN
    // =====================================================

    return (
        <div
            className="
                mx-auto
                w-full
                max-w-[1000px]
            "
        >

            {/* =========================================
                ACTION BAR
            ========================================== */}

            <div
                className="
                    mb-6
                    flex
                    flex-col
                    gap-3
                    print:hidden
                    sm:flex-row
                    sm:items-center
                    sm:justify-between
                "
            >

                <button
                    type="button"
                    onClick={() =>
                        navigate(
                            "/warehouse/shipment-history"
                        )
                    }
                    className="
                        inline-flex
                        items-center
                        gap-2
                        text-sm
                        font-medium
                        text-slate-500
                        transition
                        hover:text-[#f25d19]
                    "
                >
                    ← Back to Shipment History
                </button>


                <button
                    type="button"
                    onClick={() =>
                        window.print()
                    }
                    className="
                        inline-flex
                        items-center
                        justify-center
                        gap-2
                        rounded-xl
                        bg-slate-900
                        px-5
                        py-2.5
                        text-sm
                        font-semibold
                        text-white
                        transition
                        hover:bg-slate-800
                    "
                >
                    <PrintIcon />

                    Print Manifest
                </button>

            </div>


            {/* =========================================
                DOCUMENT
            ========================================== */}

            <div
                className="
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-8
                    shadow-sm

                    print:rounded-none
                    print:border-0
                    print:p-0
                    print:shadow-none
                "
            >

                {/* =====================================
    HEADER
====================================== */}

                <div
                    className="
        border-b
        border-slate-300
        pb-6
    "
                >

                    <div
                        className="
            grid
            grid-cols-[1fr_auto_1fr]
            items-start
            gap-4
        "
                    >

                        {/* LEFT EMPTY SPACE */}

                        <div />


                        {/* DOCUMENT TITLE */}

                        <div
                            className="
                text-center
            "
                        >

                            <h1
                                className="
                    text-2xl
                    font-bold
                    uppercase
                    tracking-wide
                    text-slate-900
                    whitespace-nowrap
                "
                            >
                                SHIPMENT MANIFEST
                            </h1>


                            <p
                                className="
                    mt-2
                    text-sm
                    text-slate-500
                "
                            >
                                Manifest Code:{" "}

                                <span
                                    className="
                        font-bold
                        text-slate-900
                    "
                                >
                    {
                        data.manifestCode ||
                        "-"
                    }
                </span>

                            </p>

                        </div>


                        {/* BARCODE */}

                        <div
                            className="
                flex
                justify-end
            "
                        >

                            {transferCode !== "-" && (

                                <div
                                    className="
                        flex
                        flex-col
                        items-center
                        overflow-hidden
                    "
                                >

                                    <Barcode
                                        value={
                                            transferCode
                                        }
                                        format="CODE128"
                                        width={0.8}
                                        height={24}
                                        displayValue={true}
                                        fontSize={8}
                                        margin={0}
                                    />

                                </div>

                            )}

                        </div>

                    </div>

                </div>


                {/* =====================================
                    WAREHOUSE INFO
                ====================================== */}

                <div
                    className="
                        mt-6
                        grid
                        grid-cols-1
                        gap-4
                        sm:grid-cols-2
                    "
                >

                    <InfoBox
                        label="Origin Warehouse"
                        value={
                            fromWarehouseName
                        }
                    />


                    <InfoBox
                        label="Destination Warehouse"
                        value={
                            toWarehouseName
                        }
                    />

                </div>


                {/* =====================================
                    TRANSFER INFO
                ====================================== */}

                <div
                    className="
                        mt-4
                        grid
                        grid-cols-1
                        gap-4
                        sm:grid-cols-3
                    "
                >

                    <InfoBox
                        label="Transfer No."
                        value={
                            transferCode
                        }
                    />


                    <InfoBox
                        label="Total Packages"
                        value={
                            packages.length
                        }
                    />


                    <InfoBox
                        label="Total Quantity"
                        value={
                            totalQuantity
                        }
                    />

                </div>


                {/* =====================================
                    PACKAGE LIST
                ====================================== */}

                <div
                    className="
                        mt-8
                    "
                >

                    <div
                        className="
                            mb-3
                        "
                    >

                        <h2
                            className="
                                text-base
                                font-bold
                                text-slate-900
                            "
                        >
                            Package List
                        </h2>


                        <p
                            className="
                                mt-1
                                text-sm
                                text-slate-500
                                print:hidden
                            "
                        >
                            Scan the box code to view
                            the products inside.
                        </p>

                    </div>


                    <table
                        className="
                            w-full
                            border-collapse
                            border
                            border-slate-300
                        "
                    >

                        <thead>

                        <tr
                            className="
                                    bg-slate-100
                                "
                        >

                            <TableHeader
                                center
                                width="80px"
                            >
                                No.
                            </TableHeader>


                            <TableHeader>
                                Box Code
                            </TableHeader>

                        </tr>

                        </thead>


                        <tbody>

                        {packages.map(
                            (
                                pack,
                                index
                            ) => (

                                <tr
                                    key={
                                        pack.packageId ??
                                        index
                                    }
                                >

                                    <TableCell
                                        center
                                    >
                                        {
                                            index + 1
                                        }
                                    </TableCell>


                                    <TableCell
                                        bold
                                    >
                                        {
                                            pack.sealNumber ||
                                            "-"
                                        }
                                    </TableCell>

                                </tr>

                            )
                        )}


                        {packages.length ===
                            0 && (

                                <tr>

                                    <td
                                        colSpan={2}
                                        className="
                                            border
                                            border-slate-300
                                            px-4
                                            py-10
                                            text-center
                                            text-sm
                                            text-slate-500
                                        "
                                    >
                                        No packages found.
                                    </td>

                                </tr>

                            )}

                        </tbody>

                    </table>

                </div>


                {/* =====================================
                    SIGNATURES
                ====================================== */}

                <div
                    className="
                        mt-14
                        grid
                        grid-cols-1
                        gap-12
                        sm:grid-cols-3
                    "
                >

                    <SignatureBox
                        title="Sender"
                    />


                    <SignatureBox
                        title="Delivery Person"
                    />


                    <SignatureBox
                        title="Receiver"
                    />

                </div>

            </div>

        </div>
    );
}


// =====================================================
// INFO BOX
// =====================================================

function InfoBox({
                     label,
                     value,
                 }) {

    return (
        <div
            className="
                rounded-xl
                border
                border-slate-200
                bg-slate-50
                p-4
                print:bg-white
            "
        >

            <p
                className="
                    text-xs
                    font-semibold
                    uppercase
                    tracking-wide
                    text-slate-400
                "
            >
                {label}
            </p>


            <p
                className="
                    mt-1
                    text-base
                    font-bold
                    text-slate-900
                "
            >
                {
                    value ??
                    "-"
                }
            </p>

        </div>
    );
}


// =====================================================
// TABLE HEADER
// =====================================================

function TableHeader({
                         children,
                         center = false,
                         width,
                     }) {

    return (
        <th
            style={
                width
                    ? {
                        width,
                    }
                    : undefined
            }
            className={`
                border
                border-slate-300
                px-4
                py-3
                text-sm
                font-semibold
                text-slate-700
                ${
                center
                    ? "text-center"
                    : "text-left"
            }
            `}
        >
            {children}
        </th>
    );
}


// =====================================================
// TABLE CELL
// =====================================================

function TableCell({
                       children,
                       center = false,
                       bold = false,
                   }) {

    return (
        <td
            className={`
                border
                border-slate-300
                px-4
                py-3
                text-sm
                ${
                center
                    ? "text-center"
                    : "text-left"
            }
                ${
                bold
                    ? "font-semibold text-slate-900"
                    : "text-slate-700"
            }
            `}
        >
            {children}
        </td>
    );
}


// =====================================================
// SIGNATURE
// =====================================================

function SignatureBox({
                          title,
                      }) {

    return (
        <div
            className="
                text-center
            "
        >

            <p
                className="
                    text-sm
                    font-bold
                    text-slate-900
                "
            >
                {title}
            </p>


            <p
                className="
                    mt-1
                    text-xs
                    italic
                    text-slate-400
                "
            >
                Signature and full name
            </p>


            <div
                className="
                    mt-20
                    border-t
                    border-slate-400
                    pt-2
                "
            >

                <span
                    className="
                        text-xs
                        text-slate-400
                    "
                >
                    Signature
                </span>

            </div>

        </div>
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
        response?.message ||
        response?.error ||
        fallback
    );
}


// =====================================================
// PRINT ICON
// =====================================================

function PrintIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="
                h-5
                w-5
            "
        >

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M7 8V3h10v5M7 17H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2M7 14h10v7H7v-7Z"
            />

        </svg>
    );
}