
import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    createWarehouseLocation,
    getWarehouseLocations,
    updateWarehouseLocation,
    updateWarehouseLocationStatus,
} from "../../../api/warehouses/locations/warehouseLocations.js";

import WarehouseLocationTable
    from "./WarehouseLocationTable";

import WarehouseLocationFormModal
    from "./WarehouseLocationFormModal";

import WarehouseLayoutView
    from "./WarehouseLayoutView";


const TYPE_OPTIONS = [
    "ALL",
    "NORMAL",
    "RECEIVING",
    "DAMAGED",
    "QUARANTINE",
    "RETURN",
];


function getErrorMessage(error) {
    const data =
        error?.response?.data;

    if (
        typeof data === "string"
    ) {
        return data;
    }

    return (
        data?.message ??
        error?.message ??
        "Unable to load warehouse locations."
    );
}


function StatCard({
                      title,
                      value,
                      description,
                  }) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

            <p className="text-sm font-medium text-slate-500">
                {title}
            </p>

            <p className="mt-2 text-3xl font-bold text-slate-900">
                {value}
            </p>

            <p className="mt-1 text-xs text-slate-400">
                {description}
            </p>

        </div>
    );
}


export default function WarehouseLocationsMain() {
    const [
        locations,
        setLocations,
    ] = useState([]);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        saving,
        setSaving,
    ] = useState(false);


    const [
        error,
        setError,
    ] = useState("");


    const [
        search,
        setSearch,
    ] = useState("");


    const [
        zone,
        setZone,
    ] = useState("ALL");


    const [
        type,
        setType,
    ] = useState("ALL");


    const [
        status,
        setStatus,
    ] = useState("ALL");


    const [
        view,
        setView,
    ] = useState("TABLE");


    const [
        modalOpen,
        setModalOpen,
    ] = useState(false);


    const [
        editingLocation,
        setEditingLocation,
    ] = useState(null);


    const loadLocations =
        useCallback(
            async () => {

                try {

                    setLoading(true);
                    setError("");


                    const data =
                        await getWarehouseLocations();


                    setLocations(
                        Array.isArray(data)
                            ? data
                            : []
                    );

                } catch (err) {

                    console.error(
                        "Load warehouse locations error:",
                        err
                    );

                    setError(
                        getErrorMessage(err)
                    );

                } finally {

                    setLoading(false);

                }
            },
            []
        );


    useEffect(() => {
        loadLocations();
    }, [loadLocations]);


    const zones =
        useMemo(
            () => {

                return [
                    ...new Set(
                        locations
                            .map(
                                (item) =>
                                    item.zone
                            )
                            .filter(Boolean)
                    ),
                ].sort();

            },
            [locations]
        );


    const filteredLocations =
        useMemo(
            () => {

                const keyword =
                    search
                        .trim()
                        .toLowerCase();


                return locations.filter(
                    (item) => {

                        if (
                            zone !== "ALL" &&
                            item.zone !== zone
                        ) {
                            return false;
                        }


                        if (
                            type !== "ALL" &&
                            item.locationType !==
                            type
                        ) {
                            return false;
                        }


                        if (
                            status !== "ALL" &&
                            item.status !==
                            status
                        ) {
                            return false;
                        }


                        if (!keyword) {
                            return true;
                        }


                        const searchable =
                            [
                                item.warehouseCode,
                                item.warehouseLocationsCode,
                                item.name,
                                item.zone,
                                item.shelf,
                                item.locationType,
                                item.status,
                            ]
                                .filter(Boolean)
                                .join(" ")
                                .toLowerCase();


                        return searchable
                            .includes(keyword);

                    }
                );

            },
            [
                locations,
                search,
                zone,
                type,
                status,
            ]
        );


    const activeCount =
        locations.filter(
            (item) =>
                item.status === "ACTIVE"
        ).length;


    const normalCount =
        locations.filter(
            (item) =>
                item.locationType ===
                "NORMAL"
        ).length;


    const shelfCount =
        new Set(
            locations
                .map(
                    (item) =>
                        `${item.zone}-${item.shelf}`
                )
                .filter(
                    (value) =>
                        !value.includes(
                            "undefined"
                        )
                )
        ).size;


    const openCreate =
        () => {

            setEditingLocation(null);
            setModalOpen(true);
        };


    const openEdit =
        (location) => {

            setEditingLocation(
                location
            );

            setModalOpen(true);
        };


    const closeModal =
        () => {

            if (saving) {
                return;
            }

            setModalOpen(false);
            setEditingLocation(null);
        };


    const handleSave =
        async (payload) => {

            try {

                setSaving(true);


                if (editingLocation) {

                    await updateWarehouseLocation(
                        editingLocation.id,
                        payload
                    );

                } else {

                    await createWarehouseLocation(
                        payload
                    );

                }


                setModalOpen(false);
                setEditingLocation(null);

                await loadLocations();

            } finally {

                setSaving(false);

            }
        };


    const handleToggleStatus =
        async (location) => {

            const newStatus =
                location.status === "ACTIVE"
                    ? "INACTIVE"
                    : "ACTIVE";


            const confirmed =
                window.confirm(
                    `Change ${location.warehouseCode} to ${newStatus}?`
                );


            if (!confirmed) {
                return;
            }


            try {

                await updateWarehouseLocationStatus(
                    location.id,
                    newStatus
                );

                await loadLocations();

            } catch (err) {

                setError(
                    getErrorMessage(err)
                );

            }
        };


    return (
        <div className="space-y-6">

            {/* HEADER */}

            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

                <div>

                    <div className="flex items-center gap-2">

                        <div className="h-6 w-1 rounded-full bg-[#f25d19]" />

                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#f25d19]">
                            Warehouse Manager
                        </p>

                    </div>


                    <h1 className="mt-2 text-2xl font-bold text-slate-900">
                        Warehouse Locations
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        Manage zones, shelves, storage locations and location types.
                    </p>

                </div>


                <button
                    type="button"
                    onClick={openCreate}
                    className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#f25d19] px-5 text-sm font-semibold text-white transition hover:bg-[#d94f12]"
                >

                    <span className="text-xl leading-none">
                        +
                    </span>

                    Add Location

                </button>

            </div>


            {/* SUMMARY */}

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

                <StatCard
                    title="Locations"
                    value={
                        locations.length
                    }
                    description="Total warehouse locations"
                />

                <StatCard
                    title="Active"
                    value={
                        activeCount
                    }
                    description="Available locations"
                />

                <StatCard
                    title="Normal Storage"
                    value={
                        normalCount
                    }
                    description="Locations for normal goods"
                />

                <StatCard
                    title="Shelves"
                    value={
                        shelfCount
                    }
                    description={`${zones.length} warehouse zones`}
                />

            </div>


            {error && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                </div>
            )}


            {/* FILTERS */}

            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">

                <div className="flex flex-col gap-3 xl:flex-row">

                    <div className="relative flex-1">

                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                        >
                            <circle
                                cx="11"
                                cy="11"
                                r="7"
                                strokeWidth="2"
                            />

                            <path
                                strokeLinecap="round"
                                strokeWidth="2"
                                d="m20 20-3.5-3.5"
                            />
                        </svg>


                        <input
                            value={search}
                            onChange={
                                (event) =>
                                    setSearch(
                                        event.target.value
                                    )
                            }
                            placeholder="Search code, name, zone, shelf..."
                            className="h-11 w-full rounded-xl border border-slate-200 pl-10 pr-4 text-sm outline-none transition focus:border-[#f25d19] focus:ring-2 focus:ring-[#f25d19]/10"
                        />

                    </div>


                    <select
                        value={zone}
                        onChange={
                            (event) =>
                                setZone(
                                    event.target.value
                                )
                        }
                        className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-[#f25d19]"
                    >

                        <option value="ALL">
                            All Zones
                        </option>

                        {zones.map(
                            (zoneValue) => (
                                <option
                                    key={
                                        zoneValue
                                    }
                                    value={
                                        zoneValue
                                    }
                                >
                                    Zone {zoneValue}
                                </option>
                            )
                        )}

                    </select>


                    <select
                        value={type}
                        onChange={
                            (event) =>
                                setType(
                                    event.target.value
                                )
                        }
                        className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-[#f25d19]"
                    >

                        {TYPE_OPTIONS.map(
                            (value) => (
                                <option
                                    key={value}
                                    value={value}
                                >
                                    {value === "ALL"
                                        ? "All Types"
                                        : value}
                                </option>
                            )
                        )}

                    </select>


                    <select
                        value={status}
                        onChange={
                            (event) =>
                                setStatus(
                                    event.target.value
                                )
                        }
                        className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-[#f25d19]"
                    >

                        <option value="ALL">
                            All Status
                        </option>

                        <option value="ACTIVE">
                            Active
                        </option>

                        <option value="INACTIVE">
                            Inactive
                        </option>

                    </select>

                </div>

            </div>


            {/* VIEW SWITCH */}

            <div className="flex items-center gap-2">

                <button
                    type="button"
                    onClick={() =>
                        setView(
                            "TABLE"
                        )
                    }
                    className={
                        "rounded-xl px-4 py-2 text-sm font-semibold transition " +
                        (
                            view === "TABLE"
                                ? "bg-[#f25d19] text-white"
                                : "border border-slate-200 bg-white text-slate-600"
                        )
                    }
                >
                    Location List
                </button>


                <button
                    type="button"
                    onClick={() =>
                        setView(
                            "LAYOUT"
                        )
                    }
                    className={
                        "rounded-xl px-4 py-2 text-sm font-semibold transition " +
                        (
                            view === "LAYOUT"
                                ? "bg-[#f25d19] text-white"
                                : "border border-slate-200 bg-white text-slate-600"
                        )
                    }
                >
                    Zone & Shelf Layout
                </button>

            </div>


            {/* CONTENT */}

            {loading ? (

                <div className="flex min-h-[320px] items-center justify-center rounded-2xl border border-slate-200 bg-white">

                    <div className="text-center">

                        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-[#f25d19]" />

                        <p className="mt-3 text-sm text-slate-500">
                            Loading locations...
                        </p>

                    </div>

                </div>

            ) : view === "TABLE" ? (

                <WarehouseLocationTable
                    locations={
                        filteredLocations
                    }
                    onEdit={
                        openEdit
                    }
                    onToggleStatus={
                        handleToggleStatus
                    }
                />

            ) : (

                <WarehouseLayoutView
                    locations={
                        filteredLocations
                    }
                />

            )}


            <WarehouseLocationFormModal
                open={modalOpen}
                location={
                    editingLocation
                }
                saving={saving}
                onClose={
                    closeModal
                }
                onSubmit={
                    handleSave
                }
            />

        </div>
    );
}