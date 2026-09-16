import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import PutawayStatusBadge from "./PutawayStatusBadge.jsx";

export default function PutawayItemCard({
    item,
    taskStatus,
    selected = false,
    onSelect,
}) {
    return (
        <Card
            className={
                selected
                    ? "border-[#f25d19] ring-2 ring-[#f25d19]/10"
                    : ""
            }
        >
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-semibold text-slate-900">
                            {item.productName || "Sản phẩm"}
                        </h3>

                        <PutawayStatusBadge
                            status={item.status}
                        />
                    </div>

                    <div className="mt-2 space-y-1 text-sm text-slate-500">
                        <div>
                            Mã SP:{" "}
                            <span className="text-slate-700">
                                {item.productCode || "-"}
                            </span>
                        </div>

                        <div>
                            Barcode:{" "}
                            <span className="text-slate-700">
                                {item.barcode || "-"}
                            </span>
                        </div>
                    </div>

                    <div className="mt-4 grid grid-cols-3 gap-3">
                        <div className="rounded-xl bg-slate-50 p-3">
                            <div className="text-xs text-slate-500">
                                Cần cất
                            </div>
                            <div className="mt-1 font-semibold text-slate-900">
                                {item.expectedQuantity ?? 0}
                            </div>
                        </div>

                        <div className="rounded-xl bg-slate-50 p-3">
                            <div className="text-xs text-slate-500">
                                Đã cất
                            </div>
                            <div className="mt-1 font-semibold text-slate-900">
                                {item.putawayQuantity ?? 0}
                            </div>
                        </div>

                        <div className="rounded-xl bg-slate-50 p-3">
                            <div className="text-xs text-slate-500">
                                Còn lại
                            </div>
                            <div className="mt-1 font-semibold text-slate-900">
                                {item.remainingQuantity ?? 0}
                            </div>
                        </div>
                    </div>

                    <div className="mt-4 text-sm text-slate-600">
                        <div>
                            Từ:{" "}
                            <strong>
                                {item.fromLocationCode ||
                                    "RECEIVING"}
                            </strong>
                        </div>

                        {item.toLocationCode && (
                            <div className="mt-1">
                                Đến:{" "}
                                <strong>
                                    {item.toLocationCode}
                                </strong>
                                {item.zone
                                    ? ` • Zone ${item.zone}`
                                    : ""}
                                {item.shelf
                                    ? ` • Shelf ${item.shelf}`
                                    : ""}
                            </div>
                        )}
                    </div>
                </div>

                {taskStatus === "IN_PROGRESS" &&
                    item.status !== "COMPLETED" && (
                        <Button
                            size="sm"
                            variant={
                                selected
                                    ? "secondary"
                                    : "outline"
                            }
                            onClick={() =>
                                onSelect?.(item)
                            }
                        >
                            {selected
                                ? "Đang chọn"
                                : "Cất hàng"}
                        </Button>
                    )}
            </div>
        </Card>
    );
}
