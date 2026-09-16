export default function StatusBadge({
                                        status,
                                    }) {

    const styles = {

        REPORTED:
            "bg-amber-50 text-amber-700",

        INSPECTING:
            "bg-blue-50 text-blue-700",

        APPROVED:
            "bg-emerald-50 text-emerald-700",

        QUARANTINED:
            "bg-purple-50 text-purple-700",

        RETURNED_TO_SUPPLIER:
            "bg-cyan-50 text-cyan-700",

        DISPOSED:
            "bg-red-50 text-red-700",

        RESOLVED:
            "bg-emerald-50 text-emerald-700",

        REJECTED:
            "bg-red-50 text-red-700",
    };


    return (
        <span
            className={`
                inline-flex
                rounded-full
                px-3
                py-1
                text-xs
                font-semibold
                ${
                styles[status]
                || "bg-slate-100 text-slate-600"
            }
            `}
        >
            {formatStatus(status)}
        </span>
    );
}


function formatStatus(value) {

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