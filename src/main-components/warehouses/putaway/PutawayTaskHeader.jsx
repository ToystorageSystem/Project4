import Card from "../../../components/ui/Card";
import Button from "../../../components/ui/Button";
import PutawayStatusBadge from "./PutawayStatusBadge.jsx";

export default function PutawayTaskHeader({
    task,
    starting = false,
    onBack,
    onStart,
}) {
    return (
        <Card className="mb-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <button
                        type="button"
                        onClick={onBack}
                        className="mb-3 text-sm font-medium text-slate-600 transition hover:text-[#f25d19]"
                    >
                        ← Quay lại Putaway
                    </button>

                    <h1 className="text-2xl font-bold text-slate-900">
                        {task.taskCode || "-"}
                    </h1>

                    <div className="mt-3 grid gap-x-8 gap-y-1 text-sm text-slate-600 sm:grid-cols-2">
                        <div>
                            Phiếu nhập:{" "}
                            <span className="font-medium text-slate-900">
                                {task.receiptCode || "-"}
                            </span>
                        </div>

                        <div>
                            Kho:{" "}
                            <span className="font-medium text-slate-900">
                                {task.warehouseName || "-"}
                            </span>
                        </div>

                        <div>
                            Nhân viên:{" "}
                            <span className="font-medium text-slate-900">
                                {task.assignedToName || "-"}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex flex-col items-end gap-3">
                    <PutawayStatusBadge
                        status={task.status}
                    />

                    {task.status === "AVAILABLE" && (
                        <Button
                            onClick={onStart}
                            disabled={starting}
                        >
                            {starting
                                ? "Đang bắt đầu..."
                                : "Bắt đầu Putaway"}
                        </Button>
                    )}
                </div>
            </div>
        </Card>
    );
}
