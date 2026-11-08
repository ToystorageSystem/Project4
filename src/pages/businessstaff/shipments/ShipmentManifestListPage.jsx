import { formatBusinessStatus } from "../../../utils/viStatus";

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import {
    getShipmentManifests,
    shipmentManifestErrorMessage,
} from "../../../api/business/shipmentManifests";

import {
    getInventoryWarehouseOptions,
} from "../../../api/business/inventoryReport";

import MainLayout from "../../../components/layout/MainLayout";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import PageHeader from "../../../components/layout/PageHeader";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import Card from "../../../components/ui/Card";
import EmptyState from "../../../components/ui/EmptyState";
import Input from "../../../components/ui/Input";
import Pagination from "../../../components/ui/Pagination";
import SearchInput from "../../../components/ui/SearchInput";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

const ROOT = "/business/shipment-manifests";
const PAGE_SIZE = 10;

const EMPTY_FILTERS = {
    keyword: "",
    fromWarehouseId: "",
    toWarehouseId: "",
    transportStatus: "",
    fromDate: "",
    toDate: "",
};

const STATUS_LABELS = {
    CREATED: "Created",
    ASSIGNED: "Assigned",
    ACCEPTED: "Driver Accepted",
    REJECTED: "Driver Rejected",
    READY_TO_SHIP: "Ready for Delivery",
    IN_TRANSIT: "In Transit",
    ARRIVED: "Arrived",
    DELIVERED: "Delivered",
    FAILED: "Delivery Failed",
    CANCELLED: "Cancelled",
};

const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    { value: "NO_DELIVERY", label: "No Delivery Trip" },
    ...Object.entries(STATUS_LABELS).map(([value, label]) => ({
        value,
        label,
    })),
];

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString("en-GB");
}

