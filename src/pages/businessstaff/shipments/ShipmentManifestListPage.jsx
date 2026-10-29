
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

const ROOT = "/business-staff/shipment-manifests";
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
    CREATED: "Mới tạo",
    ASSIGNED: "Đã phân công",
    ACCEPTED: "Tài xế đã nhận",
    REJECTED: "Tài xế từ chối",
    READY_TO_SHIP: "Sẵn sàng giao",
    IN_TRANSIT: "Đang vận chuyển",
    ARRIVED: "Đã đến nơi",
    DELIVERED: "Đã giao",
    FAILED: "Giao thất bại",
    CANCELLED: "Đã hủy",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả trạng thái" },
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
        : date.toLocaleString("vi-VN");
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
            {STATUS_LABELS[status] || status || "Chưa có chuyến giao"}
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
                    throw new Error("Dữ liệu kho không hợp lệ.");
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
                    throw new Error("Dữ liệu bảng kê không hợp lệ.");
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
                        : err.message || "Không tải được danh sách bảng kê."
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
                "Ngày bắt đầu không được sau ngày kết thúc."
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
        { value: "", label: "Tất cả kho/cửa hàng" },
        ...warehouses.map((warehouse) => ({
            value: String(warehouse.id),
            label: `${warehouse.name} (${warehouse.code})`,
        })),
    ];

    const columns = [
        {
            key: "manifestCode",
            label: "Mã bảng kê",
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
            label: "Điểm xuất",
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
            label: "Điểm nhận",
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
            label: "Thời gian tạo",
            render: formatDate,
        },
        {
            key: "createdByName",
            label: "Người tạo",
            render: (value) => value || "—",
        },
        {
            key: "totalTransfers",
            label: "Số phiếu",
            render: (value) => value ?? 0,
        },
        {
            key: "totalPackages",
            label: "Số kiện",
            render: (value) => value ?? 0,
        },
        {
            key: "transportStatus",
            label: "Vận chuyển",
            render: (value) => (
                <TransportStatusBadge status={value} />
            ),
        },
        {
            key: "manifestId",
            label: "Thao tác",
            render: (_value, manifest) => (
                <Link
                    to={`${ROOT}/${manifest.manifestId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    Xem chi tiết
                </Link>
            ),
        },
    ];

    const items = result.items || [];
    const totalPages = Math.max(1, result.totalPages || 0);

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Danh sách bảng kê đi hàng"
                    description="Theo dõi bảng kê, phiếu điều chuyển, kiện hàng và trạng thái vận chuyển."
                />

                <Alert type="info">
                    Bảng kê được hệ thống tạo sau khi hoàn tất
                    đóng kiện. Business Staff chỉ được xem và theo dõi,
                    không được chỉnh sửa hoặc xóa dữ liệu.
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
                            label="Mã bảng kê / mã phiếu điều chuyển"
                            placeholder="Nhập mã bảng kê hoặc mã ST..."
                            value={draftFilters.keyword}
                            onChange={(event) =>
                                updateDraft("keyword", event.target.value)
                            }
                        />

                        <Select
                            label="Điểm xuất"
                            value={draftFilters.fromWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft("fromWarehouseId", event.target.value)
                            }
                        />

                        <Select
                            label="Điểm nhận"
                            value={draftFilters.toWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft("toWarehouseId", event.target.value)
                            }
                        />

                        <Select
                            label="Trạng thái vận chuyển"
                            value={draftFilters.transportStatus}
                            options={STATUS_OPTIONS}
                            onChange={(event) =>
                                updateDraft("transportStatus", event.target.value)
                            }
                        />

                        <Input
                            label="Từ ngày"
                            type="date"
                            value={draftFilters.fromDate}
                            onChange={(event) =>
                                updateDraft("fromDate", event.target.value)
                            }
                        />

                        <Input
                            label="Đến ngày"
                            type="date"
                            value={draftFilters.toDate}
                            onChange={(event) =>
                                updateDraft("toDate", event.target.value)
                            }
                        />

                        <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-3">
                            <Button type="submit">
                                Tìm kiếm / Lọc
                            </Button>

                            <Button
                                type="button"
                                variant="outline"
                                onClick={resetFilters}
                            >
                                Xóa bộ lọc
                            </Button>

                            <Button
                                type="button"
                                variant="ghost"
                                onClick={() =>
                                    setReload((value) => value + 1)
                                }
                            >
                                Làm mới
                            </Button>
                        </div>
                    </form>

                    <p className="mt-4 text-sm text-slate-500">
                        {loading
                            ? "Đang tải..."
                            : `Tìm thấy ${result.totalElements ?? 0} bảng kê đi hàng.`}
                    </p>
                </Card>

                {loading ? (
                    <div
                        className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600"
                        role="status"
                    >
                        <Spinner />
                        Đang tải danh sách bảng kê...
                    </div>
                ) : error ? null : items.length === 0 ? (
                    <EmptyState
                        title="Không tìm thấy bảng kê"
                        description="Chưa có bảng kê phù hợp với bộ lọc hiện tại."
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
