
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { formatBusinessStatus } from "../../../utils/viStatus";

import {
    getInTransitShipmentDetail,
    inTransitShipmentErrorMessage,
} from "../../../api/business/inTransitShipments";

import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";

import Alert from "../../../components/ui/Alert";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

// Issue #13 - Chi tiet hang dang van chuyen.
// Business Staff chi duoc xem du lieu.

const ROOT = "/business/in-transit-shipments";

const STATUS_LABELS = {
    CREATED: "Created",
    ASSIGNED: "Assigned",
    ACCEPTED: "Trip Accepted",
    REJECTED: "Rejected",
    READY_TO_SHIP: "Ready for Delivery",
    IN_TRANSIT: "In Transit",
    ARRIVED: "Arrived at Destination",
    DELIVERED: "Delivered",
    FAILED: "Delivery Failed",
    CANCELLED: "Cancelled",
};

const TRANSFER_TYPE_LABELS = {
    STORE_RETURN: "Store Return to Warehouse",
    MAIN_TO_STORE: "Main Warehouse to Store",
    STORE_TO_STORE: "Store to Store",
};

function formatNumber(value) {
    if (value == null || value === "") return "—";

    const number = Number(value);

    return Number.isFinite(number)
        ? number.toLocaleString("en-GB")
        : String(value);
}

function formatDateTime(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString("en-GB", {
              hour12: false,
          });
}

function StatusBadge({ status }) {
    const colors = {
        DELIVERED: "bg-green-100 text-green-700",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        ARRIVED: "bg-teal-100 text-teal-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
        ASSIGNED: "bg-purple-100 text-purple-700",
    };

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                colors[status] || "bg-slate-100 text-slate-700"
            }`}
        >
            {STATUS_LABELS[status] || formatBusinessStatus(status)}
        </span>
    );
}

function InfoItem({ label, value }) {
    return (
        <div className="space-y-1">
            <p className="text-xs font-medium text-slate-500">
                {label}
            </p>

            <div className="break-words text-sm font-semibold text-slate-800">
                {value ?? "—"}
            </div>
        </div>
    );
}

// Bang danh sach phieu dieu chuyen.
const transferColumns = [
    {
        key: "transferCode",
        label: "Stock Transfer Code",
        render: (value, transfer) =>
            transfer.transferId != null ? (
                <Link
                    to={`/business/stock-transfers/${transfer.transferId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    {value || "—"}
                </Link>
            ) : (
                value || "—"
            ),
    },
    {
        key: "transferType",
        label: "Transfer Type",
        render: (value) =>
            TRANSFER_TYPE_LABELS[value] || value || "—",
    },
    {
        key: "transferStatus",
        label: "Status",
        render: (value) => value ? formatBusinessStatus(value) : "—",
    },
    {
        key: "totalRequestedQuantity",
        label: "Requested Quantity",
        render: formatNumber,
    },
    {
        key: "totalShippedQuantity",
        label: "Shipped Qty",
        render: formatNumber,
    },
    {
        key: "totalReceivedQuantity",
        label: "Received Qty",
        render: formatNumber,
    },
    {
        key: "expectedReceiptDate",
        label: "Expected Receipt Date",
        render: (value) => value || "—",
    },
];

// San pham ben trong phieu dieu chuyen.
const transferProductColumns = [
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
        key: "requestedQuantity",
        label: "Request",
        render: formatNumber,
    },
    {
        key: "approvedQuantity",
        label: "Approve",
        render: formatNumber,
    },
    {
        key: "packedQuantity",
        label: "Packing",
        render: formatNumber,
    },
    {
        key: "shippedQuantity",
        label: "Shipped",
        render: formatNumber,
    },
    {
        key: "receivedQuantity",
        label: "Received",
        render: formatNumber,
    },
];

// San pham thuc te trong tung kien hang.
const packageProductColumns = [
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
        label: "Packed Quantity",
        render: formatNumber,
    },
];

