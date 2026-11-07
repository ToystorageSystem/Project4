import { formatBusinessStatus } from "../../../utils/viStatus";

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import {
    getMyStockTransfers,
    stockTransferErrorMessage,
} from "../../../api/business/stockTransfers";

import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
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

const ROOT = "/business/stock-transfers";
const PAGE_SIZE = 10;

// Chuyen trang thai Backend thanh ten de hieu tren giao dien.
const STATUS_LABELS = {
    DRAFT: "Nháp",
    PENDING_SOURCE_CONFIRMATION: "Chờ kho xuất xác nhận",
    CREATED: "Đã gửi, chờ kho xử lý",
    PICKING: "Đang lấy hàng",
    PACKING: "Đang đóng kiện",
    PACKED: "Đã đóng kiện",
    SHIPPED: "Đang vận chuyển",
    RECEIVING: "Đang kiểm nhận",
    RECEIVED: "Đã nhận",
    COMPLETED: "Đã hoàn thành",
    REJECTED: "Bị từ chối",
    CANCELLED: "Đã hủy",
};

const STATUS_OPTIONS = [
    { value: "", label: "Tất cả trạng thái" },
    ...Object.entries(STATUS_LABELS).map(([value, label]) => ({
        value,
        label,
    })),
];

// Hien thi ngay theo dinh dang Viet Nam.
// LocalDate khong chuyen qua Date JS de tranh lech ngay.
function formatDate(value) {
    if (!value) return "—";

    const text = String(value);

    if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
        const [year, month, day] = text.split("-");
        return `${day}/${month}/${year}`;
    }

    const date = new Date(text);

    return Number.isNaN(date.getTime())
        ? text
        : date.toLocaleString("vi-VN");
}

// Hien thi trang thai phieu bang mau sac.
function StatusBadge({ status }) {
    const tones = {
        DRAFT: "bg-slate-100 text-slate-700",
        CREATED: "bg-blue-100 text-blue-700",
        PENDING_SOURCE_CONFIRMATION: "bg-amber-100 text-amber-800",
        PICKING: "bg-blue-100 text-blue-700",
        PACKING: "bg-blue-100 text-blue-700",
        PACKED: "bg-blue-100 text-blue-700",
        SHIPPED: "bg-violet-100 text-violet-700",
        RECEIVING: "bg-amber-100 text-amber-800",
        RECEIVED: "bg-green-100 text-green-700",
        COMPLETED: "bg-green-100 text-green-700",
        REJECTED: "bg-red-100 text-red-700",
        CANCELLED: "bg-red-100 text-red-700",
    };

    const className =
        tones[status] || "bg-slate-100 text-slate-700";

    return (
        <span
            className={`inline-block whitespace-nowrap rounded-full px-3 py-1 text-xs font-semibold ${className}`}
        >
            {STATUS_LABELS[status] || (status ? formatBusinessStatus(status) : "—")}
        </span>
    );
}

