import { useState } from "react";

import PackingStatusBadge
    from "./PackingStatusBadge";

export default function PackingPackageCards({
                                                packages = [],
                                            }) {
    const [expandedId, setExpandedId] =
        useState(null);

    if (!packages.length) {
        return (
            <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500">
                No packages found for this transfer.
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2 2xl:grid-cols-3">
            {packages.map((pack) => {
                const expanded =
                    expandedId === pack.packageId;

                return (
                    <PackageCard
                        key={pack.packageId}
                        pack={pack}
                        expanded={expanded}
                        onToggle={() =>
                            setExpandedId(
                                expanded
                                    ? null
                                    : pack.packageId
                            )
                        }
                    />
                );
            })}
        </div>
    );
}


function PackageCard({
                         pack,
                         expanded,
                         onToggle,
                     }) {
    const items =
        Array.isArray(pack.items)
            ? pack.items
            : [];

    return (
        <div
            className="
                overflow-hidden
                rounded-2xl
                border
                border-slate-200
                bg-white
                shadow-sm
                transition
                hover:-translate-y-0.5
                hover:shadow-md
            "
        >
            {/* CARD HEADER */}
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 p-5">
                <div className="flex min-w-0 items-center gap-4">

                    <div
                        className="
                            flex
                            h-12
                            w-12
                            shrink-0
                            items-center
                            justify-center
                            rounded-xl
                            bg-[#fff1e9]
                            text-[#f25d19]
                        "
                    >
                        <PackageIcon />
                    </div>

                    <div className="min-w-0">
                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                            Package
                        </p>

                        <h3 className="mt-1 truncate text-base font-bold text-slate-900">
                            {pack.packageCode || "-"}
                        </h3>
                    </div>
                </div>

                <PackingStatusBadge
                    status={pack.status}
                />
            </div>


            {/* CARD BODY */}
            <div className="space-y-4 p-5">

                <div className="grid grid-cols-2 gap-3">

                    <InfoBox
                        label="Seal Number"
                        value={
                            pack.sealNumber ||
                            "Not sealed"
                        }
                    />

                    <InfoBox
                        label="Quantity"
                        value={
                            pack.totalQuantity ??
                            0
                        }
                    />

                </div>


                {/* SEAL STATUS */}
                <div
                    className={`
                        flex
                        items-center
                        justify-between
                        rounded-xl
                        border
                        px-4
                        py-3
                        ${
                        pack.sealed
                            ? "border-emerald-200 bg-emerald-50"
                            : "border-red-200 bg-red-50"
                    }
                    `}
                >
                    <div className="flex items-center gap-3">

                        <div
                            className={`
                                flex
                                h-8
                                w-8
                                items-center
                                justify-center
                                rounded-full
                                text-sm
                                font-bold
                                ${
                                pack.sealed
                                    ? "bg-emerald-100 text-emerald-700"
                                    : "bg-red-100 text-red-600"
                            }
                            `}
                        >
                            {pack.sealed
                                ? "✓"
                                : "!"}
                        </div>

                        <div>
                            <p
                                className={`
                                    text-sm
                                    font-semibold
                                    ${
                                    pack.sealed
                                        ? "text-emerald-800"
                                        : "text-red-800"
                                }
                                `}
                            >
                                {pack.sealed
                                    ? "Package sealed"
                                    : "Seal required"}
                            </p>

                            <p
                                className={`
                                    mt-0.5
                                    text-xs
                                    ${
                                    pack.sealed
                                        ? "text-emerald-600"
                                        : "text-red-600"
                                }
                                `}
                            >
                                {pack.sealed
                                    ? "Seal information verified"
                                    : "This package is not ready"}
                            </p>
                        </div>
                    </div>
                </div>


                {/* USERS */}
                <div className="space-y-3 border-t border-slate-100 pt-4">

                    <UserRow
                        label="Packed by"
                        value={
                            pack.packedByName ||
                            "-"
                        }
                    />

                    <UserRow
                        label="Checked by"
                        value={
                            pack.checkedByName ||
                            "Not checked"
                        }
                    />

                </div>


                {/* ITEMS COUNT */}
                <div className="flex items-center justify-between border-t border-slate-100 pt-4">

                    <div>
                        <p className="text-xs text-slate-400">
                            Products
                        </p>

                        <p className="mt-1 text-sm font-semibold text-slate-800">
                            {items.length} item
                            {items.length !== 1
                                ? "s"
                                : ""}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onToggle}
                        className="
                            inline-flex
                            items-center
                            gap-2
                            rounded-xl
                            border
                            border-slate-200
                            bg-white
                            px-4
                            py-2
                            text-sm
                            font-semibold
                            text-slate-600
                            transition
                            hover:border-orange-200
                            hover:bg-orange-50
                            hover:text-[#f25d19]
                        "
                    >
                        {expanded
                            ? "Hide Items"
                            : "View Items"}

                        <svg
                            className={`
                                h-4
                                w-4
                                transition-transform
                                ${
                                expanded
                                    ? "rotate-180"
                                    : ""
                            }
                            `}
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                d="m6 9 6 6 6-6"
                            />
                        </svg>
                    </button>

                </div>

            </div>


            {/* ITEMS */}
            {expanded && (
                <div className="border-t border-slate-100 bg-slate-50 p-5">
                    <PackageItems
                        items={items}
                    />
                </div>
            )}

        </div>
    );
}


function InfoBox({
                     label,
                     value,
                 }) {
    return (
        <div className="rounded-xl bg-slate-50 p-4">
            <p className="text-xs font-medium text-slate-400">
                {label}
            </p>

            <p className="mt-1 truncate text-sm font-bold text-slate-900">
                {value}
            </p>
        </div>
    );
}


function UserRow({
                     label,
                     value,
                 }) {
    return (
        <div className="flex items-center justify-between gap-4">

            <span className="text-xs text-slate-400">
                {label}
            </span>

            <span className="truncate text-sm font-medium text-slate-700">
                {value}
            </span>

        </div>
    );
}


function PackageItems({
                          items,
                      }) {
    if (!items.length) {
        return (
            <div className="rounded-xl border border-dashed border-slate-300 bg-white p-5 text-center text-sm text-slate-500">
                This package contains no items.
            </div>
        );
    }

    return (
        <div className="space-y-2">

            <p className="mb-3 text-xs font-bold uppercase tracking-wider text-slate-400">
                Package Items
            </p>

            {items.map(
                (
                    item,
                    index
                ) => (
                    <div
                        key={
                            item.productId ??
                            index
                        }
                        className="
                            flex
                            items-center
                            justify-between
                            gap-4
                            rounded-xl
                            border
                            border-slate-200
                            bg-white
                            px-4
                            py-3
                        "
                    >

                        <div className="min-w-0">

                            <p className="truncate text-sm font-semibold text-slate-900">
                                {item.productName ||
                                    "-"}
                            </p>

                            <p className="mt-1 text-xs text-slate-400">
                                Product ID:{" "}
                                {item.productId ??
                                    "-"}
                            </p>

                        </div>

                        <div className="shrink-0 text-right">

                            <p className="text-xs text-slate-400">
                                Qty
                            </p>

                            <p className="mt-1 text-sm font-bold text-slate-900">
                                {item.packedQuantity ??
                                    0}
                            </p>

                        </div>

                    </div>
                )
            )}

        </div>
    );
}


function PackageIcon() {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            className="h-6 w-6"
        >
            <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M21 8 12 3 3 8l9 5 9-5Zm-18 0v8l9 5 9-5V8M12 13v8"
            />
        </svg>
    );
}