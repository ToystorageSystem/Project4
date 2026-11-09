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
    DRAFT: "Draft",
    PENDING_SOURCE_CONFIRMATION: "Awaiting Source Warehouse Confirmation",
    CREATED: "Submitted - Awaiting Warehouse Processing",
    PICKING: "Picking",
    PACKING: "Packing",
    PACKED: "Packed",
    SHIPPED: "In Transit",
    RECEIVING: "Inspection In Progress",
    RECEIVED: "Received",
    COMPLETED: "Completed",
    REJECTED: "Rejected",
    CANCELLED: "Cancelled",
};

const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
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
        : date.toLocaleString("en-GB");
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
                        "Invalid transfer list data."
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
                        "Unable to load stock transfers."
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
            label: "Document No.",
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
            label: "Origin",
            render: (value) => value || "—",
        },
        {
            key: "toWarehouseName",
            label: "Destination",
            render: (value) => value || "—",
        },
        {
            key: "status",
            label: "Status",
            render: (value) => (
                <StatusBadge status={value} />
            ),
        },
        {
            key: "expectedShipmentDate",
            label: "Expected Dispatch",
            render: formatDate,
        },
        {
            key: "expectedReceiptDate",
            label: "Expected Receipt Date",
            render: formatDate,
        },
        {
            key: "createdAt",
            label: "Created At",
            render: formatDate,
        },
        {
            key: "id",
            label: "Actions",
            render: (value) => (
                <Link
                    to={`${ROOT}/${value}`}
                    className="whitespace-nowrap font-semibold text-[#c94810] hover:underline"
                >
                    View Details
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
                    title="My Stock Transfers"
                    description="Review created stock transfers and track their processing status."
                    actions={
                        <Button
                            type="button"
                            onClick={() =>
                                navigate(`${ROOT}/new`)
                            }
                        >
                            + Create Stock Transfer
                        </Button>
                    }
                />

                <Card className="space-y-4">
                    <form
                        onSubmit={applyFilters}
                        className="grid gap-3 md:grid-cols-[2fr_1fr]"
                    >
                        <SearchInput
                            label="Search by Transfer Code"
                            placeholder="Enter ST code..."
                            maxLength={100}
                            value={keywordDraft}
                            onChange={(event) =>
                                setKeywordDraft(event.target.value)
                            }
                        />

                        <Select
                            label="Status"
                            value={statusDraft}
                            options={STATUS_OPTIONS}
                            onChange={(event) =>
                                setStatusDraft(event.target.value)
                            }
                        />

                        <div className="flex flex-wrap items-center gap-2 md:col-span-2">
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
                                    setRefresh((value) => value + 1)
                                }
                            >
                                Refresh
                            </Button>
                        </div>
                    </form>

                    <p className="text-sm text-slate-500">
                        {loading
                            ? "Loading list..."
                            : `Found ${result.totalElements ?? 0} transfers created by this account.`}
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
                        Loading stock transfers...
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
                            title="No Stock Transfers Found"
                            description="No transfers created by this account, or the filters did not match."
                        />
                    )
                )}
            </div>
        </MainLayout>
    );
}

