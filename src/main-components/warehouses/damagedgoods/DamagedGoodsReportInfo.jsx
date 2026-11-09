import InfoField
    from "../../../components/warehouse/damaged/InfoField.jsx";


export default function DamagedGoodsReportInfo({
                                                   report,
                                               }) {

    return (
        <>

            <div
                className="
                    mb-6
                    grid
                    grid-cols-1
                    gap-4
                    rounded-2xl
                    border
                    border-slate-200
                    bg-white
                    p-5
                    shadow-sm
                    md:grid-cols-2
                    xl:grid-cols-4
                "
            >

                <InfoField
                    label="Report Code"
                    value={
                        report.reportCode
                    }
                />


                <InfoField
                    label="Source"
                    value={
                        formatText(
                            report.sourceType
                        )
                    }
                />


                <InfoField
                    label="Source Reference"
                    value={
                        report.sourceId
                            ? `#${report.sourceId}`
                            : "-"
                    }
                />


                <InfoField
                    label="Warehouse ID"
                    value={
                        report.warehouseId
                    }
                />


                <InfoField
                    label="Reported By"
                    value={
                        report.reportedByName
                    }
                />


                <InfoField
                    label="Reviewed By"
                    value={
                        report.reviewedByName
                        || "-"
                    }
                />


                <InfoField
                    label="Created At"
                    value={
                        formatDateTime(
                            report.createdAt
                        )
                    }
                />


                <InfoField
                    label="Reviewed At"
                    value={
                        formatDateTime(
                            report.reviewedAt
                        )
                    }
                />

            </div>


            {report.description && (

                <div
                    className="
                        mb-6
                        rounded-2xl
                        border
                        border-slate-200
                        bg-white
                        p-5
                    "
                >

                    <div
                        className="
                            text-xs
                            font-semibold
                            uppercase
                            tracking-wide
                            text-slate-400
                        "
                    >
                        Description
                    </div>


                    <p
                        className="
                            mt-2
                            text-sm
                            leading-6
                            text-slate-700
                        "
                    >
                        {report.description}
                    </p>

                </div>

            )}

        </>
    );
}


function formatText(value) {

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


function formatDateTime(value) {

    if (!value) {
        return "-";
    }


    return new Date(
        value
    ).toLocaleString();
}