import {
    useEffect,
    useState,
} from "react";

import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";

export default function RequestReinspectionModal({
    open,
    loading = false,
    onClose,
    onSubmit,
}) {
    const [reason, setReason] = useState("");
    const [allowSameStaff, setAllowSameStaff] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!open) return;

        setReason("");
        setAllowSameStaff(false);
        setError("");
    }, [open]);

    if (!open) {
        return null;
    }

    const handleSubmit = async (event) => {
        event.preventDefault();

        const trimmedReason = reason.trim();

        if (!trimmedReason) {
            setError("Please enter a reason for re-inspection.");
            return;
        }

        setError("");

        await onSubmit?.({
            reason: trimmedReason,
            allowSameStaff,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <Card className="w-full max-w-lg p-0 shadow-2xl">
                <div className="border-b border-slate-100 p-5">
                    <h3 className="text-lg font-semibold text-slate-900">
                        Request re-inspection
                    </h3>

                    <p className="mt-1 text-sm text-slate-500">
                        Send this receiving receipt back to Warehouse Staff for another inspection attempt.
                    </p>
                </div>

                <form
                    onSubmit={handleSubmit}
                    className="space-y-5 p-5"
                >
                    <div>
                        <label className="mb-2 block text-sm font-medium text-slate-700">
                            Reason
                        </label>

                        <textarea
                            value={reason}
                            onChange={(event) =>
                                setReason(event.target.value)
                            }
                            rows={5}
                            maxLength={500}
                            placeholder="Explain why the goods need to be inspected again..."
                            className="w-full rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm text-slate-800 outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />

                        <div className="mt-1 text-right text-xs text-slate-400">
                            {reason.length}/500
                        </div>
                    </div>

                    <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-orange-100 bg-[#fff8f4] p-4">
                        <input
                            type="checkbox"
                            checked={allowSameStaff}
                            onChange={(event) =>
                                setAllowSameStaff(event.target.checked)
                            }
                            className="mt-1 h-4 w-4 accent-[#f25d19]"
                        />

                        <div>
                            <p className="text-sm font-medium text-slate-800">
                                Allow the same Warehouse Staff
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                                Enable this only when the same staff member is allowed to perform the next inspection attempt.
                            </p>
                        </div>
                    </label>

                    {error && (
                        <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={onClose}
                            disabled={loading}
                        >
                            Cancel
                        </Button>

                        <Button
                            type="submit"
                            disabled={loading}
                        >
                            {loading
                                ? "Requesting..."
                                : "Request re-inspection"}
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
}
