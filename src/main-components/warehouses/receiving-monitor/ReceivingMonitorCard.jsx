import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import StatusBadge from "../../../components/ui/StatusBadge";

const number = (value) => typeof value === "number" ? value : 0;
const formatDateTime = (value) => {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("en-GB");
};

export default function ReceivingMonitorCard({ item, onView }) {
    const status = String(item?.status ?? "").toUpperCase();
    const done = status === "INSPECTED" || status === "COMPLETED";
    const total =
        number(item?.totalProducts);

    const rawInspected =
        number(item?.inspectedProducts);

    const inspected =
        done
            ? total
            : rawInspected;

    const percent =
        done
            ? 100
            : typeof item?.progressPercent === "number"
                ? Math.min(
                    100,
                    Math.max(
                        0,
                        Math.round(
                            item.progressPercent
                        )
                    )
                )
                : total > 0
                    ? Math.round(
                        (inspected / total) * 100
                    )
                    : 0;
    const label = status === "COMPLETED" ? "View details" : status === "INSPECTED" ? "Review" : "View progress";

    return (
        <Card className="overflow-hidden p-0 transition hover:-translate-y-0.5 hover:shadow-lg">
            <div className="h-1 bg-[#f25d19]" />
            <div className="space-y-5 p-5">
                <div className="flex items-start justify-between gap-4">
                    <div><p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Receiving Receipt</p><h3 className="mt-1 text-lg font-semibold text-slate-900">{item?.receiptCode ?? `#${item?.receiptId ?? item?.id ?? "-"}`}</h3></div>
                    <StatusBadge status={status} />
                </div>
                <div>
                    <div className="mb-2 flex justify-between text-sm"><span className="font-medium text-slate-600">{done ? "Inspection completed" : "Inspection progress"}<span className="ml-2 text-xs text-slate-400">{inspected}/{total}</span></span><span className="font-semibold text-[#d94f12]">{percent}%</span></div>
                    <div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-[#f25d19]" style={{ width: `${percent}%` }} /></div>
                </div>
                <div className="rounded-xl border border-orange-100 bg-[#fff8f4] p-4"><div className="flex justify-between gap-4"><div><p className="text-xs text-slate-400">Warehouse Staff</p><p className="mt-1 text-sm font-semibold text-slate-800">{item?.staffName ?? "-"}</p></div><div className="text-right"><p className="text-xs text-slate-400">Started At</p><p className="mt-1 text-sm font-medium text-slate-700">{formatDateTime(item?.receivingStartedAt)}</p></div></div></div>
                <div className="flex justify-end border-t border-slate-100 pt-4"><Button onClick={() => onView?.(item)}>{label}</Button></div>
            </div>
        </Card>
    );
}
