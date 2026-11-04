import {
    useEffect,
    useState,
} from "react";


const EMPTY_FORM = {
    warehouseCode: "",
    name: "",
    zone: "",
    shelf: "",
    locationType: "NORMAL",
    status: "ACTIVE",
};


const LOCATION_TYPES = [
    {
        value: "NORMAL",
        label: "Normal storage",
    },
    {
        value: "RECEIVING",
        label: "Receiving area",
    },
    {
        value: "DAMAGED",
        label: "Damaged goods",
    },
    {
        value: "QUARANTINE",
        label: "Quarantine",
    },
    {
        value: "RETURN",
        label: "Return area",
    },
];


function FieldLabel({
                        children,
                        required = false,
                    }) {
    return (
        <label className="mb-1.5 block text-sm font-semibold text-slate-700">

            {children}

            {required && (
                <span className="ml-1 text-red-500">
                    *
                </span>
            )}

        </label>
    );
}


export default function WarehouseLocationFormModal({
                                                       open,
                                                       location,
                                                       saving = false,
                                                       onClose,
                                                       onSubmit,
                                                   }) {
    const [
        form,
        setForm,
    ] = useState(EMPTY_FORM);


    const [
        error,
        setError,
    ] = useState("");


    useEffect(() => {
        if (!open) {
            return;
        }

        if (location) {
            setForm({
                warehouseCode:
                    location.warehouseCode ?? "",

                name:
                    location.name ?? "",

                zone:
                    location.zone ?? "",

                shelf:
                    location.shelf ?? "",

                locationType:
                    location.locationType ??
                    "NORMAL",

                status:
                    location.status ??
                    "ACTIVE",
            });
        } else {
            setForm(
                EMPTY_FORM
            );
        }

        setError("");
    }, [
        open,
        location,
    ]);


    if (!open) {
        return null;
    }


    const handleChange =
        (event) => {

            const {
                name,
                value,
            } = event.target;

            setForm(
                (previous) => ({
                    ...previous,

                    [name]:
                    value,
                })
            );
        };


    const handleSubmit =
        async (event) => {

            event.preventDefault();

            setError("");


            if (
                !form.warehouseCode.trim()
            ) {
                setError(
                    "Location code is required."
                );

                return;
            }


            if (!form.name.trim()) {
                setError(
                    "Location name is required."
                );

                return;
            }


            if (!form.zone.trim()) {
                setError(
                    "Zone is required."
                );

                return;
            }


            if (!form.shelf.trim()) {
                setError(
                    "Shelf is required."
                );

                return;
            }


            try {

                await onSubmit?.({
                    warehouseCode:
                        form.warehouseCode
                            .trim()
                            .toUpperCase(),

                    name:
                        form.name.trim(),

                    zone:
                        form.zone
                            .trim()
                            .toUpperCase(),

                    shelf:
                        form.shelf
                            .trim()
                            .toUpperCase(),

                    locationType:
                    form.locationType,

                    status:
                    form.status,
                });

            } catch (submitError) {

                const data =
                    submitError?.response?.data;

                setError(
                    typeof data === "string"
                        ? data
                        : data?.message ??
                        "Unable to save warehouse location."
                );
            }
        };


    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">

            <div className="w-full max-w-2xl overflow-hidden rounded-3xl bg-white shadow-2xl">

                {/* HEADER */}

                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5">

                    <div>

                        <p className="text-xs font-semibold uppercase tracking-[0.15em] text-[#f25d19]">
                            Warehouse Manager
                        </p>

                        <h2 className="mt-1 text-xl font-bold text-slate-900">
                            {location
                                ? "Edit Location"
                                : "Add Location"}
                        </h2>

                    </div>


                    <button
                        type="button"
                        disabled={saving}
                        onClick={onClose}
                        className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    >
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            className="h-5 w-5"
                        >
                            <path
                                strokeLinecap="round"
                                strokeWidth="2"
                                d="M6 6l12 12M18 6 6 18"
                            />
                        </svg>
                    </button>

                </div>


                <form
                    onSubmit={handleSubmit}
                    className="p-6"
                >

                    {error && (
                        <div className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {error}
                        </div>
                    )}


                    <div className="grid gap-5 sm:grid-cols-2">

                        <div>

                            <FieldLabel required>
                                Location Code
                            </FieldLabel>

                            <input
                                name="warehouseCode"
                                value={
                                    form.warehouseCode
                                }
                                onChange={
                                    handleChange
                                }
                                placeholder="Example: A03"
                                className="h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            />

                        </div>


                        <div>

                            <FieldLabel required>
                                Location Name
                            </FieldLabel>

                            <input
                                name="name"
                                value={form.name}
                                onChange={
                                    handleChange
                                }
                                placeholder="Example: Kệ A03"
                                className="h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            />

                        </div>


                        <div>

                            <FieldLabel required>
                                Zone
                            </FieldLabel>

                            <input
                                name="zone"
                                value={form.zone}
                                onChange={
                                    handleChange
                                }
                                placeholder="Example: A"
                                className="h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            />

                        </div>


                        <div>

                            <FieldLabel required>
                                Shelf
                            </FieldLabel>

                            <input
                                name="shelf"
                                value={form.shelf}
                                onChange={
                                    handleChange
                                }
                                placeholder="Example: A03-01"
                                className="h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            />

                        </div>


                        <div>

                            <FieldLabel required>
                                Location Type
                            </FieldLabel>

                            <select
                                name="locationType"
                                value={
                                    form.locationType
                                }
                                onChange={
                                    handleChange
                                }
                                className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            >

                                {LOCATION_TYPES.map(
                                    (type) => (
                                        <option
                                            key={
                                                type.value
                                            }
                                            value={
                                                type.value
                                            }
                                        >
                                            {type.label}
                                        </option>
                                    )
                                )}

                            </select>

                        </div>


                        <div>

                            <FieldLabel required>
                                Status
                            </FieldLabel>

                            <select
                                name="status"
                                value={
                                    form.status
                                }
                                onChange={
                                    handleChange
                                }
                                className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                            >

                                <option value="ACTIVE">
                                    Active
                                </option>

                                <option value="INACTIVE">
                                    Inactive
                                </option>

                            </select>

                        </div>

                    </div>


                    <div className="mt-6 rounded-2xl bg-[#fff8f4] p-4">

                        <p className="text-sm font-semibold text-[#d94f12]">
                            Example
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                            Zone A → Shelf A03-01 → Location A03.
                            Putaway staff can later use this location
                            as a destination.
                        </p>

                    </div>


                    <div className="mt-6 flex justify-end gap-3">

                        <button
                            type="button"
                            disabled={saving}
                            onClick={onClose}
                            className="h-11 rounded-xl border border-slate-200 px-5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
                        >
                            Cancel
                        </button>


                        <button
                            type="submit"
                            disabled={saving}
                            className="h-11 rounded-xl bg-[#f25d19] px-6 text-sm font-semibold text-white transition hover:bg-[#d94f12] disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {saving
                                ? "Saving..."
                                : location
                                    ? "Save Changes"
                                    : "Create Location"}
                        </button>

                    </div>

                </form>

            </div>

        </div>
    );
}