import LocationTypeBadge, {
    LocationStatusBadge,
} from "./LocationTypeBadge";


export default function WarehouseLocationTable({
                                                   locations = [],
                                                   onEdit,
                                                   onToggleStatus,
                                               }) {
    if (!locations.length) {
        return (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">

                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">

                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        className="h-7 w-7"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M3 21h18M5 21V8h14v13M8 12h8M8 16h8M9 8V4h6v4"
                        />
                    </svg>

                </div>

                <h3 className="mt-4 font-semibold text-slate-800">
                    No warehouse locations
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                    Create a location to start organizing the warehouse.
                </p>

            </div>
        );
    }


    return (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

            <div className="overflow-x-auto">

                <table className="min-w-full divide-y divide-slate-200">

                    <thead className="bg-slate-50">

                    <tr>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Location
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Name
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Zone
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Shelf
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Type
                        </th>

                        <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Status
                        </th>

                        <th className="px-5 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">
                            Action
                        </th>

                    </tr>

                    </thead>


                    <tbody className="divide-y divide-slate-100">

                    {locations.map(
                        (location) => {

                            const active =
                                location.status ===
                                "ACTIVE";

                            return (
                                <tr
                                    key={location.id}
                                    className="transition hover:bg-[#fffaf7]"
                                >

                                    <td className="px-5 py-4">

                                        <p className="font-semibold text-slate-900">
                                            {location.warehouseCode ?? "-"}
                                        </p>

                                        <p className="mt-1 text-xs text-slate-400">
                                            {location.warehouseLocationsCode ?? ""}
                                        </p>

                                    </td>


                                    <td className="px-5 py-4 text-sm font-medium text-slate-700">

                                        {location.name ?? "-"}

                                    </td>


                                    <td className="px-5 py-4">

                                            <span className="inline-flex min-w-9 justify-center rounded-lg bg-slate-100 px-2.5 py-1 text-sm font-semibold text-slate-700">
                                                {location.zone ?? "-"}
                                            </span>

                                    </td>


                                    <td className="px-5 py-4 text-sm text-slate-600">

                                        {location.shelf ?? "-"}

                                    </td>


                                    <td className="px-5 py-4">

                                        <LocationTypeBadge
                                            type={
                                                location.locationType
                                            }
                                        />

                                    </td>


                                    <td className="px-5 py-4">

                                        <LocationStatusBadge
                                            status={
                                                location.status
                                            }
                                        />

                                    </td>


                                    <td className="px-5 py-4">

                                        <div className="flex justify-end gap-2">

                                            <button
                                                type="button"
                                                onClick={() =>
                                                    onEdit?.(
                                                        location
                                                    )
                                                }
                                                className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 transition hover:border-[#f25d19] hover:text-[#f25d19]"
                                            >
                                                Edit
                                            </button>


                                            <button
                                                type="button"
                                                onClick={() =>
                                                    onToggleStatus?.(
                                                        location
                                                    )
                                                }
                                                className={
                                                    "rounded-xl border px-3 py-2 text-xs font-semibold transition " +
                                                    (
                                                        active
                                                            ? "border-red-200 text-red-600 hover:bg-red-50"
                                                            : "border-emerald-200 text-emerald-600 hover:bg-emerald-50"
                                                    )
                                                }
                                            >
                                                {active
                                                    ? "Deactivate"
                                                    : "Activate"}
                                            </button>

                                        </div>

                                    </td>

                                </tr>
                            );
                        }
                    )}

                    </tbody>

                </table>

            </div>

        </div>
    );
}