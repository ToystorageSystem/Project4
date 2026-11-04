export default function DispatchHandoverList({
                                                 items,
                                                 loading,
                                                 error,
                                                 keyword,
                                                 onKeywordChange,
                                                 onSearch,
                                                 onView,
                                             }) {

    return (
        <div className="w-full">

            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-900">
                    Dispatch & Handover
                </h1>

                <p className="mt-1 text-sm text-slate-500">
                    Review shipments waiting for warehouse dispatch
                    and handover.
                </p>
            </div>


            <div
                className="
                    mb-5
                    flex
                    flex-col
                    gap-3
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-4
                    sm:flex-row
                    sm:items-center
                "
            >

                <input
                    type="text"
                    value={keyword}
                    onChange={(event) =>
                        onKeywordChange(
                            event.target.value
                        )
                    }
                    onKeyDown={(event) => {
                        if (event.key === "Enter") {
                            onSearch();
                        }
                    }}
                    placeholder="Search transfer code..."
                    className="
                        w-full
                        rounded-xl
                        border
                        border-slate-300
                        px-4
                        py-2.5
                        text-sm
                        outline-none
                        transition
                        focus:border-[#f25d19]
                    "
                />

                <button
                    type="button"
                    onClick={onSearch}
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
                    "
                >
                    Search
                </button>

            </div>


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
                    "
                >
                    {error}
                </div>

            )}


            <div
                className="
                    overflow-hidden
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    shadow-sm
                "
            >

                <table className="w-full border-collapse">

                    <thead>
                    <tr className="bg-slate-50">

                        <TableHeader>
                            Transfer
                        </TableHeader>

                        <TableHeader>
                            Manifest
                        </TableHeader>

                        <TableHeader>
                            Destination
                        </TableHeader>

                        <TableHeader>
                            Driver
                        </TableHeader>

                        <TableHeader>
                            Delivery Status
                        </TableHeader>

                        <TableHeader center>
                            Packages
                        </TableHeader>

                        <TableHeader center>
                            Action
                        </TableHeader>

                    </tr>
                    </thead>


                    <tbody>

                    {loading && (

                        <tr>
                            <td
                                colSpan={7}
                                className="
                                        px-4
                                        py-12
                                        text-center
                                        text-sm
                                        text-slate-500
                                    "
                            >
                                Loading dispatch shipments...
                            </td>
                        </tr>

                    )}


                    {!loading &&
                        items.length === 0 && (

                            <tr>
                                <td
                                    colSpan={7}
                                    className="
                                        px-4
                                        py-12
                                        text-center
                                        text-sm
                                        text-slate-500
                                    "
                                >
                                    No shipments waiting for handover.
                                </td>
                            </tr>

                        )}


                    {!loading &&
                        items.map((item) => (

                            <tr
                                key={item.transferId}
                                className="
                                        border-t
                                        border-slate-100
                                        transition
                                        hover:bg-slate-50
                                    "
                            >

                                <TableCell bold>
                                    {
                                        item.transferCode ||
                                        "-"
                                    }
                                </TableCell>


                                <TableCell>
                                    {
                                        item.manifestCode ||
                                        "-"
                                    }
                                </TableCell>


                                <TableCell>
                                    {
                                        item.toWarehouseName ||
                                        "-"
                                    }
                                </TableCell>


                                <TableCell>
                                    {
                                        item.driverName ||
                                        "Not assigned"
                                    }
                                </TableCell>


                                <TableCell>
                                    <StatusBadge
                                        status={
                                            item.deliveryStatus
                                        }
                                    />
                                </TableCell>


                                <TableCell center>
                                    {
                                        item.packageCount ??
                                        item.packages?.length ??
                                        0
                                    }
                                </TableCell>


                                <TableCell center>

                                    <button
                                        type="button"
                                        onClick={() =>
                                            onView(
                                                item.transferId
                                            )
                                        }
                                        className="
                                                rounded-lg
                                                bg-[#fff1e9]
                                                px-3
                                                py-2
                                                text-xs
                                                font-semibold
                                                text-[#d94f12]
                                                transition
                                                hover:bg-[#ffe4d3]
                                            "
                                    >
                                        View
                                    </button>

                                </TableCell>

                            </tr>

                        ))}

                    </tbody>

                </table>

            </div>

        </div>
    );
}


function TableHeader({
                         children,
                         center = false,
                     }) {

    return (
        <th
            className={`
                px-4
                py-3
                text-xs
                font-semibold
                uppercase
                tracking-wide
                text-slate-500

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


function TableCell({
                       children,
                       center = false,
                       bold = false,
                   }) {

    return (
        <td
            className={`
                px-4
                py-4
                text-sm

                ${
                center
                    ? "text-center"
                    : "text-left"
            }

                ${
                bold
                    ? "font-semibold text-slate-900"
                    : "text-slate-600"
            }
            `}
        >
            {children}
        </td>
    );
}


function StatusBadge({
                         status,
                     }) {

    const normalized =
        status || "NOT_ASSIGNED";


    const accepted =
        normalized === "ACCEPTED";


    const inTransit =
        normalized === "IN_TRANSIT";


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
                inTransit
                    ? "bg-emerald-50 text-emerald-700"
                    : accepted
                        ? "bg-blue-50 text-blue-700"
                        : "bg-amber-50 text-amber-700"
            }
            `}
        >
            {
                formatStatus(
                    normalized
                )
            }
        </span>
    );
}


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