import {
    useCallback,
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    getPackingConfirmations,
} from "../../../api/packages/packing/managerPacking.js";

import PackingStatusBadge
    from "./PackingStatusBadge";


const PAGE_SIZE = 6;


export default function PackingConfirmationMain() {

    const navigate =
        useNavigate();


    const [transfers, setTransfers] =
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


    const loadTransfers =
        useCallback(async () => {

            try {

                setLoading(true);

                setError("");


                const response =
                    await getPackingConfirmations({
                        page,
                        size: PAGE_SIZE,
                        keyword: searchKeyword,
                    });


                setTransfers(
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

                setError(
                    getErrorMessage(
                        err,
                        "Failed to load packing confirmations."
                    )
                );

                setTransfers([]);

            } finally {

                setLoading(false);

            }

        }, [
            page,
            searchKeyword,
        ]);


    useEffect(() => {

        loadTransfers();

    }, [loadTransfers]);


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
                            text-slate-900
                        "
                    >
                        Packing Confirmation
                    </h1>


                    <p
                        className="
                            mt-2
                            text-sm
                            text-slate-500
                        "
                    >
                        Review transfers waiting
                        for manager packing confirmation.
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
                            font-medium
                            text-slate-400
                        "
                    >
                        Waiting for review
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
                        value={keyword}
                        onChange={(event) =>
                            setKeyword(
                                event.target.value
                            )
                        }
                        placeholder="Search by transfer code..."
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
                            text-slate-900
                            outline-none
                            transition
                            placeholder:text-slate-400
                            focus:border-[#f25d19]
                            focus:ring-4
                            focus:ring-orange-100
                        "
                    />

                </div>


                {searchKeyword && (
                    <button
                        type="button"
                        onClick={
                            handleClear
                        }
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
                        transition
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

            ) : transfers.length === 0 ? (

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

                        {transfers.map(
                            (transfer) => (

                                <TransferCard
                                    key={
                                        transfer.transferId
                                    }
                                    transfer={
                                        transfer
                                    }
                                    onReview={() =>
                                        navigate(
                                            `/warehouse/packing/${transfer.transferId}`
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


function TransferCard({
                          transfer,
                          onReview,
                      }) {

    const packages =
        Array.isArray(
            transfer?.packages
        )
            ? transfer.packages
            : [];


    const sealedCount =
        packages.filter(
            (item) =>
                item.sealed
        ).length;


    const packedCount =
        packages.filter(
            (item) =>
                item.status === "PACKED" ||
                item.status === "CHECKED"
        ).length;


    const ready =
        transfer.allPackagesPacked === true &&
        transfer.allPackagesSealed === true &&
        transfer.quantityMatched === true &&
        packages.length > 0;


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
                        <PackageIcon />
                    </div>


                    <div
                        className="
                            min-w-0
                        "
                    >

                        <p
                            className="
                                text-xs
                                font-semibold
                                uppercase
                                tracking-wider
                                text-slate-400
                            "
                        >
                            Stock Transfer
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
                            {
                                transfer.transferCode ||
                                `Transfer #${transfer.transferId}`
                            }
                        </h3>

                    </div>

                </div>


                <PackingStatusBadge
                    status={
                        transfer.transferStatus
                    }
                />

            </div>


            <div
                className="
                    space-y-4
                    p-5
                "
            >

                <div
                    className="
                        grid
                        grid-cols-2
                        gap-3
                    "
                >

                    <MetricBox
                        label="Packages"
                        value={
                            packages.length
                        }
                        subtext={
                            `${packedCount}/${packages.length} packed`
                        }
                    />


                    <MetricBox
                        label="Sealed"
                        value={
                            `${sealedCount}/${packages.length}`
                        }
                        subtext={
                            transfer.allPackagesSealed
                                ? "All sealed"
                                : "Waiting"
                        }
                    />


                    <MetricBox
                        label="Expected Qty"
                        value={
                            transfer.expectedQuantity ??
                            0
                        }
                    />


                    <MetricBox
                        label="Packed Qty"
                        value={
                            transfer.packedQuantity ??
                            0
                        }
                        subtext={
                            transfer.quantityMatched
                                ? "Matched"
                                : "Mismatch"
                        }
                    />

                </div>


                <div
                    className={`
                        flex
                        items-center
                        gap-3
                        rounded-xl
                        border
                        px-4
                        py-3
                        ${
                        ready
                            ? "border-emerald-200 bg-emerald-50"
                            : "border-amber-200 bg-amber-50"
                    }
                    `}
                >

                    <div
                        className={`
                            flex
                            h-8
                            w-8
                            shrink-0
                            items-center
                            justify-center
                            rounded-full
                            text-sm
                            font-bold
                            ${
                            ready
                                ? "bg-emerald-100 text-emerald-700"
                                : "bg-amber-100 text-amber-700"
                        }
                        `}
                    >
                        {ready
                            ? "✓"
                            : "!"}
                    </div>


                    <div>

                        <p
                            className={`
                                text-sm
                                font-semibold
                                ${
                                ready
                                    ? "text-emerald-800"
                                    : "text-amber-800"
                            }
                            `}
                        >
                            {ready
                                ? "Ready for review"
                                : "Packing incomplete"}
                        </p>

                        <p
                            className={`
                                mt-0.5
                                text-xs
                                ${
                                ready
                                    ? "text-emerald-600"
                                    : "text-amber-600"
                            }
                            `}
                        >
                            {ready
                                ? "All conditions passed"
                                : "Review package status"}
                        </p>

                    </div>

                </div>


                <button
                    type="button"
                    onClick={onReview}
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
                    "
                >
                    Review Packages
                </button>

            </div>

        </article>
    );
}


function MetricBox({
                       label,
                       value,
                       subtext,
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

            {subtext && (
                <p
                    className="
                        mt-1
                        text-xs
                        text-slate-500
                    "
                >
                    {subtext}
                </p>
            )}

        </div>
    );
}


function Pagination({
                        page,
                        totalPages,
                        totalElements,
                        pageSize,
                        onPageChange,
                    }) {

    if (
        totalPages <= 1
    ) {
        return null;
    }


    const start =
        page * pageSize + 1;


    const end =
        Math.min(
            (page + 1) *
            pageSize,

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
                <strong
                    className="
                        text-slate-800
                    "
                >
                    {start}
                </strong>

                {" - "}

                <strong
                    className="
                        text-slate-800
                    "
                >
                    {end}
                </strong>

                {" of "}

                <strong
                    className="
                        text-slate-800
                    "
                >
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
                Loading packing confirmations...
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
                <PackageIcon />
            </div>


            <h3
                className="
                    mt-4
                    text-base
                    font-bold
                    text-slate-900
                "
            >
                No transfers waiting
            </h3>


            <p
                className="
                    mt-2
                    text-sm
                    text-slate-500
                "
            >
                No PACKING transfers
                are waiting for manager review.
            </p>

        </div>
    );
}


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


function PackageIcon() {

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
                d="M21 8 12 3 3 8l9 5 9-5Zm-18 0v8l9 5 9-5V8M12 13v8"
            />

        </svg>
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