import {
    useCallback,
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    getShipmentHistory,
} from "../../../api/packages/shipments/shipmentHistory.js";

import PackingStatusBadge
    from "../packing/PackingStatusBadge.jsx";


const PAGE_SIZE = 6;


export default function ShipmentHistoryMain() {

    const navigate =
        useNavigate();


    const [history, setHistory] =
        useState([]);


    const [page, setPage] =
        useState(0);


    const [totalPages, setTotalPages] =
        useState(0);


    const [totalElements, setTotalElements] =
        useState(0);


    const [keyword, setKeyword] =
        useState("");


    const [searchKeyword, setSearchKeyword] =
        useState("");


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");


    // =====================================================
    // LOAD
    // =====================================================

    const loadHistory =
        useCallback(
            async () => {

                try {

                    setLoading(true);

                    setError("");


                    const response =
                        await getShipmentHistory({
                            page,
                            size: PAGE_SIZE,
                            keyword:
                            searchKeyword,
                        });


                    setHistory(
                        Array.isArray(
                            response?.content
                        )
                            ? response.content
                            : []
                    );


                    setTotalPages(
                        response?.totalPages ??
                        0
                    );


                    setTotalElements(
                        response?.totalElements ??
                        0
                    );

                } catch (err) {

                    setHistory([]);

                    setError(
                        getErrorMessage(
                            err,
                            "Failed to load shipment history."
                        )
                    );

                } finally {

                    setLoading(false);

                }
            },

            [
                page,
                searchKeyword,
            ]
        );


    useEffect(() => {

        loadHistory();

    }, [loadHistory]);


    // =====================================================
    // SEARCH
    // =====================================================

    const handleSearch =
        (event) => {

            event.preventDefault();

            setPage(0);

            setSearchKeyword(
                keyword.trim()
            );
        };


    const handleClear =
        () => {

            setKeyword("");

            setSearchKeyword("");

            setPage(0);
        };


    // =====================================================
    // MANIFEST
    // =====================================================

    const viewManifest =
        (transferId) => {

            navigate(
                `/warehouse/manifests/transfer/${transferId}`
            );
        };


    const printManifest =
        (transferId) => {

            navigate(
                `/warehouse/manifests/transfer/${transferId}?print=true`
            );
        };


    return (
        <div
            className="
                mx-auto
                w-full
                max-w-[1600px]
                space-y-6
            "
        >

            {/* HEADER */}

            <div
                className="
                    flex
                    flex-col
                    gap-4
                    lg:flex-row
                    lg:items-end
                    lg:justify-between
                "
            >

                <div>

                    <div
                        className="
                            mb-2
                            flex
                            items-center
                            gap-2
                        "
                    >

                        <span
                            className="
                                h-2.5
                                w-2.5
                                rounded-full
                                bg-[#f25d19]
                            "
                        />

                        <span
                            className="
                                text-xs
                                font-bold
                                uppercase
                                tracking-[0.18em]
                                text-[#d94f12]
                            "
                        >
                            Warehouse Operations
                        </span>

                    </div>


                    <h1
                        className="
                            text-3xl
                            font-bold
                            tracking-tight
                            text-slate-900
                        "
                    >
                        Shipment History
                    </h1>


                    <p
                        className="
                            mt-2
                            text-sm
                            text-slate-500
                        "
                    >
                        View confirmed packing records
                        and print shipment manifests.
                    </p>

                </div>


                <div
                    className="
                        rounded-2xl
                        border
                        border-slate-200
                        bg-white
                        px-5
                        py-3
                        shadow-sm
                    "
                >

                    <p
                        className="
                            text-xs
                            text-slate-400
                        "
                    >
                        Total manifests
                    </p>


                    <p
                        className="
                            mt-1
                            text-2xl
                            font-bold
                            text-slate-900
                        "
                    >
                        {totalElements}
                    </p>

                </div>

            </div>


            {/* SEARCH */}

            <form
                onSubmit={handleSearch}
                className="
                    flex
                    flex-col
                    gap-3
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-4
                    shadow-sm
                    md:flex-row
                "
            >

                <div
                    className="
                        relative
                        flex-1
                    "
                >

                    <div
                        className="
                            pointer-events-none
                            absolute
                            inset-y-0
                            left-0
                            flex
                            items-center
                            pl-4
                            text-slate-400
                        "
                    >
                        <SearchIcon />
                    </div>


                    <input
                        type="text"
                        value={keyword}
                        onChange={
                            (event) =>
                                setKeyword(
                                    event.target.value
                                )
                        }
                        placeholder="Search transfer or manifest code..."
                        className="
                            h-11
                            w-full
                            rounded-xl
                            border
                            border-slate-300
                            bg-white
                            pl-11
                            pr-4
                            text-sm
                            outline-none
                            transition
                            focus:border-[#f25d19]
                            focus:ring-4
                            focus:ring-orange-100
                        "
                    />

                </div>


                {searchKeyword && (

                    <button
                        type="button"
                        onClick={handleClear}
                        className="
                            h-11
                            rounded-xl
                            border
                            border-slate-300
                            bg-white
                            px-5
                            text-sm
                            font-semibold
                            text-slate-600
                            hover:bg-slate-50
                        "
                    >
                        Clear
                    </button>

                )}


                <button
                    type="submit"
                    className="
                        h-11
                        rounded-xl
                        bg-[#f25d19]
                        px-6
                        text-sm
                        font-semibold
                        text-white
                        hover:bg-[#d94f12]
                    "
                >
                    Search
                </button>

            </form>


            {/* ERROR */}

            {error && (

                <div
                    className="
                        rounded-xl
                        border
                        border-red-200
                        bg-red-50
                        px-4
                        py-3
                        text-sm
                        text-red-700
                    "
                >
                    {error}
                </div>

            )}


            {/* CONTENT */}

            {loading ? (

                <LoadingState />

            ) : history.length === 0 ? (

                <EmptyState />

            ) : (

                <>

                    <div
                        className="
                            grid
                            grid-cols-1
                            gap-5
                            lg:grid-cols-2
                            2xl:grid-cols-3
                        "
                    >

                        {history.map(
                            (item) => (

                                <ShipmentHistoryCard
                                    key={
                                        item.manifestId
                                    }
                                    item={item}
                                    onView={() =>
                                        viewManifest(
                                            item.transferId
                                        )
                                    }
                                    onPrint={() =>
                                        printManifest(
                                            item.transferId
                                        )
                                    }
                                />

                            )
                        )}

                    </div>


                    <Pagination
                        page={page}
                        totalPages={
                            totalPages
                        }
                        totalElements={
                            totalElements
                        }
                        pageSize={
                            PAGE_SIZE
                        }
                        onPageChange={
                            setPage
                        }
                    />

                </>

            )}

        </div>
    );
}


// =====================================================
// CARD
// =====================================================

function ShipmentHistoryCard({
                                 item,
                                 onView,
                                 onPrint,
                             }) {

    return (
        <article
            className="
                overflow-hidden
                rounded-2xl
                border
                border-slate-200
                bg-white
                shadow-sm
                transition
                hover:-translate-y-0.5
                hover:shadow-md
            "
        >

            {/* HEADER */}

            <div
                className="
                    flex
                    items-start
                    justify-between
                    gap-4
                    border-b
                    border-slate-100
                    p-5
                "
            >

                <div
                    className="
                        flex
                        min-w-0
                        items-center
                        gap-4
                    "
                >

                    <div
                        className="
                            flex
                            h-12
                            w-12
                            shrink-0
                            items-center
                            justify-center
                            rounded-xl
                            bg-[#fff1e9]
                            text-[#f25d19]
                        "
                    >
                        <ManifestIcon />
                    </div>


                    <div className="min-w-0">

                        <p
                            className="
                                text-xs
                                font-semibold
                                uppercase
                                tracking-wider
                                text-slate-400
                            "
                        >
                            Shipment Manifest
                        </p>


                        <h3
                            className="
                                mt-1
                                truncate
                                text-base
                                font-bold
                                text-slate-900
                            "
                        >
                            {item.manifestCode}
                        </h3>

                    </div>

                </div>


                <PackingStatusBadge
                    status={
                        item.transferStatus
                    }
                />

            </div>


            {/* BODY */}

            <div
                className="
                    space-y-4
                    p-5
                "
            >

                <div
                    className="
                        rounded-xl
                        bg-slate-50
                        p-4
                    "
                >

                    <p
                        className="
                            text-xs
                            text-slate-400
                        "
                    >
                        Transfer
                    </p>


                    <p
                        className="
                            mt-1
                            font-bold
                            text-slate-900
                        "
                    >
                        {
                            item.transferCode ||
                            `#${item.transferId}`
                        }
                    </p>

                </div>


                {/* ROUTE */}

                <div
                    className="
                        rounded-xl
                        border
                        border-slate-100
                        p-4
                    "
                >

                    <p
                        className="
                            text-xs
                            text-slate-400
                        "
                    >
                        Route
                    </p>


                    <div
                        className="
                            mt-2
                            flex
                            items-center
                            gap-2
                            text-sm
                            font-semibold
                            text-slate-800
                        "
                    >

                        <span
                            className="
                                min-w-0
                                truncate
                            "
                        >
                            {
                                item.fromWarehouseName ||
                                "-"
                            }
                        </span>

                        <span
                            className="
                                shrink-0
                                text-[#f25d19]
                            "
                        >
                            →
                        </span>

                        <span
                            className="
                                min-w-0
                                truncate
                            "
                        >
                            {
                                item.toWarehouseName ||
                                "-"
                            }
                        </span>

                    </div>

                </div>


                {/* METRICS */}

                <div
                    className="
                        grid
                        grid-cols-2
                        gap-3
                    "
                >

                    <InfoBox
                        label="Packages"
                        value={
                            item.totalPackages ??
                            0
                        }
                    />


                    <InfoBox
                        label="Quantity"
                        value={
                            item.totalQuantity ??
                            0
                        }
                    />

                </div>


                {/* CREATED */}

                <div
                    className="
                        space-y-2
                        border-t
                        border-slate-100
                        pt-4
                    "
                >

                    <InfoRow
                        label="Confirmed by"
                        value={
                            item.createdByName ||
                            "-"
                        }
                    />


                    <InfoRow
                        label="Created at"
                        value={
                            formatDateTime(
                                item.createdAt
                            )
                        }
                    />


                    <InfoRow
                        label="Manifest status"
                        value={
                            item.manifestStatus ||
                            "-"
                        }
                    />

                </div>


                {/* ACTIONS */}

                <div
                    className="
                        grid
                        grid-cols-2
                        gap-2
                        border-t
                        border-slate-100
                        pt-4
                    "
                >

                    <button
                        type="button"
                        onClick={onView}
                        className="
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
                            hover:bg-orange-50
                        "
                    >
                        View Manifest
                    </button>


                    <button
                        type="button"
                        onClick={onPrint}
                        className="
                            rounded-xl
                            bg-slate-900
                            px-4
                            py-2.5
                            text-sm
                            font-semibold
                            text-white
                            transition
                            hover:bg-slate-800
                        "
                    >
                        Print
                    </button>

                </div>

            </div>

        </article>
    );
}


// =====================================================
// INFO
// =====================================================

function InfoBox({
                     label,
                     value,
                 }) {

    return (
        <div
            className="
                rounded-xl
                bg-slate-50
                p-4
            "
        >

            <p
                className="
                    text-xs
                    text-slate-400
                "
            >
                {label}
            </p>


            <p
                className="
                    mt-1
                    text-lg
                    font-bold
                    text-slate-900
                "
            >
                {value}
            </p>

        </div>
    );
}


function InfoRow({
                     label,
                     value,
                 }) {

    return (
        <div
            className="
                flex
                items-center
                justify-between
                gap-4
            "
        >

            <span
                className="
                    text-xs
                    text-slate-400
                "
            >
                {label}
            </span>


            <span
                className="
                    text-right
                    text-sm
                    font-medium
                    text-slate-700
                "
            >
                {value}
            </span>

        </div>
    );
}


// =====================================================
// PAGINATION
// =====================================================

function Pagination({
                        page,
                        totalPages,
                        totalElements,
                        pageSize,
                        onPageChange,
                    }) {

    if (totalPages <= 1) {
        return null;
    }


    const start =
        page * pageSize + 1;


    const end =
        Math.min(
            (page + 1)
            * pageSize,

            totalElements
        );


    return (
        <div
            className="
                flex
                flex-col
                gap-4
                rounded-2xl
                border
                border-slate-200
                bg-white
                px-5
                py-4
                shadow-sm
                sm:flex-row
                sm:items-center
                sm:justify-between
            "
        >

            <p
                className="
                    text-sm
                    text-slate-500
                "
            >
                Showing{" "}
                <strong>
                    {start}
                </strong>

                {" - "}

                <strong>
                    {end}
                </strong>

                {" of "}

                <strong>
                    {totalElements}
                </strong>
            </p>


            <div
                className="
                    flex
                    items-center
                    gap-2
                "
            >

                <button
                    type="button"
                    disabled={
                        page === 0
                    }
                    onClick={() =>
                        onPageChange(
                            page - 1
                        )
                    }
                    className="
                        rounded-lg
                        border
                        border-slate-200
                        px-4
                        py-2
                        text-sm
                        font-semibold
                        text-slate-600
                        hover:bg-slate-50
                        disabled:cursor-not-allowed
                        disabled:opacity-40
                    "
                >
                    Previous
                </button>


                <span
                    className="
                        px-3
                        text-sm
                        font-semibold
                        text-slate-700
                    "
                >
                    {page + 1}
                    {" / "}
                    {totalPages}
                </span>


                <button
                    type="button"
                    disabled={
                        page >=
                        totalPages - 1
                    }
                    onClick={() =>
                        onPageChange(
                            page + 1
                        )
                    }
                    className="
                        rounded-lg
                        border
                        border-slate-200
                        px-4
                        py-2
                        text-sm
                        font-semibold
                        text-slate-600
                        hover:bg-slate-50
                        disabled:cursor-not-allowed
                        disabled:opacity-40
                    "
                >
                    Next
                </button>

            </div>

        </div>
    );
}


// =====================================================
// STATES
// =====================================================

function LoadingState() {

    return (
        <div
            className="
                flex
                min-h-[350px]
                flex-col
                items-center
                justify-center
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
                Loading shipment history...
            </p>

        </div>
    );
}


function EmptyState() {

    return (
        <div
            className="
                rounded-2xl
                border
                border-dashed
                border-slate-300
                bg-white
                px-6
                py-16
                text-center
            "
        >

            <div
                className="
                    mx-auto
                    flex
                    h-14
                    w-14
                    items-center
                    justify-center
                    rounded-2xl
                    bg-orange-50
                    text-[#f25d19]
                "
            >
                <ManifestIcon />
            </div>


            <h3
                className="
                    mt-4
                    text-base
                    font-bold
                    text-slate-900
                "
            >
                No shipment history
            </h3>


            <p
                className="
                    mt-2
                    text-sm
                    text-slate-500
                "
            >
                Confirmed packing records
                will appear here.
            </p>

        </div>
    );
}


// =====================================================
// UTIL
// =====================================================

function formatDateTime(
    value
) {

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
        "vi-VN"
    );
}


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
// ICONS
// =====================================================

function SearchIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-5 w-5"
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
                d="m20 20-4-4"
            />

        </svg>
    );
}


function ManifestIcon() {

    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-6 w-6"
        >

            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M6 3h9l3 3v15H6V3Zm9 0v4h4M9 11h6M9 15h6M9 19h4"
            />

        </svg>
    );
}