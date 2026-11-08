import { useEffect, useState } from "react";

import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";

const number = (value) =>
    typeof value === "number" ? value : 0;

export default function ReceivingIncidentReportDetail({
    data,
    busy = false,
    onBack,
    onSaveGeneral,
    onSaveItem,
    onPrint,
}) {
    const [managerNote, setManagerNote] = useState("");
    const [penaltyAction, setPenaltyAction] = useState("");
    const [drafts, setDrafts] = useState({});

    useEffect(() => {
        setManagerNote(data?.managerNote ?? "");
        setPenaltyAction(data?.penaltyAction ?? "");

        const next = {};

        (data?.items ?? []).forEach((item) => {
            next[item.id] = {
                reason: item.reason ?? "",
            };
        });

        setDrafts(next);
    }, [data?.id, data?.updatedAt]);

    if (!data) return null;

    const items = data.items ?? [];

    const changeDraft = (itemId, field, value) => {
        setDrafts((current) => ({
            ...current,
            [itemId]: {
                ...current[itemId],
                [field]: value,
            },
        }));
    };

    return (
        <div className="space-y-5">
            <Card className="overflow-hidden p-0">
                <div className="h-1 bg-[#f25d19]" />

                <div className="flex flex-col gap-4 border-b border-slate-100 p-5 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <p className="text-xs font-semibold uppercase tracking-wider text-[#f25d19]">
                            Receiving Incident Report
                        </p>
                        <h2 className="mt-1 text-xl font-semibold text-slate-900">
                            {data.reportCode}
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Receipt: {data.receiptCode}
                        </p>
                    </div>

                    <div className="flex gap-2">
                        <Button variant="outline" onClick={onPrint}>
                            Print
                        </Button>
                        <Button variant="ghost" onClick={onBack}>
                            Back
                        </Button>
                    </div>
                </div>

                <div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-4">
                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">Warehouse</p>
                        <p className="mt-1 font-semibold text-slate-800">
                            {data.warehouseName ?? "-"}
                        </p>
                    </div>

                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">
                            Warehouse Staff / Inspector
                        </p>
                        <p className="mt-1 font-semibold text-slate-800">
                            {data.warehouseStaffName ?? "-"}
                        </p>
                    </div>

                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">
                            Warehouse Manager
                        </p>
                        <p className="mt-1 font-semibold text-slate-800">
                            {data.warehouseManagerName ?? "-"}
                        </p>
                    </div>

                    <div className="rounded-xl bg-slate-50 p-4">
                        <p className="text-xs text-slate-400">
                            Issue products
                        </p>
                        <p className="mt-1 text-xl font-semibold text-slate-800">
                            {items.length}
                        </p>
                    </div>
                </div>
            </Card>

            <Card className="overflow-hidden p-0">
                <div className="border-b border-slate-100 p-5">
                    <h3 className="font-semibold text-slate-900">
                        Faulty products
                    </h3>
                    <p className="mt-1 text-sm text-slate-500">
                        Products accepted by Warehouse Manager in the same receipt are grouped into one report.
                    </p>
                </div>

                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200 text-sm">
                        <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                            <tr>
                                <th className="px-3 py-3">Product</th>
                                <th className="px-3 py-3">Type</th>
                                <th className="px-3 py-3">Expected</th>
                                <th className="px-3 py-3">Actual</th>
                                <th className="px-3 py-3">Accepted</th>
                                <th className="min-w-72 px-3 py-3">Reason</th>
                                <th className="px-3 py-3">Action</th>
                            </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-100 bg-white">
                            {items.map((item) => {
                                const draft = drafts[item.id] ?? {
                                    reason: item.reason ?? "",
                                    penaltyNote: item.penaltyNote ?? "",
                                };

                                return (
                                    <tr key={item.id}>
                                        <td className="px-3 py-3">
                                            <p className="font-medium text-slate-800">
                                                {item.productName}
                                            </p>
                                            <p className="text-xs text-slate-400">
                                                {item.productCode}
                                            </p>
                                        </td>

                                        <td className="px-3 py-3">
                                            {item.discrepancyType}
                                        </td>

                                        <td className="px-3 py-3">
                                            {number(item.expectedQuantity)}
                                        </td>

                                        <td className="px-3 py-3">
                                            {number(item.actualQuantity)}
                                        </td>

                                        <td className="px-3 py-3 font-semibold">
                                            {number(item.acceptedQuantity)}
                                        </td>

                                        <td className="px-3 py-3">
                                            <textarea
                                                rows={4}
                                                value={draft.reason}
                                                onChange={(e) =>
                                                    changeDraft(
                                                        item.id,
                                                        "reason",
                                                        e.target.value
                                                    )
                                                }
                                                className="w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-[#f25d19]"
                                            />
                                        </td>


                                        <td className="px-3 py-3">
                                            <Button
                                                disabled={
                                                    busy ||
                                                    !draft.reason.trim()
                                                }
                                                onClick={() =>
                                                    onSaveItem?.(
                                                        item.id,
                                                        {
                                                            reason: draft.reason.trim(),
                                                        }
                                                    )
                                                }
                                            >
                                                Save
                                            </Button>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </Card>
            <Card>
                <label className="mb-2 block text-sm font-medium text-slate-700">
                    Penalty / Action
                </label>

                <textarea
                    rows={5}
                    value={penaltyAction}
                    placeholder="Enter the overall penalty or handling action..."
                    onChange={(e) => setPenaltyAction(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none focus:border-[#f25d19]"
                />
            </Card>
            <Card>
                <label className="mb-2 block text-sm font-medium text-slate-700">
                    General Warehouse Manager note
                </label>

                <textarea
                    rows={5}
                    value={managerNote}
                    placeholder="Optional note for the whole report..."
                    onChange={(e) => setManagerNote(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none focus:border-[#f25d19]"
                />

                <div className="mt-4 flex justify-end">
                    <Button
                        disabled={busy}
                        onClick={() =>
                            onSaveGeneral?.({
                                penaltyAction: penaltyAction.trim(),
                                managerNote: managerNote.trim(),
                            })
                        }
                    >
                        Save
                    </Button>
                </div>
            </Card>

            <Card>
                <h3 className="font-semibold text-slate-900">
                    Physical signatures
                </h3>

                <p className="mt-2 text-sm leading-6 text-slate-500">
                    No digital signing or confirmation is required. Print the
                    report, then Warehouse Staff / Inspector and Warehouse
                    Manager sign the paper document.
                </p>
            </Card>
        </div>
    );
}
