import Barcode from "react-barcode";


export default function DispatchHandover({
                                             data,
                                             packages,
                                             totalQuantity,

                                             alreadyShipped,
                                             canConfirmHandover,

                                             confirming,
                                             error,
                                             successMessage,

                                             onBack,
                                             onViewManifest,
                                             onConfirm,
                                             onPrintDispatch,
                                         }) {

    const transferCode =
        data?.transferCode ||
        "-";


    return (
        <div
            className="
                mx-auto
                w-full
                max-w-[1050px]
            "
        >

            {/* =================================================
                PAGE HEADER
            ================================================== */}

            <div
                className="
                    mb-6
                    print:hidden
                "
            >

                <button
                    type="button"
                    onClick={
                        onBack
                    }
                    className="
                        mb-4
                        text-sm
                        font-medium
                        text-slate-500
                        transition
                        hover:text-[#f25d19]
                    "
                >
                    ← Back
                </button>


                <div
                    className="
                        flex
                        flex-col
                        gap-4
                        lg:flex-row
                        lg:items-center
                        lg:justify-between
                    "
                >

                    <div>

                        <h1
                            className="
                                text-2xl
                                font-bold
                                text-slate-900
                            "
                        >
                            Dispatch & Handover
                        </h1>


                        <p
                            className="
                                mt-1
                                text-sm
                                text-slate-500
                            "
                        >
                            Confirm goods have left the warehouse
                            and have been handed over to delivery.
                        </p>

                    </div>


                    <div
                        className="
                            flex
                            flex-wrap
                            gap-2
                        "
                    >

                        <button
                            type="button"
                            onClick={
                                onViewManifest
                            }
                            className="
                                rounded-xl
                                border
                                border-slate-300
                                bg-white
                                px-4
                                py-2.5
                                text-sm
                                font-semibold
                                text-slate-700
                                transition
                                hover:bg-slate-50
                            "
                        >
                            View Manifest
                        </button>


                        {alreadyShipped && (

                            <button
                                type="button"
                                onClick={
                                    onPrintDispatch
                                }
                                className="
                                    inline-flex
                                    items-center
                                    gap-2
                                    rounded-xl
                                    border
                                    border-[#f25d19]
                                    bg-white
                                    px-4
                                    py-2.5
                                    text-sm
                                    font-semibold
                                    text-[#f25d19]
                                    transition
                                    hover:bg-[#fff1e9]
                                "
                            >
                                <PrintIcon />

                                Print Dispatch Note
                            </button>

                        )}


                        {!alreadyShipped && (

                            <button
                                type="button"
                                disabled={
                                    !canConfirmHandover
                                    || confirming
                                }
                                onClick={
                                    onConfirm
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
                                    disabled:bg-slate-300
                                "
                            >
                                {
                                    confirming
                                        ? "Confirming..."
                                        : "Confirm Dispatch & Handover"
                                }
                            </button>

                        )}


                        {alreadyShipped && (

                            <div
                                className="
                                    inline-flex
                                    items-center
                                    gap-2
                                    rounded-xl
                                    bg-emerald-50
                                    px-4
                                    py-2.5
                                    text-sm
                                    font-semibold
                                    text-emerald-700
                                "
                            >
                                <CheckIcon />

                                Handover Completed
                            </div>

                        )}

                    </div>

                </div>

            </div>


            {/* =================================================
                SUCCESS
            ================================================== */}

            {successMessage && (

                <div
                    className="
                        mb-5
                        rounded-xl
                        border
                        border-emerald-200
                        bg-emerald-50
                        px-4
                        py-3
                        text-sm
                        font-medium
                        text-emerald-700
                        print:hidden
                    "
                >
                    {successMessage}
                </div>

            )}


            {/* =================================================
                ERROR
            ================================================== */}

            {error && (

                <div
                    className="
                        mb-5
                        rounded-xl
                        border
                        border-red-200
                        bg-red-50
                        px-4
                        py-3
                        text-sm
                        text-red-700
                        print:hidden
                    "
                >
                    {error}
                </div>

            )}


            {/* =================================================
                READINESS
            ================================================== */}

            {!alreadyShipped && (

                <ReadinessBox
                    data={
                        data
                    }
                    packages={
                        packages
                    }
                />

            )}


            {/* =================================================
                MAIN CARD
            ================================================== */}

            <div
                className="
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-6
                    shadow-sm

                    print:rounded-none
                    print:border-0
                    print:p-0
                    print:shadow-none
                "
            >

                {/* =================================================
                    TITLE
                ================================================== */}

                <div
                    className="
                        grid
                        grid-cols-[1fr_auto_1fr]
                        items-start
                        gap-6
                        border-b
                        border-slate-300
                        pb-6
                    "
                >

                    <div />


                    <div
                        className="
                            text-center
                        "
                    >

                        <h2
                            className="
                                whitespace-nowrap
                                text-xl
                                font-bold
                                uppercase
                                tracking-wide
                                text-slate-900
                            "
                        >
                            WAREHOUSE DISPATCH
                        </h2>


                        <p
                            className="
                                mt-2
                                text-sm
                                text-slate-500
                            "
                        >
                            Transfer:{" "}

                            <span
                                className="
                                    font-bold
                                    text-slate-900
                                "
                            >
                                {
                                    transferCode
                                }
                            </span>

                        </p>

                    </div>


                    <div
                        className="
                            flex
                            justify-end
                        "
                    >

                        {transferCode !== "-" && (

                            <Barcode
                                value={
                                    transferCode
                                }
                                format="CODE128"
                                width={0.8}
                                height={24}
                                displayValue
                                fontSize={8}
                                margin={0}
                            />

                        )}

                    </div>

                </div>


                {/* =================================================
                    TRANSFER INFO
                ================================================== */}

                <div
                    className="
                        mt-6
                        grid
                        grid-cols-1
                        gap-4
                        sm:grid-cols-3
                    "
                >

                    <InfoBox
                        label="Transfer No."
                        value={
                            data?.transferCode
                        }
                    />


                    <InfoBox
                        label="Manifest Code"
                        value={
                            data?.manifestCode
                        }
                    />


                    <InfoBox
                        label="Transfer Status"
                        value={
                            formatStatus(
                                data?.transferStatus
                            )
                        }
                    />

                </div>


                {/* =================================================
                    ROUTE
                ================================================== */}

                <div
                    className="
                        mt-4
                        grid
                        grid-cols-1
                        gap-4
                        sm:grid-cols-2
                    "
                >

                    <InfoBox
                        label="Origin Warehouse"
                        value={
                            data?.fromWarehouseName ||
                            "-"
                        }
                    />


                    <InfoBox
                        label="Destination Warehouse"
                        value={
                            data?.toWarehouseName ||
                            "-"
                        }
                    />

                </div>


                {/* =================================================
                    DELIVERY
                ================================================== */}

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
                        label="Shipment Code"
                        value={
                            data?.shipmentCode ||
                            "Not assigned"
                        }
                    />


                    <InfoBox
                        label="Delivery Person"
                        value={
                            data?.driverName ||
                            "Not assigned"
                        }
                    />


                    <InfoBox
                        label="Delivery Status"
                        value={
                            formatStatus(
                                data?.deliveryStatus
                            )
                        }
                    />

                </div>


                {/* =================================================
                    TOTAL
                ================================================== */}

                <div
                    className="
                        mt-4
                        grid
                        grid-cols-1
                        gap-4
                        sm:grid-cols-2
                    "
                >

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


                {/* =================================================
                    PACKAGES
                ================================================== */}

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

                        <h3
                            className="
                                font-bold
                                text-slate-900
                            "
                        >
                            Package List
                        </h3>


                        <p
                            className="
                                mt-1
                                text-sm
                                text-slate-500
                                print:hidden
                            "
                        >
                            Verify all packages before confirming handover.
                        </p>

                    </div>


                    <div
                        className="
                            overflow-hidden
                            rounded-xl
                            border
                            border-slate-300
                            print:rounded-none
                        "
                    >

                        <table
                            className="
                                w-full
                                border-collapse
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
                                    width="70px"
                                >
                                    No.
                                </TableHeader>


                                <TableHeader>
                                    Box Code
                                </TableHeader>


                                <TableHeader
                                    center
                                    width="140px"
                                >
                                    Quantity
                                </TableHeader>


                                <TableHeader
                                    center
                                    width="140px"
                                >
                                    Status
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


                                        <TableCell
                                            center
                                        >
                                            {
                                                pack.totalQuantity ??
                                                0
                                            }
                                        </TableCell>


                                        <TableCell
                                            center
                                        >

                                            <StatusBadge
                                                status={
                                                    pack.status
                                                }
                                            />

                                        </TableCell>

                                    </tr>

                                )
                            )}


                            {packages.length ===
                                0 && (

                                    <tr>

                                        <td
                                            colSpan={4}
                                            className="
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

                </div>


                {/* =================================================
                    HANDOVER RESULT
                ================================================== */}

                {alreadyShipped && (

                    <div
                        className="
                            mt-8
                            rounded-xl
                            border
                            border-slate-300
                            p-5
                        "
                    >

                        <h3
                            className="
                                text-sm
                                font-bold
                                uppercase
                                tracking-wide
                                text-slate-900
                            "
                        >
                            Handover Information
                        </h3>


                        <div
                            className="
                                mt-5
                                grid
                                grid-cols-1
                                gap-5
                                sm:grid-cols-2
                            "
                        >

                            <DocumentField
                                label="Handed Over By"
                                value={
                                    data?.handedOverByName
                                }
                            />


                            <DocumentField
                                label="Handed Over At"
                                value={
                                    formatDateTime(
                                        data?.handedOverAt
                                    )
                                }
                            />


                            <DocumentField
                                label="Delivery Person"
                                value={
                                    data?.driverName
                                }
                            />


                            <DocumentField
                                label="Status"
                                value="Shipped"
                            />

                        </div>

                    </div>

                )}


                {/* =================================================
                    SIGNATURES
                ================================================== */}

                {alreadyShipped && (

                    <div
                        className="
                            mt-14
                            hidden
                            grid-cols-2
                            gap-20
                            print:grid
                        "
                    >

                        <SignatureBox
                            title="Warehouse Representative"
                        />


                        <SignatureBox
                            title="Delivery Person"
                        />

                    </div>

                )}

            </div>

        </div>
    );
}


// =====================================================
// READINESS BOX
// =====================================================

function ReadinessBox({
                          data,
                          packages,
                      }) {

    const packageReady =
        packages.length > 0 &&
        packages.every(
            (item) =>
                item.status === "CHECKED"
                && Boolean(
                    item.sealNumber
                )
        );


    let message =
        "Shipment is ready for dispatch and handover.";

    let ready =
        true;


    if (
        data?.transferStatus !==
        "PACKED"
    ) {

        ready = false;

        message =
            "Packing must be completed before dispatch.";

    } else if (!data?.manifestId) {

        ready = false;

        message =
            "Shipment manifest is required before dispatch.";

    } else if (!packageReady) {

        ready = false;

        message =
            "All packages must be checked and sealed.";

    } else if (!data?.deliveryId) {

        ready = false;

        message =
            "Waiting for delivery assignment.";

    } else if (!data?.driverId) {

        ready = false;

        message =
            "Waiting for a delivery person.";

    } else if (
        data?.deliveryStatus !==
        "ACCEPTED"
    ) {

        ready = false;

        message =
            "Waiting for the delivery person to accept the trip.";
    }


    return (
        <div
            className={`
                mb-5
                rounded-xl
                border
                px-4
                py-3
                text-sm
                print:hidden

                ${
                ready
                    ? `
                            border-emerald-200
                            bg-emerald-50
                            text-emerald-700
                        `
                    : `
                            border-amber-200
                            bg-amber-50
                            text-amber-800
                        `
            }
            `}
        >

            <p
                className="
                    font-semibold
                "
            >
                {
                    ready
                        ? "Ready for Handover"
                        : "Not Ready for Handover"
                }
            </p>


            <p
                className="
                    mt-1
                "
            >
                {message}
            </p>

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
                border-b
                border-r
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
                border-r
                border-t
                border-slate-200
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
// STATUS BADGE
// =====================================================

function StatusBadge({
                         status,
                     }) {

    const shipped =
        status === "SHIPPED";


    const checked =
        status === "CHECKED";


    return (
        <span
            className={`
                inline-flex
                rounded-full
                px-2.5
                py-1
                text-xs
                font-semibold

                ${
                shipped
                    ? `
                            bg-emerald-50
                            text-emerald-700
                        `
                    : checked
                        ? `
                                bg-blue-50
                                text-blue-700
                            `
                        : `
                                bg-amber-50
                                text-amber-700
                            `
            }
            `}
        >
            {
                formatStatus(
                    status
                )
            }
        </span>
    );
}


// =====================================================
// DOCUMENT FIELD
// =====================================================

function DocumentField({
                           label,
                           value,
                       }) {

    return (
        <div>

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
                    text-sm
                    font-semibold
                    text-slate-900
                "
            >
                {
                    value ||
                    "-"
                }
            </p>

        </div>
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
                "
            />

        </div>
    );
}


// =====================================================
// FORMAT STATUS
// =====================================================

function formatStatus(
    status
) {

    if (!status) {
        return "Not assigned";
    }


    return status
        .replaceAll(
            "_",
            " "
        )
        .toLowerCase()
        .replace(
            /\b\w/g,
            (letter) =>
                letter.toUpperCase()
        );
}


// =====================================================
// DATE
// =====================================================

function formatDateTime(
    value
) {

    if (!value) {
        return "-";
    }


    const date =
        new Date(
            value
        );


    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }


    return date.toLocaleString(
        "vi-VN"
    );
}


// =====================================================
// ICONS
// =====================================================

function CheckIcon() {

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
                d="m5 12 4 4L19 6"
            />

        </svg>
    );
}


function PrintIcon() {

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
                d="M7 8V3h10v5M7 17H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2M7 14h10v7H7v-7Z"
            />

        </svg>
    );
}