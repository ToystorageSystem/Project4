
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { formatBusinessStatus } from "../../../utils/viStatus";

import {
    getShipmentManifestDetail,
    shipmentManifestErrorMessage,
} from "../../../api/business/shipmentManifests";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";

import Alert from "../../../components/ui/Alert";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business/shipment-manifests";

const formatNumber = (value) =>
    Number(value ?? 0).toLocaleString("en-GB");

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString("en-GB");
}

function formatLocalDate(value) {
    if (!value) return "—";
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value));
    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
}

const transportLabels = {
    CREATED: "Created",
    ASSIGNED: "Assigned",
    ACCEPTED: "Trip Accepted",
    REJECTED: "Rejected",
    READY_TO_SHIP: "Ready for Delivery",
    IN_TRANSIT: "In Transit",
    ARRIVED: "Arrived",
    DELIVERED: "Delivered",
    FAILED: "Delivery Failed",
    CANCELLED: "Cancelled",
};

function StatusBadge({ status }) {
    const colors = {
        DELIVERED: "bg-green-100 text-green-700",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
    };

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                colors[status] || "bg-slate-100 text-slate-700"
            }`}
        >
            {transportLabels[status] || formatBusinessStatus(status)}
        </span>
    );
}

function InfoItem({ label, value }) {
    return (
        <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">
                {label}
            </p>
            <p className="break-words text-sm font-semibold text-slate-800">
                {value ?? "—"}
            </p>
        </div>
    );
}

const transferColumns = [
    {
        key: "transferCode",
        label: "Stock Transfer Code",
        render: (value, item) => (
            <Link
                to={`/business/stock-transfers/${item.transferId}`}
                className="font-semibold text-[#c94810] hover:underline"
            >
                {value}
            </Link>
        ),
    },
    {
        key: "transferType",
        label: "Transfer Type",
    },
    {
        key: "transferStatus",
        label: "Status",
        render: (value) => value ? formatBusinessStatus(value) : "—",
    },
    {
        key: "totalProducts",
        label: "Product Count",
        render: formatNumber,
    },
    {
        key: "totalRequestedQuantity",
        label: "Requested Quantity",
        render: formatNumber,
    },
    {
        key: "expectedShipmentDate",
        label: "Expected Dispatch",
        render: (value) => formatLocalDate(value),
    },
    {
        key: "expectedReceiptDate",
        label: "Expected Receipt Date",
        render: (value) => formatLocalDate(value),
    },
];

const productColumns = [
    {
        key: "productCode",
        label: "Product Code",
    },
    {
        key: "productName",
        label: "Product Name",
    },
    {
        key: "barcode",
        label: "Barcode",
    },
    {
        key: "baseUnit",
        label: "Unit",
    },
    {
        key: "quantity",
        label: "Quantity",
        render: formatNumber,
    },
];

export default function ShipmentManifestDetailPage() {
    const { manifestId } = useParams();

    const [manifest, setManifest] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setManifest(null);

        getShipmentManifestDetail(
            manifestId,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!data?.manifestId) {
                    throw new Error("Invalid shipment manifest data.");
                }

                setManifest(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                setError(
                    err?.isAxiosError
                        ? shipmentManifestErrorMessage(err)
                        : err.message || "Unable to load shipment manifest details."
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [manifestId]);

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Shipment Manifest Details"
                    description="Track shipment details, stock transfers, and packages."
                    actions={
                        <Link
                            to={ROOT}
                            className="rounded-xl border border-orange-500 px-4 py-2 text-sm font-medium text-orange-600 hover:bg-orange-50"
                        >
                            Back to List
                        </Link>
                    }
                />

                {loading ? (
                    <div
                        role="status"
                        className="flex items-center gap-3 rounded-xl bg-white p-8 text-slate-600"
                    >
                        <Spinner />
                        Loading manifest details...
                    </div>
                ) : error ? (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                ) : !manifest ? (
                    <EmptyState
                        title="Manifest Not Found"
                        description="No details available to display."
                    />
                ) : (
                    <>
                        <Alert type="info">
                            Business Operations users can view and track shipment manifests but cannot edit, cancel, or change package contents.
                        </Alert>

                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <p className="text-sm text-slate-500">
                                        Manifest Code
                                    </p>
                                    <h2 className="text-xl font-bold text-slate-900">
                                        {manifest.manifestCode}
                                    </h2>
                                </div>

                                <StatusBadge
                                    status={manifest.transportStatus}
                                />
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Manifest Status"
                                    value={manifest.manifestStatus}
                                />
                                <InfoItem
                                    label="Created By"
                                    value={manifest.createdByName}
                                />
                                <InfoItem
                                    label="Created At"
                                    value={formatDate(manifest.createdAt)}
                                />
                                <InfoItem
                                    label="Created By User ID"
                                    value={manifest.createdByCode}
                                />
                            </div>
                        </Card>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Origin
                                </h3>
                                <InfoItem
                                    label="Warehouse / Store"
                                    value={manifest.fromWarehouseName}
                                />
                                <div className="mt-3">
                                    <InfoItem
                                        label="Origin Code"
                                        value={manifest.fromWarehouseCode}
                                    />
                                </div>
                                <div className="mt-3">
                                    <InfoItem
                                        label="Address"
                                        value={manifest.fromWarehouseAddress}
                                    />
                                </div>
                            </Card>

                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Destination
                                </h3>
                                <InfoItem
                                    label="Warehouse / Store"
                                    value={manifest.toWarehouseName}
                                />
                                <div className="mt-3">
                                    <InfoItem
                                        label="Destination Code"
                                        value={manifest.toWarehouseCode}
                                    />
                                </div>
                                <div className="mt-3">
                                    <InfoItem
                                        label="Address"
                                        value={manifest.toWarehouseAddress}
                                    />
                                </div>
                            </Card>
                        </div>

                        <div className="grid gap-4 sm:grid-cols-3">
                            {[
                                ["Total Stock Transfers", manifest.totalTransfers],
                                ["Total Packages", manifest.totalPackages],
                                ["Total Product Quantity", manifest.totalProductQuantity],
                            ].map(([label, value]) => (
                                <Card key={label}>
                                    <p className="text-sm text-slate-500">
                                        {label}
                                    </p>
                                    <p className="mt-2 text-3xl font-bold text-[#c94810]">
                                        {formatNumber(value)}
                                    </p>
                                </Card>
                            ))}
                        </div>

                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Shipment Information
                            </h3>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Delivery Trip Code"
                                    value={manifest.deliveryCode}
                                />
                                <InfoItem
                                    label="Driver"
                                    value={manifest.driverName}
                                />
                                <InfoItem
                                    label="Start Time"
                                    value={formatDate(manifest.startedAt)}
                                />
                                <InfoItem
                                    label="Delivery Completion Time"
                                    value={formatDate(manifest.deliveredAt)}
                                />
                            </div>

                            {manifest.note && (
                                <p className="mt-4 text-sm text-slate-600">
                                    <strong>Notes:</strong> {manifest.note}
                                </p>
                            )}
                        </Card>

                        <div className="space-y-3">
                            <h3 className="text-lg font-bold text-slate-800">
                                Stock Transfers
                            </h3>

                            {manifest.transfers?.length ? (
                                <Table
                                    columns={transferColumns}
                                    data={manifest.transfers}
                                    rowKey="transferId"
                                />
                            ) : (
                                <EmptyState
                                    title="No Stock Transfers"
                                />
                            )}
                        </div>

                        <div className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Packages
                            </h3>

                            {!manifest.packages?.length ? (
                                <EmptyState title="No Packages" />
                            ) : (
                                manifest.packages.map((pkg) => (
                                    <Card key={pkg.packageId}>
                                        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                            <h4 className="text-lg font-bold text-slate-800">
                                                {pkg.packageCode}
                                            </h4>

                                            <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                {pkg.packageStatus}
                                            </span>
                                        </div>

                                        <div className="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                            <InfoItem
                                                label="Seal Code"
                                                value={pkg.sealNumber}
                                            />
                                            <InfoItem
                                                label="Packed By"
                                                value={pkg.packedByName}
                                            />
                                            <InfoItem
                                                label="Packed At"
                                                value={formatDate(pkg.packedAt)}
                                            />
                                            <InfoItem
                                                label="Total Quantity"
                                                value={formatNumber(pkg.totalQuantity)}
                                            />
                                        </div>

                                        <h5 className="mb-3 font-semibold text-slate-800">
                                            Package Items
                                        </h5>

                                        {pkg.products?.length ? (
                                            <Table
                                                columns={productColumns}
                                                data={pkg.products}
                                                rowKey="productId"
                                            />
                                        ) : (
                                            <EmptyState
                                                title="This package contains no products"
                                            />
                                        )}
                                    </Card>
                                ))
                            )}
                        </div>
                    </>
                )}
            </div>
        </MainLayout>
    );
}