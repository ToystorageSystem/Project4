export const normalizeStatus = (value) =>
    String(
        value ?? ""
    )
        .trim()
        .toUpperCase();


export default function ReceivingStatusBadge({
                                                 status,
                                             }) {

    const normalized =
        normalizeStatus(
            status
        );


    const styles = {

        DRAFT:
            "bg-slate-100 text-slate-700",

        PENDING_APPROVAL:
            "bg-amber-100 text-amber-700",

        APPROVED:
            "bg-blue-100 text-blue-700",

        PACKING:
            "bg-purple-100 text-purple-700",

        ISSUED:
            "bg-indigo-100 text-indigo-700",

        SHIPPED:
            "bg-cyan-100 text-cyan-700",

        INSPECTING:
            "bg-orange-100 text-orange-700",

        PENDING_CONFIRMATION:
            "bg-amber-100 text-amber-800",

        RECEIVED:
            "bg-green-100 text-green-700",

        CANCELLED:
            "bg-red-100 text-red-700",

        OPEN:
            "bg-red-100 text-red-700",

        INVESTIGATING:
            "bg-amber-100 text-amber-700",

        RESOLVED:
            "bg-green-100 text-green-700",

        NORMAL:
            "bg-green-100 text-green-700",

        DAMAGED:
            "bg-red-100 text-red-700",

        EXPIRED:
            "bg-red-100 text-red-700",

        QUARANTINE:
            "bg-amber-100 text-amber-700",

        RECEIVING:
            "bg-orange-100 text-orange-700",

        INSPECTED:
            "bg-amber-100 text-amber-700",

        COMPLETED:
            "bg-green-100 text-green-700",
    };


    return (
        <span
            className={
                "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold " +

                (
                    styles[
                        normalized
                        ]
                    ??
                    "bg-slate-100 text-slate-700"
                )
            }
        >

            {
                normalized
                    .replaceAll(
                        "_",
                        " "
                    )
                ||
                "-"
            }

        </span>
    );
}