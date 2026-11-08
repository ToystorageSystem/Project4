import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import {
    getReceivingIncidentReportDetail,
} from "../../../api/receipts/incidents/receivingIncidentReport";

const number = (value) =>
    typeof value === "number" ? value : 0;

const formatDateTime = (value) => {
    if (!value) return "-";

    const d = new Date(value);

    return Number.isNaN(d.getTime())
        ? "-"
        : d.toLocaleString("en-GB");
};

export default function ReceivingIncidentReportPrintPage() {
    const { id } = useParams();

    const [data, setData] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        const load = async () => {
            try {
                setData(
                    await getReceivingIncidentReportDetail(id)
                );
            } catch (e) {
                setError(
                    e.response?.data?.message ??
                    "Unable to load report."
                );
            }
        };

        load();
    }, [id]);

    if (error) {
        return <div className="p-8 text-red-600">{error}</div>;
    }

    if (!data) {
        return <div className="p-8">Loading...</div>;
    }

    const items = data.items ?? [];

    return (
        <div className="min-h-screen bg-slate-100 p-6 print:bg-white print:p-0">
            <div className="mx-auto w-full max-w-[210mm] bg-white p-[14mm] shadow print:max-w-none print:p-[10mm] print:shadow-none">

                <div className="mb-6 flex justify-end print:hidden">
                    <button
                        type="button"
                        onClick={() => window.print()}
                        className="
        rounded-lg
        bg-[#f25d19]
        px-4
        py-2
        text-sm
        font-semibold
        text-white
        transition-all
        duration-200
        hover:bg-[#d94f12]
        hover:shadow-md
        hover:-translate-y-0.5
        active:translate-y-0
        active:shadow-sm
    "
                    >
                        Print
                    </button>
                </div>

                <div className="text-center">
                    <h1 className="text-2xl font-bold uppercase">
                        RECEIVING DISCREPANCY REPORT
                    </h1>

                    <p className="mt-2 text-sm">
                        Report Code:{" "}
                        <strong>{data.reportCode}</strong>
                    </p>
                </div>

                <div className="mt-8 grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
                    <p>
                        Receipt:{" "}
                        <strong>{data.receiptCode}</strong>
                    </p>

                    <p>
                        Warehouse:{" "}
                        <strong>{data.warehouseName}</strong>
                    </p>

                    <p>
                        Warehouse Staff / Inspector:{" "}
                        <strong>{data.warehouseStaffName}</strong>
                    </p>

                    <p>
                        Warehouse Manager:{" "}
                        <strong>{data.warehouseManagerName}</strong>
                    </p>

                    <p>
                        Issue Products:{" "}
                        <strong>{items.length}</strong>
                    </p>

                    <p>
                        Created At:{" "}
                        <strong>
                            {formatDateTime(data.createdAt)}
                        </strong>
                    </p>
                </div>

                <table className="mt-8 w-full border-collapse text-xs">
                    <thead>
                    <tr>
                        <th className="border border-black p-2">No.</th>
                        <th className="border border-black p-2 text-left">
                            Product
                        </th>
                        <th className="border border-black p-2">Issue Type</th>
                        <th className="border border-black p-2">Expected</th>
                        <th className="border border-black p-2">
                            Actual
                        </th>
                        <th className="border border-black p-2">
                            Accepted
                        </th>
                        <th className="border border-black p-2">Damaged</th>
                        <th className="border border-black p-2">Shortage</th>
                        <th className="border border-black p-2">Surplus</th>
                    </tr>
                    </thead>

                    <tbody>
                    {items.map((item, index) => (
                        <tr key={item.id}>
                            <td className="border border-black p-2 text-center">
                                {index + 1}
                            </td>

                            <td className="border border-black p-2">
                                <strong>{item.productName}</strong>
                                <div>{item.productCode}</div>
                            </td>

                            <td className="border border-black p-2 text-center">
                                {item.discrepancyType}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.expectedQuantity)}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.actualQuantity)}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.acceptedQuantity)}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.damagedQuantity)}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.shortageQuantity)}
                            </td>

                            <td className="border border-black p-2 text-center">
                                {number(item.surplusQuantity)}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                <div className="mt-8">
                    <p className="font-semibold">
                        Reasons / Handling Details:
                    </p>

                    <ul className="mt-3 list-disc space-y-2 pl-6 text-sm">
                        {items.map((item) => (
                            <li key={item.id}>
                                <strong>{item.productName}</strong>
                                {" — "}
                                {item.reason}
                            </li>
                        ))}
                    </ul>
                </div>
                {data.penaltyAction && (
                    <div className="mt-6">
                        <p className="font-semibold">
                            Penalty / Action:
                        </p>

                        <div className="mt-2 whitespace-pre-wrap border border-black p-3 text-sm">
                            {data.penaltyAction}
                        </div>
                    </div>
                )}

                {data.managerNote && (
                    <div className="mt-6">
                        <p className="font-semibold">
                            Warehouse Manager Note:
                        </p>

                        <div className="mt-2 whitespace-pre-wrap border border-black p-3 text-sm">
                            {data.managerNote}
                        </div>
                    </div>
                )}

                <div className="mt-14 grid grid-cols-2 gap-20 text-center text-sm">
                    <div>
                        <p className="font-semibold uppercase">
                            Warehouse Staff / Inspector
                        </p>
                        <p className="mt-1 text-xs italic">
                            (Signature and full name)
                        </p>

                        <div className="h-28" />

                        <p className="font-semibold">
                            {data.warehouseStaffName}
                        </p>
                    </div>

                    <div>
                        <p className="font-semibold uppercase">
                            Warehouse Manager
                        </p>
                        <p className="mt-1 text-xs italic">
                            (Signature and full name)
                        </p>

                        <div className="h-28" />

                        <p className="font-semibold">
                            {data.warehouseManagerName}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
