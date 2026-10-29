
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import {
    getInTransitShipments,
    inTransitShipmentErrorMessage,
} from "../../../api/business/inTransitShipments";

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
import Pagination from "../../../components/ui/Pagination";
import SearchInput from "../../../components/ui/SearchInput";
import Select from "../../../components/ui/Select";
import Spinner from "../../../components/ui/Spinner";
import Table from "../../../components/ui/Table";

// Issue #13 - Business Staff xem hàng đang vận chuyển.
// Chỉ xem dữ liệu, không cập nhật trạng thái vận chuyển.

const ROOT = "/business-staff/in-transit-shipments";
const PAGE_SIZE = 10;

const EMPTY_FILTERS = {
    keyword: "",
    fromWarehouseId: "",
    toWarehouseId: "",
    status: "",
};

const STATUS_LABELS = {
    CREATED: "Đã tạo",
    ASSIGNED: "Đã phân công",
    READY_TO_SHIP: "Sẵn sàng xuất hàng",
    IN_TRANSIT: "Đang vận chuyển",
    ARRIVED: "Đã đến điểm nhận",
    DELIVERED: "Đã giao thành công",
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

// Chuyển ngày giờ Backend thành định dạng dễ đọc.
function formatDateTime(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString("vi-VN", {
        hour12: false,
    });
}

// Hiển thị trạng thái vận chuyển.
function DeliveryStatusBadge({ status }) {
    const colorClasses = {
        CREATED: "bg-slate-100 text-slate-700",
        ASSIGNED: "bg-purple-100 text-purple-700",
        READY_TO_SHIP: "bg-amber-100 text-amber-800",
        IN_TRANSIT: "bg-blue-100 text-blue-700",
        ARRIVED: "bg-teal-100 text-teal-700",
        DELIVERED: "bg-green-100 text-green-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
    };

    const color =
        colorClasses[status] || "bg-slate-100 text-slate-700";

    return (
        <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${color}`}
        >
            {STATUS_LABELS[status] || status || "Chưa xác định"}
        </span>
    );
}

// Hiển thị các mã phiếu, kiện hàng.
function CodeList({ label, values }) {
    const codes = Array.isArray(values)
        ? values.filter(Boolean)
        : [];

    return (
        <p className="text-xs text-slate-600">
            <span className="font-semibold">{label}: </span>
            {codes.length > 0 ? codes.join(", ") : "—"}
        </p>
    );
}

export default function InTransitShipmentListPage() {
    // Bộ lọc người dùng đang nhập.
    const [draftFilters, setDraftFilters] = useState({
        ...EMPTY_FILTERS,
    });

    // Bộ lọc thực sự gửi lên Backend.
    const [filters, setFilters] = useState({
        ...EMPTY_FILTERS,
    });

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
    const [optionsError, setOptionsError] = useState("");

    // 1. Lấy danh sách kho / cửa hàng.
    useEffect(() => {
        const controller = new AbortController();

        getInventoryWarehouseOptions(controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(data)) {
                    throw new Error(
                        "Dữ liệu kho/cửa hàng không hợp lệ."
                    );
                }

                setWarehouses(data);
            })
            .catch((err) => {
                if (controller.signal.aborted) return;

                const message = err?.isAxiosError
                    ? inTransitShipmentErrorMessage(err)
                    : err?.message ||
                      "Không tải được danh sách kho/cửa hàng.";

                setOptionsError(message);
            });

        return () => controller.abort();
    }, []);

    // 2. Lấy danh sách chuyến vận chuyển.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        getInTransitShipments(
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
                    throw new Error(
                        "Dữ liệu chuyến vận chuyển không hợp lệ."
                    );
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

                const message = err?.isAxiosError
                    ? inTransitShipmentErrorMessage(err)
                    : err?.message ||
                      "Không tải được danh sách vận chuyển.";

                setError(message);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters, page, reload]);

    // 3. Cập nhật bộ lọc đang nhập.
    const updateDraft = (name, value) => {
        setDraftFilters((current) => ({
            ...current,
            [name]: value,
        }));
    };

    // 4. Áp dụng bộ lọc.
    const applyFilters = (event) => {
        event.preventDefault();

        setPage(0);

        setFilters({
            ...draftFilters,
            keyword: draftFilters.keyword.trim(),
        });
    };

    // 5. Xóa bộ lọc.
    const resetFilters = () => {
        setDraftFilters({ ...EMPTY_FILTERS });
        setFilters({ ...EMPTY_FILTERS });
        setPage(0);
        setReload((value) => value + 1);
    };

    // 6. Danh sách lựa chọn kho / cửa hàng.
    const warehouseOptions = [
        {
            value: "",
            label: "Tất cả kho/cửa hàng",
        },
        ...warehouses
            .filter((warehouse) => warehouse?.id != null)
            .map((warehouse) => ({
                value: String(warehouse.id),
                label: `${warehouse.name} (${warehouse.code})`,
            })),
    ];

    // 7. Các cột của bảng danh sách.
    const columns = [
        {
            key: "deliveryCode",
            label: "Mã chuyến giao",
            render: (value, delivery) => (
                <Link
                    to={`${ROOT}/${delivery.deliveryId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    {value || "—"}
                </Link>
            ),
        },
        {
            key: "manifestCode",
            label: "Bảng kê / Phiếu / Kiện",
            render: (value, delivery) => (
                <div className="space-y-1">
                    <p className="font-semibold text-slate-800">
                        {value || "—"}
                    </p>

                    <CodeList
                        label="Phiếu"
                        values={delivery.transferCodes}
                    />

                    <CodeList
                        label="Kiện"
                        values={delivery.packageCodes}
                    />
                </div>
            ),
        },
        {
            key: "fromWarehouseName",
            label: "Điểm xuất",
            render: (value, delivery) => (
                <div>
                    <p className="font-medium">
                        {value || "—"}
                    </p>

                    <p className="text-xs text-slate-500">
                        {delivery.fromWarehouseCode || ""}
                    </p>
                </div>
            ),
        },
        {
            key: "toWarehouseName",
            label: "Điểm nhận",
            render: (value, delivery) => (
                <div>
                    <p className="font-medium">
                        {value || "—"}
                    </p>

                    <p className="text-xs text-slate-500">
                        {delivery.toWarehouseCode || ""}
                    </p>
                </div>
            ),
        },
        {
            key: "startedAt",
            label: "Bắt đầu vận chuyển",
            render: (value) => formatDateTime(value),
        },
        {
            key: "expectedDeliveryAt",
            label: "Dự kiến nhận",
            render: (value) => formatDateTime(value),
        },
        {
            key: "deliveryStatus",
            label: "Trạng thái",
            render: (value) => (
                <DeliveryStatusBadge status={value} />
            ),
        },
        {
            key: "totalPackages",
            label: "Thông tin hàng",
            render: (_value, delivery) => (
                <div className="space-y-1 text-sm">
                    <p>
                        Phiếu: {delivery.totalTransfers ?? 0}
                    </p>

                    <p>
                        Kiện: {delivery.totalPackages ?? 0}
                    </p>

                    <p className="font-semibold">
                        Số lượng:{" "}
                        {delivery.totalProductQuantity ?? 0}
                    </p>
                </div>
            ),
        },
        {
            key: "overdue",
            label: "Cảnh báo",
            render: (value) =>
                value === true ? (
                    <span
                        className="inline-block rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700"
                        title="Đã quá thời gian dự kiến nhận hàng"
                    >
                        Quá hạn giao hàng
                    </span>
                ) : (
                    <span className="text-sm text-slate-500">
                        —
                    </span>
                ),
        },
        {
            key: "deliveryId",
            label: "Thao tác",
            render: (_value, delivery) => (
                <Link
                    to={`${ROOT}/${delivery.deliveryId}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    Xem chi tiết
                </Link>
            ),
        },
    ];

    const items = result.items || [];
    const totalPages = Math.max(
        1,
        result.totalPages || 0
    );

    return (
        <MainLayout
            title="Business Staff"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Xem hàng đang vận chuyển"
                    description="Theo dõi chuyến giao, phiếu điều chuyển, kiện hàng, thời gian dự kiến nhận và trạng thái vận chuyển."
                />

                <Alert type="info">
                    Trang này hiển thị các chuyến vận chuyển
                    trong hệ thống, bao gồm cả chuyến đã hoàn
                    thành. Chọn trạng thái "Đang vận chuyển"
                    để chỉ xem các chuyến đang trên đường.
                    Business Staff chỉ được xem dữ liệu.
                </Alert>

                {optionsError && (
                    <Alert type="warning">
                        {optionsError}
                    </Alert>
                )}

                {error && (
                    <div role="alert">
                        <Alert type="danger">
                            {error}
                        </Alert>
                    </div>
                )}

                {/* Bộ lọc danh sách */}
                <Card>
                    <form
                        onSubmit={applyFilters}
                        className="grid grid-cols-1 items-end gap-3 md:grid-cols-2 xl:grid-cols-3"
                    >
                        <SearchInput
                            label="Mã chuyến / bảng kê / phiếu / kiện"
                            placeholder="Nhập SHIP, MAN, ST hoặc PKG..."
                            value={draftFilters.keyword}
                            onChange={(event) =>
                                updateDraft(
                                    "keyword",
                                    event.target.value
                                )
                            }
                        />

                        <Select
                            label="Điểm xuất"
                            value={draftFilters.fromWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft(
                                    "fromWarehouseId",
                                    event.target.value
                                )
                            }
                        />

                        <Select
                            label="Điểm nhận"
                            value={draftFilters.toWarehouseId}
                            options={warehouseOptions}
                            onChange={(event) =>
                                updateDraft(
                                    "toWarehouseId",
                                    event.target.value
                                )
                            }
                        />

                        <Select
                            label="Trạng thái vận chuyển"
                            value={draftFilters.status}
                            options={STATUS_OPTIONS}
                            onChange={(event) =>
                                updateDraft(
                                    "status",
                                    event.target.value
                                )
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
                            : `Tìm thấy ${
                                  result.totalElements ?? 0
                              } chuyến vận chuyển.`}
                    </p>
                </Card>

                {/* Danh sách chuyến giao */}
                {loading ? (
                    <div
                        className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600"
                        role="status"
                    >
                        <Spinner />
                        Đang tải danh sách vận chuyển...
                    </div>
                ) : error ? null : items.length === 0 ? (
                    <EmptyState
                        title="Không tìm thấy chuyến vận chuyển"
                        description="Chưa có chuyến giao phù hợp với bộ lọc hiện tại."
                    />
                ) : (
                    <div className="space-y-4">
                        <Table
                            columns={columns}
                            data={items}
                            rowKey="deliveryId"
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
