import AlertMessage
    from "../../../components/warehouse/damaged/AlertMessage.jsx";


export default function DamagedGoodsResolutionForm({
                                                       maxQuantity,

                                                       confirmedQuantity,
                                                       disposition,
                                                       resolutionNote,

                                                       processing,

                                                       onQuantityChange,
                                                       onDispositionChange,
                                                       onNoteChange,
                                                       onSubmit,
                                                   }) {

    return (
        <div
            className="
                mt-6
                border-t
                border-slate-100
                pt-5
            "
        >

            <div
                className="
                    grid
                    grid-cols-1
                    gap-4
                    lg:grid-cols-3
                "
            >

                {/* QUANTITY */}
                <div>

                    <label
                        className="
                            mb-2
                            block
                            text-sm
                            font-medium
                            text-slate-700
                        "
                    >
                        Confirmed Quantity
                    </label>


                    <input
                        type="number"

                        min="1"

                        max={
                            maxQuantity
                        }

                        value={
                            confirmedQuantity
                        }

                        onChange={(event) =>
                            onQuantityChange(
                                event.target.value
                            )
                        }

                        className="
                            w-full
                            rounded-xl
                            border
                            border-slate-300
                            px-4
                            py-2.5
                            text-sm
                            outline-none
                            focus:border-[#f25d19]
                        "
                    />

                </div>


                {/* RESOLUTION */}
                <div>

                    <label
                        className="
                            mb-2
                            block
                            text-sm
                            font-medium
                            text-slate-700
                        "
                    >
                        Resolution
                    </label>


                    <select
                        value={
                            disposition
                        }

                        onChange={(event) =>
                            onDispositionChange(
                                event.target.value
                            )
                        }

                        className="
                            w-full
                            rounded-xl
                            border
                            border-slate-300
                            bg-white
                            px-4
                            py-2.5
                            text-sm
                            outline-none
                            focus:border-[#f25d19]
                        "
                    >

                        <option value="QUARANTINE">
                            Move to Quarantine
                        </option>


                        <option value="RETURN_TO_SUPPLIER">
                            Return to Supplier
                        </option>


                        <option value="DISPOSE">
                            Dispose
                        </option>

                    </select>

                </div>


                {/* NOTE */}
                <div>

                    <label
                        className="
                            mb-2
                            block
                            text-sm
                            font-medium
                            text-slate-700
                        "
                    >
                        Resolution Note
                    </label>


                    <input
                        type="text"

                        value={
                            resolutionNote
                        }

                        onChange={(event) =>
                            onNoteChange(
                                event.target.value
                            )
                        }

                        placeholder="Enter resolution note..."

                        className="
                            w-full
                            rounded-xl
                            border
                            border-slate-300
                            px-4
                            py-2.5
                            text-sm
                            outline-none
                            focus:border-[#f25d19]
                        "
                    />

                </div>

            </div>


            {disposition ===
                "RETURN_TO_SUPPLIER" && (

                    <div className="mt-4">

                        <AlertMessage
                            type="warning"
                            message="The confirmed quantity will be removed from quarantine inventory and returned to the supplier."
                        />

                    </div>

                )}


            {disposition ===
                "DISPOSE" && (

                    <div className="mt-4">

                        <AlertMessage
                            type="warning"
                            message="Disposal will permanently reduce the physical inventory quantity."
                        />

                    </div>

                )}


            <div
                className="
                    mt-5
                    flex
                    justify-end
                "
            >

                <button
                    type="button"

                    disabled={
                        processing
                    }

                    onClick={
                        onSubmit
                    }

                    className="
                        rounded-xl
                        bg-[#f25d19]
                        px-5
                        py-2.5
                        text-sm
                        font-semibold
                        text-white
                        transition
                        hover:bg-[#d94f12]
                        disabled:cursor-not-allowed
                        disabled:opacity-50
                    "
                >
                    {processing
                        ? "Processing..."
                        : "Confirm Resolution"}
                </button>

            </div>

        </div>
    );
}