export default function InTransitShipmentDetailPage() {
    const { deliveryId } = useParams();

    const [delivery, setDelivery] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // 1. Goi API chi tiet chuyen giao.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");
        setDelivery(null);

        getInTransitShipmentDetail(
            deliveryId,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (data?.deliveryId == null) {
                    throw new Error(
                        "Invalid shipment data."
                    );
                }

                setDelivery(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                const message = err?.isAxiosError
                    ? inTransitShipmentErrorMessage(err)
                    : err?.message ||
                      "Unable to load delivery trip details.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [deliveryId]);

    // 2. Xu ly danh sach long nhau an toan.
    const transfers = Array.isArray(delivery?.transfers)
        ? delivery.transfers
        : [];

    const packages = Array.isArray(delivery?.packages)
        ? delivery.packages
        : [];

    const history = Array.isArray(delivery?.statusHistory)
        ? [...delivery.statusHistory].sort((a, b) => {
              const timeA = Date.parse(a?.changedAt || "");
              const timeB = Date.parse(b?.changedAt || "");

              if (
                  !Number.isFinite(timeA) ||
                  !Number.isFinite(timeB)
              ) {
                  return 0;
              }

              return timeA - timeB;
          })
        : [];

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Shipment Details"
                    description="Track deliveries, products, packages, and status history."
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
                        Loading shipment details...
                    </div>
                ) : error ? (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                ) : !delivery ? (
                    <EmptyState
                        title="Shipment Not Found"
                        description="No details available to display."
                    />
                ) : (
                    <>
                        {/* Quyen han Business Staff */}
                        <Alert type="info">
                            Business Operations users can only view and track shipments. Delivery status updates are restricted to authorized roles.
                        </Alert>

                        {/* Canh bao qua han */}
                        {delivery.overdue === true && (
                            <div role="alert">
                                <Alert type="danger">
                                    <strong>
                                        Warning: Delivery Overdue!
                                    </strong>{" "}
                                    This shipment has passed its expected delivery time:{" "}
                                    {formatDateTime(
                                        delivery.expectedDeliveryAt
                                    )}. Current status:{" "}
                                    {STATUS_LABELS[
                                        delivery.deliveryStatus
                                    ] || delivery.deliveryStatus}.
                                </Alert>
                            </div>
                        )}

                        {/* Thong tin chung */}
                        <Card>
                            <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <p className="text-sm text-slate-500">
                                        Delivery Trip Code
                                    </p>

                                    <h2 className="text-xl font-bold text-slate-900">
                                        {delivery.deliveryCode || "—"}
                                    </h2>
                                </div>

                                <StatusBadge
                                    status={delivery.deliveryStatus}
                                />
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Manifest Code"
                                    value={
                                        delivery.manifestId != null ? (
                                            <Link
                                                to={`/business/shipment-manifests/${delivery.manifestId}`}
                                                className="text-[#c94810] hover:underline"
                                            >
                                                {delivery.manifestCode || "—"}
                                            </Link>
                                        ) : (
                                            delivery.manifestCode || "—"
                                        )
                                    }
                                />

                                <InfoItem
                                    label="Manifest Status"
                                    value={delivery.manifestStatus}
                                />

                                <InfoItem
                                    label="Trip Created At"
                                    value={formatDateTime(
                                        delivery.createdAt
                                    )}
                                />

                                <InfoItem
                                    label="Last Updated"
                                    value={formatDateTime(
                                        delivery.updatedAt
                                    )}
                                />
                            </div>

                            {delivery.manifestNote && (
                                <p className="mt-4 text-sm text-slate-600">
                                    <strong>Manifest Notes:</strong>{" "}
                                    {delivery.manifestNote}
                                </p>
                            )}
                        </Card>

                        {/* Diem xuat va diem nhan */}
                        <div className="grid gap-4 md:grid-cols-2">
                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Origin
                                </h3>

                                <div className="space-y-4">
                                    <InfoItem
                                        label="Warehouse / Store"
                                        value={delivery.fromWarehouseName}
                                    />

                                    <InfoItem
                                        label="Origin Code"
                                        value={delivery.fromWarehouseCode}
                                    />

                                    <InfoItem
                                        label="Address"
                                        value={delivery.fromWarehouseAddress}
                                    />
                                </div>
                            </Card>

                            <Card>
                                <h3 className="mb-4 text-lg font-bold text-slate-800">
                                    Destination
                                </h3>

                                <div className="space-y-4">
                                    <InfoItem
                                        label="Warehouse / Store"
                                        value={delivery.toWarehouseName}
                                    />

                                    <InfoItem
                                        label="Destination Code"
                                        value={delivery.toWarehouseCode}
                                    />

                                    <InfoItem
                                        label="Address"
                                        value={delivery.toWarehouseAddress}
                                    />
                                </div>
                            </Card>
                        </div>

                        {/* So lieu tong hop */}
                        <div className="grid gap-4 sm:grid-cols-3">
                            {[
                                [
                                    "Total Stock Transfers",
                                    delivery.totalTransfers,
                                ],
                                [
                                    "Total Packages",
                                    delivery.totalPackages,
                                ],
                                [
                                    "Total Product Quantity",
                                    delivery.totalProductQuantity,
                                ],
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

                        {/* Thong tin nguoi phu trach */}
                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Driver & Handover Details
                            </h3>

                            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                <InfoItem
                                    label="Driver"
                                    value={delivery.driverName}
                                />

                                <InfoItem
                                    label="Driver ID"
                                    value={delivery.driverCode}
                                />

                                <InfoItem
                                    label="Handed Over By"
                                    value={delivery.handedOverByName}
                                />

                                <InfoItem
                                    label="Handover User ID"
                                    value={delivery.handedOverByCode}
                                />

                                <InfoItem
                                    label="Expected Pickup"
                                    value={formatDateTime(
                                        delivery.expectedPickupAt
                                    )}
                                />

                                <InfoItem
                                    label="Handover Time"
                                    value={formatDateTime(
                                        delivery.handedOverAt
                                    )}
                                />

                                <InfoItem
                                    label="Departed At"
                                    value={formatDateTime(
                                        delivery.startedAt
                                    )}
                                />

                                <InfoItem
                                    label="Expected Delivery"
                                    value={formatDateTime(
                                        delivery.expectedDeliveryAt
                                    )}
                                />

                                <InfoItem
                                    label="Delivered At"
                                    value={formatDateTime(
                                        delivery.deliveredAt
                                    )}
                                />
                            </div>
                        </Card>

                        {/* Danh sach phieu dieu chuyen */}
                        <section className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Stock Transfers
                            </h3>

                            {transfers.length === 0 ? (
                                <EmptyState
                                    title="No Stock Transfers"
                                />
                            ) : (
                                <>
                                    <Table
                                        columns={transferColumns}
                                        data={transfers}
                                        rowKey="transferId"
                                    />

                                    {transfers.map((transfer, index) => {
                                        const products =
                                            Array.isArray(transfer.products)
                                                ? transfer.products
                                                : [];

                                        return (
                                            <Card
                                                key={
                                                    transfer.transferId ??
                                                    index
                                                }
                                            >
                                                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                                    <h4 className="font-bold text-slate-800">
                                                        Transfer Items{" "}
                                                        {transfer.transferCode}
                                                    </h4>

                                                    <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                        {transfer.transferStatus || "—"}
                                                    </span>
                                                </div>

                                                {products.length > 0 ? (
                                                    <Table
                                                        columns={
                                                            transferProductColumns
                                                        }
                                                        data={products}
                                                        rowKey="productId"
                                                    />
                                                ) : (
                                                    <EmptyState
                                                        title="This transfer contains no products"
                                                    />
                                                )}
                                            </Card>
                                        );
                                    })}
                                </>
                            )}
                        </section>

                        {/* Danh sach kien hang */}
                        <section className="space-y-4">
                            <h3 className="text-lg font-bold text-slate-800">
                                Packages
                            </h3>

                            {packages.length === 0 ? (
                                <EmptyState
                                    title="No Packages"
                                />
                            ) : (
                                packages.map((pkg, index) => {
                                    const products =
                                        Array.isArray(pkg.products)
                                            ? pkg.products
                                            : [];

                                    return (
                                        <Card
                                            key={pkg.packageId ?? index}
                                        >
                                            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                                                <h4 className="text-lg font-bold text-slate-800">
                                                    {pkg.packageCode || "—"}
                                                </h4>

                                                <span className="rounded-lg bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
                                                    {pkg.packageStatus || "—"}
                                                </span>
                                            </div>

                                            <div className="mb-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                                                <InfoItem
                                                    label="Seal Code"
                                                    value={pkg.sealNumber}
                                                />

                                                <InfoItem
                                                    label="Packed At"
                                                    value={formatDateTime(
                                                        pkg.packedAt
                                                    )}
                                                />

                                                <InfoItem
                                                    label="Sealed At"
                                                    value={formatDateTime(
                                                        pkg.sealedAt
                                                    )}
                                                />

                                                <InfoItem
                                                    label="Total Quantity"
                                                    value={formatNumber(
                                                        pkg.totalQuantity
                                                    )}
                                                />
                                            </div>

                                            <h5 className="mb-3 font-semibold text-slate-800">
                                                Package Items
                                            </h5>

                                            {products.length > 0 ? (
                                                <Table
                                                    columns={
                                                        packageProductColumns
                                                    }
                                                    data={products}
                                                    rowKey="productId"
                                                />
                                            ) : (
                                                <EmptyState
                                                    title="This package contains no products"
                                                />
                                            )}
                                        </Card>
                                    );
                                })
                            )}
                        </section>

                        {/* Timeline lich su trang thai */}
                        <Card>
                            <h3 className="mb-4 text-lg font-bold text-slate-800">
                                Shipment Status History
                            </h3>

                            {history.length === 0 ? (
                                <EmptyState
                                    title="No Status History"
                                />
                            ) : (
                                <ol className="space-y-4">
                                    {history.map((event, index) => (
                                        <li
                                            key={`${event.status}-${event.changedAt}-${index}`}
                                            className="relative border-l-2 border-orange-200 py-1 pl-5"
                                        >
                                            <span
                                                aria-hidden="true"
                                                className="absolute -left-[7px] top-2 h-3 w-3 rounded-full bg-[#c94810]"
                                            />

                                            <div className="flex flex-wrap items-center gap-3">
                                                <StatusBadge
                                                    status={event.status}
                                                />

                                                <span className="text-sm font-semibold text-slate-600">
                                                    {formatDateTime(
                                                        event.changedAt
                                                    )}
                                                </span>
                                            </div>

                                            <p className="mt-2 text-sm text-slate-700">
                                                <strong>Performed by:</strong>{" "}
                                                {event.changedByName || "No Information"}
                                            </p>

                                            {event.note && (
                                                <p className="mt-1 text-sm text-slate-600">
                                                    <strong>Notes:</strong>{" "}
                                                    {event.note}
                                                </p>
                                            )}
                                        </li>
                                    ))}
                                </ol>
                            )}

                            <p className="mt-4 text-xs text-slate-500">
                                Only status updates recorded by the system are shown in the history.
                            </p>
                        </Card>
                    </>
                )}
            </div>
        </MainLayout>
    );
}