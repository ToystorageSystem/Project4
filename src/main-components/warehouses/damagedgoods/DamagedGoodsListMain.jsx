import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    getDamagedGoodsReports,
} from "../../../api/warehouses/damagedgoods/damagedGoods.js";

import AlertMessage
    from "../../../components/warehouse/damaged/AlertMessage.jsx";

import StatusBadge
    from "../../../components/warehouse/damaged/StatusBadge.jsx";


export default function DamagedGoodsListMain() {

    const navigate =
        useNavigate();


    const [reports, setReports] =
        useState([]);


    const [keyword, setKeyword] =
        useState("");


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");


    // =====================================================
    // LOAD
    // =====================================================

    const loadReports =
        async () => {

            try {

                setLoading(true);
                setError("");


                const data =
                    await getDamagedGoodsReports();


                setReports(
                    Array.isArray(data)
                        ? data
                        : []
                );

            } catch (err) {

                setError(
                    getErrorMessage(
                        err,
                        "Failed to load damaged goods reports."
                    )
                );

            } finally {

                setLoading(false);
            }
        };


    useEffect(() => {

        loadReports();

    }, []);


    // =====================================================
    // FILTER
    // =====================================================

    const filteredReports =
        useMemo(
            () => {

                const search =
                    keyword
                        .trim()
                        .toLowerCase();


                if (!search) {

                    return reports;
                }


                return reports.filter(
                    (report) =>

                        contains(
                            report.reportCode,
                            search
                        )

                        ||

                        contains(
                            report.sourceType,
                            search
                        )

                        ||

                        contains(
                            report.reportedByName,
                            search
                        )

                        ||

                        contains(
                            report.status,
                            search
                        )
                );
            },
            [
                reports,
                keyword,
            ]
        );


    // =====================================================
    // VIEW
    // =====================================================

    const handleView =
        (reportId) => {

            navigate(
                `/warehouse/damaged-goods/${reportId}`
            );
        };


    return (
        <div className="w-full">

            {/* HEADER */}
            <div className="mb-6">

                <h1
                    className="
                        text-2xl
                        font-bold
                        text-slate-900
                    "
                >
                    Damaged Goods
                </h1>


                <p
                    className="
                        mt-1
                        text-sm
                        text-slate-500
                    "
                >
                    Review and process damaged goods in the warehouse.
                </p>

            </div>


            <AlertMessage
                type="error"
                message={error}
            />


            {/* SEARCH */}
            <div
                className="
                    mb-5
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-4
                "
            >

                <input
                    type="text"

                    value={keyword}

                    onChange={(event) =>
                        setKeyword(
                            event.target.value
                        )
                    }

                    placeholder="Search report code, source, reporter..."

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

            </div>


            {/* TABLE */}
            <div
                className="
                    overflow-x-auto
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    shadow-sm
                "
            >

                <table
                    className="
                        w-full
                        min-w-[950px]
                    "
                >

                    <thead>

                    <tr className="bg-slate-50">

                        <TableHeader>
                            Report
                        </TableHeader>

                        <TableHeader>
                            Source
                        </TableHeader>

                        <TableHeader>
                            Reported By
                        </TableHeader>

                        <TableHeader>
                            Products
                        </TableHeader>

                        <TableHeader>
                            Created At
                        </TableHeader>

                        <TableHeader>
                            Status
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
                                        py-14
                                        text-center
                                        text-sm
                                        text-slate-500
                                    "
                            >
                                Loading damaged goods...
                            </td>

                        </tr>

                    )}


                    {!loading
                        && filteredReports.length === 0
                        && (

                            <tr>

                                <td
                                    colSpan={7}
                                    className="
                                        px-4
                                        py-14
                                        text-center
                                        text-sm
                                        text-slate-500
                                    "
                                >
                                    No damaged goods reports found.
                                </td>

                            </tr>

                        )}


                    {!loading
                        && filteredReports.map(
                            (report) => (

                                <tr
                                    key={
                                        report.reportId
                                    }
                                    className="
                                        border-t
                                        border-slate-100
                                        transition
                                        hover:bg-slate-50
                                    "
                                >

                                    <TableCell>

                                        <span
                                            className="
                                                font-semibold
                                                text-slate-900
                                            "
                                        >
                                            {report.reportCode}
                                        </span>

                                    </TableCell>


                                    <TableCell>

                                        <div>
                                            {formatText(
                                                report.sourceType
                                            )}
                                        </div>

                                        {report.sourceId && (

                                            <div
                                                className="
                                                    mt-1
                                                    text-xs
                                                    text-slate-400
                                                "
                                            >
                                                Ref #{report.sourceId}
                                            </div>

                                        )}

                                    </TableCell>


                                    <TableCell>
                                        {report.reportedByName || "-"}
                                    </TableCell>


                                    <TableCell>
                                        {report.items?.length ?? 0}
                                    </TableCell>


                                    <TableCell>
                                        {formatDateTime(
                                            report.createdAt
                                        )}
                                    </TableCell>


                                    <TableCell>

                                        <StatusBadge
                                            status={
                                                report.status
                                            }
                                        />

                                    </TableCell>


                                    <TableCell center>

                                        <button
                                            type="button"

                                            onClick={() =>
                                                handleView(
                                                    report.reportId
                                                )
                                            }

                                            className="
                                                rounded-lg
                                                bg-[#fff1e9]
                                                px-4
                                                py-2
                                                text-xs
                                                font-semibold
                                                text-[#d94f12]
                                                transition
                                                hover:bg-[#ffe2d1]
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


// =====================================================
// TABLE
// =====================================================

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
                   }) {

    return (
        <td
            className={`
                px-4
                py-4
                text-sm
                text-slate-600
                ${
                center
                    ? "text-center"
                    : "text-left"
            }
            `}
        >
            {children}
        </td>
    );
}


// =====================================================
// UTILS
// =====================================================

function contains(
    value,
    search
) {

    return value
        ?.toString()
        .toLowerCase()
        .includes(search);
}


function formatText(value) {

    if (!value) {
        return "-";
    }


    return value
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(
            /\b\w/g,
            (letter) =>
                letter.toUpperCase()
        );
}


function formatDateTime(value) {

    if (!value) {
        return "-";
    }


    return new Date(
        value
    ).toLocaleString();
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