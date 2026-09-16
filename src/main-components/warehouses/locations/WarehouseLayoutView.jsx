import {
    useMemo,
} from "react";

import LocationTypeBadge
    from "./LocationTypeBadge";


export default function WarehouseLayoutView({
                                                locations = [],
                                            }) {
    const grouped =
        useMemo(
            () => {

                const zones = {};


                locations.forEach(
                    (location) => {

                        const zone =
                            location.zone ||
                            "NO-ZONE";

                        const shelf =
                            location.shelf ||
                            "NO-SHELF";


                        if (!zones[zone]) {
                            zones[zone] = {};
                        }


                        if (!zones[zone][shelf]) {
                            zones[zone][shelf] = [];
                        }


                        zones[zone][shelf]
                            .push(location);
                    }
                );


                return zones;
            },
            [locations]
        );


    const zoneEntries =
        Object.entries(grouped)
            .sort(
                ([a], [b]) =>
                    a.localeCompare(b)
            );


    if (!zoneEntries.length) {
        return (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white py-14 text-center text-sm text-slate-500">
                No warehouse layout data.
            </div>
        );
    }


    return (
        <div className="space-y-5">

            {zoneEntries.map(
                ([
                     zone,
                     shelves,
                 ]) => (

                    <section
                        key={zone}
                        className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                    >

                        {/* ZONE */}

                        <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-5 py-4">

                            <div className="flex items-center gap-3">

                                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1e9] font-bold text-[#f25d19]">
                                    {zone}
                                </div>


                                <div>

                                    <h3 className="font-bold text-slate-900">
                                        Zone {zone}
                                    </h3>

                                    <p className="text-xs text-slate-500">
                                        {
                                            Object.keys(
                                                shelves
                                            ).length
                                        } shelves
                                    </p>

                                </div>

                            </div>

                        </div>


                        <div className="grid gap-4 p-5 lg:grid-cols-2 xl:grid-cols-3">

                            {Object.entries(
                                shelves
                            )
                                .sort(
                                    ([a], [b]) =>
                                        a.localeCompare(
                                            b
                                        )
                                )
                                .map(
                                    ([
                                         shelf,
                                         shelfLocations,
                                     ]) => (

                                        <div
                                            key={
                                                `${zone}-${shelf}`
                                            }
                                            className="rounded-2xl border border-slate-200 p-4"
                                        >

                                            <div className="flex items-center justify-between">

                                                <div>

                                                    <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                                                        Shelf
                                                    </p>

                                                    <h4 className="mt-1 font-bold text-slate-800">
                                                        {shelf}
                                                    </h4>

                                                </div>


                                                <div className="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                                                    {
                                                        shelfLocations.length
                                                    } locations
                                                </div>

                                            </div>


                                            <div className="mt-4 space-y-2">

                                                {shelfLocations.map(
                                                    (
                                                        location
                                                    ) => (

                                                        <div
                                                            key={
                                                                location.id
                                                            }
                                                            className="rounded-xl bg-slate-50 px-3 py-3"
                                                        >

                                                            <div className="flex items-center justify-between gap-3">

                                                                <div className="min-w-0">

                                                                    <p className="truncate text-sm font-bold text-slate-800">
                                                                        {location.warehouseCode}
                                                                    </p>

                                                                    <p className="mt-0.5 truncate text-xs text-slate-500">
                                                                        {location.name}
                                                                    </p>

                                                                </div>


                                                                <LocationTypeBadge
                                                                    type={
                                                                        location.locationType
                                                                    }
                                                                />

                                                            </div>

                                                        </div>
                                                    )
                                                )}

                                            </div>

                                        </div>

                                    )
                                )}

                        </div>

                    </section>
                )
            )}

        </div>
    );
}