function SummaryCard({
                         title,
                         value,
                         description,
                     }) {
    return (
        <div
            className="
                rounded-xl
                border
                border-gray-200
                bg-white
                p-5
                shadow-sm
            "
        >
            <p
                className="
                    text-sm
                    font-medium
                    text-gray-500
                "
            >
                {title}
            </p>

            <p
                className="
                    mt-2
                    text-2xl
                    font-bold
                    text-gray-900
                "
            >
                {value}
            </p>

            {description && (
                <p
                    className="
                        mt-1
                        text-xs
                        text-gray-500
                    "
                >
                    {description}
                </p>
            )}
        </div>
    );
}

export default function PackingSummaryCards({
                                                data,
                                            }) {
    const packages =
        Array.isArray(data?.packages)
            ? data.packages
            : [];

    const sealedPackages =
        packages.filter(
            (item) => item.sealed
        ).length;

    const packedPackages =
        packages.filter(
            (item) =>
                item.status === "PACKED" ||
                item.status === "CHECKED"
        ).length;

    return (
        <div
            className="
                grid
                grid-cols-1
                gap-4
                sm:grid-cols-2
                xl:grid-cols-4
            "
        >
            <SummaryCard
                title="Packages"
                value={packages.length}
                description={`${packedPackages}/${packages.length} packed`}
            />

            <SummaryCard
                title="Expected Quantity"
                value={
                    data?.expectedQuantity ?? 0
                }
                description="Quantity required by transfer"
            />

            <SummaryCard
                title="Packed Quantity"
                value={
                    data?.packedQuantity ?? 0
                }
                description={
                    data?.quantityMatched
                        ? "Quantity matched"
                        : "Quantity mismatch"
                }
            />

            <SummaryCard
                title="Sealed Packages"
                value={`${sealedPackages}/${packages.length}`}
                description={
                    data?.allPackagesSealed
                        ? "All packages sealed"
                        : "Waiting for sealing"
                }
            />
        </div>
    );
}