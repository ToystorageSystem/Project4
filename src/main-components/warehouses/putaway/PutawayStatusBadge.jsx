const STATUS_CONFIG = {
    PENDING: {
        label: "Pending",
        className:
            "bg-slate-100 text-slate-700 border-slate-200",
    },

    AVAILABLE: {
        label: "Available",
        className:
            "bg-blue-50 text-blue-700 border-blue-200",
    },

    IN_PROGRESS: {
        label: "In Progress",
        className:
            "bg-amber-50 text-amber-700 border-amber-200",
    },

    COMPLETED: {
        label: "Completed",
        className:
            "bg-emerald-50 text-emerald-700 border-emerald-200",
    },

    CANCELLED: {
        label: "Cancelled",
        className:
            "bg-red-50 text-red-700 border-red-200",
    },
};


export default function PutawayStatusBadge({
                                               status,
                                           }) {
    const normalizedStatus =
        String(status ?? "")
            .trim()
            .toUpperCase();


    const config =
        STATUS_CONFIG[normalizedStatus] ?? {
            label:
                normalizedStatus || "-",

            className:
                "bg-slate-100 text-slate-600 border-slate-200",
        };


    return (
        <span
            className={
                "inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold " +
                config.className
            }
        >
            {config.label}
        </span>
    );
}