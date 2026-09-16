import {
    useState,
} from "react";

import InfoField
    from "../../../components/warehouse/damaged/InfoField.jsx";

import StatusBadge
    from "../../../components/warehouse/damaged/StatusBadge.jsx";

import AlertMessage
    from "../../../components/warehouse/damaged/AlertMessage.jsx";

import DamagedGoodsResolutionForm
    from "./DamagedGoodsResolutionForm.jsx";


export default function DamagedGoodsItemCard({
                                                 item,
                                                 reportStatus,
                                                 processing,
                                                 onHandleItem,
                                             }) {

    const [
        confirmedQuantity,
        setConfirmedQuantity,
    ] = useState(
        item.confirmedQuantity
        ?? item.quantity
        ?? 1
    );


    const [
        disposition,
        setDisposition,
    ] = useState(
        "QUARANTINE"
    );


    const [
        resolutionNote,
        setResolutionNote,
    ] = useState("");


    const finishedStatuses = [
        "QUARANTINED",
        "RETURNED_TO_SUPPLIER",
        "DISPOSED",
        "RESOLVED",
    ];


    const alreadyHandled =
        finishedStatuses.includes(
            item.status
        );


    const canHandle =
        reportStatus === "INSPECTING"
        && !alreadyHandled;


    const handleSubmit =
        () => {

            const quantity =
                Number(
                    confirmedQuantity
                );


            if (
                !Number.isInteger(quantity)
                || quantity <= 0
            ) {

                window.alert(
                    "Confirmed quantity must be greater than 0."
                );

                return;
            }


            if (
                quantity
                > item.quantity
            ) {

                window.alert(
                    "Confirmed quantity cannot exceed reported quantity."
                );

                return;
            }


            onHandleItem({

                itemId:
                item.itemId,

                confirmedQuantity:
                quantity,

                disposition,

                resolutionNote:
                    resolutionNote.trim(),
            });
        };


    return (
        <div
            className="
                rounded-2xl
                border
                border-slate-200
                bg-white
                p-5
                shadow-sm
            "
        >

            {/* HEADER */}
            <div
                className="
                    flex
                    flex-col
                    gap-4
                    md:flex-row
                    md:items-start
                    md:justify-between
                "
            >

                <div>

                    <h3
                        className="
                            text-lg
                            font-semibold
                            text-slate-900
                        "
                    >
                        {item.productName || "-"}
                    </h3>


                    <div
                        className="
                            mt-1
                            text-xs
                            text-slate-400
                        "
                    >
                        Product ID #{item.productId}
                    </div>

                </div>


                <StatusBadge
                    status={
                        item.status
                    }
                />

            </div>


            {/* INFO */}
            <div
                className="
                    mt-5
                    grid
                    grid-cols-1
                    gap-4
                    sm:grid-cols-2
                    xl:grid-cols-5
                "
            >

                <InfoField
                    label="Reported Quantity"
                    value={
                        item.quantity
                    }
                />


                <InfoField
                    label="Confirmed Quantity"
                    value={
                        item.confirmedQuantity
                        ?? "-"
                    }
                />


                <InfoField
                    label="Damage Type"
                    value={
                        formatText(
                            item.damageType
                        )
                    }
                />


                <InfoField
                    label="Location"
                    value={
                        item.locationName
                    }
                />


                <InfoField
                    label="Disposition"
                    value={
                        formatText(
                            item.disposition
                        )
                    }
                />

            </div>


            {/* NOTE */}
            {item.conditionNote && (

                <div
                    className="
                        mt-5
                        rounded-xl
                        bg-slate-50
                        px-4
                        py-3
                    "
                >

                    <div
                        className="
                            text-xs
                            font-semibold
                            uppercase
                            text-slate-400
                        "
                    >
                        Condition Note
                    </div>


                    <div
                        className="
                            mt-1
                            text-sm
                            text-slate-700
                        "
                    >
                        {item.conditionNote}
                    </div>

                </div>

            )}


            {/* BEFORE START */}
            {reportStatus ===
                "REPORTED" && (

                    <div className="mt-5">

                        <AlertMessage
                            type="warning"
                            message="Start inspection before handling this product."
                        />

                    </div>

                )}


            {/* FORM */}
            {canHandle && (

                <DamagedGoodsResolutionForm

                    maxQuantity={
                        item.quantity
                    }

                    confirmedQuantity={
                        confirmedQuantity
                    }

                    disposition={
                        disposition
                    }

                    resolutionNote={
                        resolutionNote
                    }

                    processing={
                        processing
                    }

                    onQuantityChange={
                        setConfirmedQuantity
                    }

                    onDispositionChange={
                        setDisposition
                    }

                    onNoteChange={
                        setResolutionNote
                    }

                    onSubmit={
                        handleSubmit
                    }
                />

            )}

        </div>
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