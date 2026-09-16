const STATUS_CONFIG = {
    CREATED: {
        label: "Created",
        className:
            "border-slate-200 bg-slate-100 text-slate-700",
    },

    PICKING: {
        label: "Picking",
        className:
            "border-indigo-200 bg-indigo-100 text-indigo-700",
    },

    PACKING: {
        label: "Packing",
        className:
            "border-amber-200 bg-amber-100 text-amber-700",
    },

    PACKED: {
        label: "Packed",
        className:
            "border-blue-200 bg-blue-100 text-blue-700",
    },

    CHECKED: {
        label: "Checked",
        className:
            "border-emerald-200 bg-emerald-100 text-emerald-700",
    },

    ASSIGNED_TO_DELIVERY: {
        label: "Assigned To Delivery",
        className:
            "border-violet-200 bg-violet-100 text-violet-700",
    },

    SHIPPED: {
        label: "Shipped",
        className:
            "border-cyan-200 bg-cyan-100 text-cyan-700",
    },

    RECEIVING: {
        label: "Receiving",
        className:
            "border-orange-200 bg-orange-100 text-orange-700",
    },

    RECEIVED: {
        label: "Received",
        className:
            "border-green-200 bg-green-100 text-green-700",
    },

    COMPLETED: {
        label: "Completed",
        className:
            "border-green-200 bg-green-100 text-green-700",
    },

    CANCELLED: {
        label: "Cancelled",
        className:
            "border-red-200 bg-red-100 text-red-700",
    },
};

export default function PackingStatusBadge({
                                               status,
                                           }) {
    const config =
        STATUS_CONFIG[status] || {
            label: status || "-",
            className:
                "border-gray-200 bg-gray-100 text-gray-700",
        };

    return (
        <span
            className={`
                inline-flex
                items-center
                rounded-full
                border
                px-2.5
                py-1
                text-xs
                font-semibold
                ${config.className}
            `}
        >
            {config.label}
        </span>
    );
}