function TransportStatusBadge({ status }) {
    const colorClasses = {
        DELIVERED: "bg-green-100 text-green-700",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
        ARRIVED: "bg-teal-100 text-teal-700",
    };

    const color =
        colorClasses[status] || "bg-amber-100 text-amber-800";

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${color}`}
        >
            {STATUS_LABELS[status] || formatBusinessStatus(status)}
        </span>
    );
}

export default function ShipmentManifestListPage() {
    const [draftFilters, setDraftFilters] = useState(EMPTY_FILTERS);
    const [filters, setFilters] = useState(EMPTY_FILTERS);

    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);

    const [result, setResult] = useState({
        items: [],
        totalElements: 0,
        totalPages: 0,
    });

    const [warehouses, setWarehouses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [filterError, setFilterError] = useState("");
    const [optionsError, setOptionsError] = useState("");

    // Lấy danh sách kho/cửa hàng từ API đã có.
    useEffect(() => {
        const controller = new AbortController();

        getInventoryWarehouseOptions(controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(data)) {
                    throw new Error("Invalid warehouse data.");
                }

                setWarehouses(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                setOptionsError(
                    err?.isAxiosError
                        ? shipmentManifestErrorMessage(err)
                        : err.message
                );
            });

        return () => controller.abort();
    }, []);

    // Lấy danh sách bảng kê từ Backend.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        getShipmentManifests(
            {
                ...filters,
                page,
                size: PAGE_SIZE,
            },
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(data?.items)) {
                    throw new Error("Invalid shipment manifest data.");
                }

                setResult(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                setResult({
                    items: [],
                    totalElements: 0,
                    totalPages: 0,
                });

                setError(
                    err?.isAxiosError
                        ? shipmentManifestErrorMessage(err)
                        : err.message || "Unable to load shipment manifests."
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters, page, reload]);

    const updateDraft = (name, value) => {
        setDraftFilters((current) => ({
            ...current,
            [name]: value,
        }));
    };

    const applyFilters = (event) => {
        event.preventDefault();

        if (
            draftFilters.fromDate &&
            draftFilters.toDate &&
            draftFilters.fromDate > draftFilters.toDate
        ) {
            setFilterError(
                "Start date cannot be after end date."
            );
            return;
        }

        setFilterError("");
        setPage(0);
        setFilters({
            ...draftFilters,
            keyword: draftFilters.keyword.trim(),
        });
    };

    const resetFilters = () => {
        setDraftFilters({ ...EMPTY_FILTERS });
        setFilters({ ...EMPTY_FILTERS });
        setPage(0);
        setFilterError("");
        setReload((value) => value + 1);
    };

    const warehouseOptions = [
        { value: "", label: "All Warehouses / Stores" },
        ...warehouses.map((warehouse) => ({
            value: String(warehouse.id),
            label: `${warehouse.name} (${warehouse.code})`,
        })),
    ];

    const columns = [
        {
            key: "manifestCode",
            label: "Manifest Code",
            render: (value, manifest) => (
                <Link
                    to={`${ROOT}/${manifest.manifestId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    {value || "—"}
                </Link>
            ),
        },
        {
            key: "fromWarehouseName",
            label: "Origin",
            render: (value, manifest) => (
                <div>
                    <p className="font-medium">{value || "—"}</p>
                    <p className="text-xs text-slate-500">
                        {manifest.fromWarehouseCode}
                    </p>
                </div>
            ),
        },
        {
            key: "toWarehouseName",
            label: "Destination",
            render: (value, manifest) => (
                <div>
                    <p className="font-medium">{value || "—"}</p>
                    <p className="text-xs text-slate-500">
                        {manifest.toWarehouseCode}
                    </p>
                </div>
            ),
        },
        {
            key: "createdAt",
            label: "Created At",
            render: formatDate,
        },
        {
            key: "createdByName",
            label: "Created By",
            render: (value) => value || "—",
        },
        {
            key: "totalTransfers",
            label: "Transfer Count",
            render: (value) => value ?? 0,
        },
        {
            key: "totalPackages",
            label: "Package Count",
            render: (value) => value ?? 0,
        },
        {
            key: "transportStatus",
            label: "Shipment",
            render: (value) => (
                <TransportStatusBadge status={value || "NO_DELIVERY"} />
            ),
        },
        {
            key: "manifestId",
            label: "Actions",
            render: (_value, manifest) => (
                <Link
                    to={`${ROOT}/${manifest.manifestId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    View Details
                </Link>
            ),
        },
    ];

    const items = result.items || [];
    const totalPages = Math.max(1, result.totalPages || 0);

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Shipment Manifests"
                    description="Track shipment manifests, stock transfers, packages, and shipment status."
                />

                <Alert type="info">
                    Shipment manifests are generated after packing is completed. Business Operations users have read-only access and cannot modify or delete manifest data.
                </Alert>

                {optionsError && (
                    <Alert type="warning">{optionsError}</Alert>
                )}

                {filterError && (
                    <Alert type="danger">{filterError}</Alert>
                )}

                {error && (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                )}

                <Card>
                    <form
                        onSubmit={applyFilters}
                        className="grid grid-cols-1 items-end gap-3 md:grid-cols-2 xl:grid-cols-3"
                    >
                        <SearchInput
                            label="Manifest Code / Stock Transfer Code"
                            placeholder="Enter manifest code or ST code..."
                            value={draftFilters.keyword}
                            onChange={(event) =>
                                updateDraft("keyword", event.target.value)
                            }
                        />

                        <Select
                            label="Origin"
                            value={draftFilters.fromWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft("fromWarehouseId", event.target.value)
                            }
                        />

                        <Select
                            label="Destination"
                            value={draftFilters.toWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft("toWarehouseId", event.target.value)
                            }
                        />

                        <Select
                            label="Shipment Status"
                            value={draftFilters.transportStatus}
                            options={STATUS_OPTIONS}
                            onChange={(event) =>
                                updateDraft("transportStatus", event.target.value)
                            }
                        />

                        <Input
                            label="From Date"
                            type="date"
                            value={draftFilters.fromDate}
                            onChange={(event) =>
                                updateDraft("fromDate", event.target.value)
                            }
                        />

                        <Input
                            label="To Date"
                            type="date"
                            value={draftFilters.toDate}
                            onChange={(event) =>
                                updateDraft("toDate", event.target.value)
                            }
                        />

                        <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-3">
                            <Button type="submit">
                                Search / Filters
                            </Button>

                            <Button
                                type="button"
                                variant="outline"
                                onClick={resetFilters}
                            >
                                Clear Filters
                            </Button>

                            <Button
                                type="button"
                                variant="ghost"
                                onClick={() =>
                                    setReload((value) => value + 1)
                                }
                            >
                                Refresh
                            </Button>
                        </div>
                    </form>

                    <p className="mt-4 text-sm text-slate-500">
                        {loading
                            ? "Loading..."
                            : `Found ${result.totalElements ?? 0} manifests.`}
                    </p>
                </Card>

                {loading ? (
                    <div
                        className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600"
                        role="status"
                    >
                        <Spinner />
                        Loading shipment manifests...
                    </div>
                ) : error ? null : items.length === 0 ? (
                    <EmptyState
                        title="Manifest Not Found"
                        description="No shipment manifests match the current filters."
                    />
                ) : (
                    <div className="space-y-4">
                        <Table
                            columns={columns}
                            data={items}
                            rowKey="manifestId"
                        />

                        <Pagination
                            page={page + 1}
                            totalPages={totalPages}
                            onChange={(nextPage) =>
                                setPage(nextPage - 1)
                            }
                        />
                    </div>
                )}
            </div>
        </MainLayout>
    );
}
