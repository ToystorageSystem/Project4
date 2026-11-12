const TYPE_CONFIG = {
    NORMAL: {
        label: "Normal",
        className:
            "border-blue-200 bg-blue-50 text-blue-700",
    },

    RECEIVING: {
        label: "Receiving",
        className:
            "border-violet-200 bg-violet-50 text-violet-700",
    },

    DAMAGED: {
        label: "Damaged",
        className:
            "border-red-200 bg-red-50 text-red-700",
    },

    QUARANTINE: {
        label: "Quarantine",
        className:
            "border-amber-200 bg-amber-50 text-amber-700",
    },

    RETURN: {
        label: "Return",
        className:
            "border-slate-200 bg-slate-100 text-slate-700",
    },
};


export default function LocationTypeBadge({
                                              type,
                                          }) {
    const normalized =
        String(type ?? "")
            .trim()
            .toUpperCase();

    const config =
        TYPE_CONFIG[normalized] ?? {
            label:
                normalized || "-",

            className:
                "border-slate-200 bg-slate-50 text-slate-600",
        };

    return (
        <span
            className={
                "inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold " +
                config.className
            }
        >
            {config.label}
        </span>
    );
}


export function LocationStatusBadge({
                                        status,
                                    }) {
    const active =
        String(status)
            .toUpperCase() === "ACTIVE";

    return (
        <span
            className={
                "inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold " +
                (
                    active
                        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                        : "border-slate-200 bg-slate-100 text-slate-600"
                )
            }
        >
            {active
                ? "Active"
                : "Inactive"}
        </span>
    );
}