export default function StockTransferListPage() {
    const navigate = useNavigate();

    const [keywordDraft, setKeywordDraft] = useState("");
    const [statusDraft, setStatusDraft] = useState("");

    const [filters, setFilters] = useState({
        keyword: "",
        status: "",
    });

    const [page, setPage] = useState(0);
    const [refresh, setRefresh] = useState(0);

    const [result, setResult] = useState({
        content: [],
        totalPages: 0,
        totalElements: 0,
    });

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Lay danh sach phieu tu Backend.
    // Backend chi tra ve phieu cua nguoi dang nhap.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        getMyStockTransfers(
            {
                ...filters,
                page,
                size: PAGE_SIZE,
            },
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;

                if (!Array.isArray(data?.content)) {
                    throw new Error(
                        "Dữ liệu danh sách phiếu không hợp lệ."
                    );
                }

                setResult(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;

                setResult({
                    content: [],
                    totalPages: 0,
                    totalElements: 0,
                });

                setError(
                    stockTransferErrorMessage(
                        requestError,
                        "Không tải được danh sách phiếu điều chuyển."
                    )
                );
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters, page, refresh]);

    // Ap dung bo loc khi nguoi dung nhan Tim kiem.
    function applyFilters(event) {
        event.preventDefault();

        setPage(0);

        setFilters({
            keyword: keywordDraft.trim(),
            status: statusDraft,
        });
    }

    // Xoa bo loc va tai lai danh sach.
    function resetFilters() {
        setKeywordDraft("");
        setStatusDraft("");

        setFilters({
            keyword: "",
            status: "",
        });

        setPage(0);
        setRefresh((value) => value + 1);
    }

    // Cau hinh cac cot trong bang.
    const columns = [
        {
            key: "transferCode",
            label: "Mã chứng từ",
            render: (value, item) => (
                <Link
                    to={`${ROOT}/${item.id}`}
                    className="font-semibold text-[#c94810] hover:underline"
                >
                    {value || `#${item.id}`}
                </Link>
            ),
        },
        {
            key: "fromWarehouseName",
            label: "Điểm xuất phát",
            render: (value) => value || "—",
        },
        {
            key: "toWarehouseName",
            label: "Điểm đến",
            render: (value) => value || "—",
        },
        {
            key: "status",
            label: "Trạng thái",
            render: (value) => (
                <StatusBadge status={value} />
            ),
        },
        {
            key: "expectedShipmentDate",
            label: "Dự kiến xuất kho",
            render: formatDate,
        },
        {
            key: "expectedReceiptDate",
            label: "Dự kiến nhận hàng",
            render: formatDate,
        },
        {
            key: "createdAt",
            label: "Ngày tạo",
            render: formatDate,
        },
        {
            key: "id",
            label: "Thao tác",
            render: (value) => (
                <Link
                    to={`${ROOT}/${value}`}
                    className="whitespace-nowrap font-semibold text-[#c94810] hover:underline"
                >
                    Xem chi tiết
                </Link>
            ),
        },
    ];

    return (
        <MainLayout
            title="Business Operations"
            items={businessStaffMenuItems}
        >
            <div className="space-y-6">
                <PageHeader
                    title="Phiếu điều chuyển của tôi"
                    description="Xem lại các phiếu điều chuyển đã tạo và theo dõi trạng thái xử lý từ hệ thống."
                    actions={
                        <Button
                            type="button"
                            onClick={() =>
                                navigate(`${ROOT}/new`)
                            }
                        >
                            + Tạo phiếu điều chuyển
                        </Button>
                    }
                />

                <Card className="space-y-4">
                    <form
                        onSubmit={applyFilters}
                        className="grid gap-3 md:grid-cols-[2fr_1fr]"
                    >
                        <SearchInput
                            label="Tìm theo mã phiếu"
                            placeholder="Nhập mã ST-..."
                            maxLength={100}
                            value={keywordDraft}
                            onChange={(event) =>
                                setKeywordDraft(event.target.value)
                            }
                        />

                        <Select
                            label="Trạng thái"
                            value={statusDraft}
                            options={STATUS_OPTIONS}
                            onChange={(event) =>
                                setStatusDraft(event.target.value)
                            }
                        />

                        <div className="flex flex-wrap items-center gap-2 md:col-span-2">
                            <Button type="submit">
                                Tìm kiếm / Bộ lọc
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
                                    setRefresh((value) => value + 1)
                                }
                            >
                                Làm mới
                            </Button>
                        </div>
                    </form>

                    <p className="text-sm text-slate-500">
                        {loading
                            ? "Đang tải danh sách..."
                            : `Tìm thấy ${result.totalElements ?? 0} phiếu của tài khoản hiện tại.`}
                    </p>
                </Card>

                {error && (
                    <div role="alert">
                        <Alert type="danger">{error}</Alert>
                    </div>
                )}

                {loading ? (
                    <Card className="flex items-center gap-3">
                        <Spinner />
                        Đang tải phiếu điều chuyển...
                    </Card>
                ) : result.content.length > 0 ? (
                    <>
                        <Table
                            columns={columns}
                            data={result.content}
                        />

                        <Pagination
                            page={page + 1}
                            totalPages={Math.max(
                                1,
                                Number(result.totalPages) || 1
                            )}
                            onChange={(nextPage) =>
                                setPage(nextPage - 1)
                            }
                        />
                    </>
                ) : (
                    !error && (
                        <EmptyState
                            title="Không tìm thấy phiếu điều chuyển"
                            description="Chưa có phiếu do tài khoản này tạo hoặc bộ lọc chưa khớp."
                        />
                    )
                )}
            </div>
        </MainLayout>
    );
}
