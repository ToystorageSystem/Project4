import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
    getPurchaseOrderSuppliers,
    listPurchaseOrders,
    purchaseOrderErrorMessage,
} from "../../../api/business/purchaseOrders";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
import MainLayout from "../../../components/layout/MainLayout";
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
import {
    formatExpectedDeliveryDate,
    formatPurchaseOrderDate,
    formatPurchaseOrderMoney,
    PURCHASE_ORDER_STATUS_OPTIONS,
    PurchaseOrderStatusBadge,
} from "./purchaseOrderFormat";

const ROOT = "/business-staff/purchase-orders";
const PAGE_SIZE = 10;
const EMPTY_FILTERS = {
    keyword: "",
    status: "",
    supplierId: "",
    createdFrom: "",
    createdTo: "",
};

export default function PurchaseOrderListPage() {
    const navigate = useNavigate();
    const [draft, setDraft] = useState(EMPTY_FILTERS);
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [suppliers, setSuppliers] = useState([]);
    const [supplierWarning, setSupplierWarning] = useState("");
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [filterError, setFilterError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [page, setPage] = useState(1);

    useEffect(() => {
        const controller = new AbortController();
        getPurchaseOrderSuppliers(controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) throw new Error("Invalid suppliers options response");
                setSuppliers(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setSupplierWarning(purchaseOrderErrorMessage(requestError, "Không tải được danh sách nhà cung cấp để lọc."));
                }
            });
        return () => controller.abort();
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        listPurchaseOrders(filters, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) throw new Error("Invalid purchase order list response");
                setOrders(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                setOrders([]);
                setError(purchaseOrderErrorMessage(requestError));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [filters, refresh]);

    const applyFilters = (event) => {
        event.preventDefault();
        if (draft.createdFrom && draft.createdTo && draft.createdFrom > draft.createdTo) {
            setFilterError("Ngày bắt đầu không được sau ngày kết thúc.");
            return;
        }
        setFilterError("");
        setPage(1);
        setFilters({ ...draft, keyword: draft.keyword.trim() });
    };

    const resetFilters = () => {
        setDraft(EMPTY_FILTERS);
        setFilters(EMPTY_FILTERS);
        setFilterError("");
        setPage(1);
        setRefresh((value) => value + 1);
    };

    const columns = [
        {
            key: "orderCode",
            label: "Mã đơn mua hàng",
            render: (value, order) => (
                <div className="space-y-1">
                    <Link to={`${ROOT}/${order.id}`} className="font-semibold text-[#c94810] hover:underline">
                        {value || "—"}
                    </Link>
                    {String(value || "").startsWith("PO-AUTO-") && (
                        <p className="text-xs text-slate-500">Đơn tự tạo từ phiếu nhập</p>
                    )}
                </div>
            ),
        },
        { key: "supplierName", label: "Nhà cung cấp", render: (value) => value || "—" },
        { key: "warehouseName", label: "Kho nhận", render: (value) => value || "—" },
        { key: "status", label: "Trạng thái", render: (value) => <PurchaseOrderStatusBadge status={value} /> },
        { key: "totalProducts", label: "Số SP", render: (value) => value ?? "—" },
        { key: "totalOrderedQuantity", label: "SL đặt", render: (value) => value ?? "—" },
        { key: "totalAmount", label: "Tổng tiền", render: (value) => <span className="whitespace-nowrap font-semibold">{formatPurchaseOrderMoney(value)}</span> },
        { key: "expectedDeliveryDate", label: "Dự kiến giao", render: formatExpectedDeliveryDate },
        { key: "createdAt", label: "Ngày tạo", render: formatPurchaseOrderDate },
        { key: "id", label: "Thao tác", render: (_value, order) => <Link className="font-semibold text-[#c94810] hover:underline" to={`${ROOT}/${order.id}`}>Chi tiết</Link> },
    ];

    const pageCount = Math.max(1, Math.ceil(orders.length / PAGE_SIZE));
    const currentPage = Math.min(page, pageCount);
    const visibleOrders = orders.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
    const supplierOptions = [
        { value: "", label: "Tất cả nhà cung cấp" },
        ...suppliers.map((supplier) => ({
            value: String(supplier.id),
            label: `${supplier.name} (${supplier.code})`,
        })),
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Quản lý đơn hàng nhập"
                    description="Danh sách Purchase Order lấy từ Backend. Tìm kiếm, lọc và xem chi tiết đơn mua hàng theo Issue #9."
                    actions={<Button type="button" onClick={() => navigate(`${ROOT}/new`)}>+ Tạo đơn mua hàng</Button>}
                />
                <Alert type="info">
                    Đơn PO-AUTO có thể đã được Backend tạo cùng phiếu nhập hàng. Chỉ tạo đơn mới khi đã thống nhất dữ liệu thử với nhóm; tạo đơn DRAFT không thay đổi tồn kho.
                </Alert>
                {supplierWarning && <div role="alert"><Alert type="warning">{supplierWarning}</Alert></div>}
                {filterError && <div role="alert"><Alert type="danger">{filterError}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <Card>
                    <form onSubmit={applyFilters} className="grid grid-cols-1 items-end gap-3 md:grid-cols-2 xl:grid-cols-5">
                        <SearchInput
                            label="Mã đơn hoặc tên nhà cung cấp"
                            placeholder="Nhập mã PO hoặc tên NCC..."
                            value={draft.keyword}
                            onChange={(event) => setDraft((value) => ({ ...value, keyword: event.target.value }))}
                        />
                        <Select
                            label="Trạng thái"
                            value={draft.status}
                            options={PURCHASE_ORDER_STATUS_OPTIONS}
                            onChange={(event) => setDraft((value) => ({ ...value, status: event.target.value }))}
                        />
                        <Select
                            label="Nhà cung cấp"
                            value={draft.supplierId}
                            options={supplierOptions}
                            onChange={(event) => setDraft((value) => ({ ...value, supplierId: event.target.value }))}
                        />
                        <Input
                            label="Tạo từ ngày"
                            type="date"
                            value={draft.createdFrom}
                            onChange={(event) => setDraft((value) => ({ ...value, createdFrom: event.target.value }))}
                        />
                        <Input
                            label="Đến ngày"
                            type="date"
                            value={draft.createdTo}
                            onChange={(event) => setDraft((value) => ({ ...value, createdTo: event.target.value }))}
                        />
                        <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-5">
                            <Button type="submit">Tìm kiếm / Lọc</Button>
                            <Button type="button" variant="outline" onClick={resetFilters}>Xóa bộ lọc</Button>
                            <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                        </div>
                    </form>
                    <p className="mt-4 text-sm text-slate-500">
                        {loading ? "Đang tải..." : `Tìm thấy ${orders.length} đơn mua hàng (phân trang trên giao diện, 10 đơn/trang).`}
                    </p>
                </Card>
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Đang tải danh sách đơn mua hàng...
                    </div>
                ) : error ? null : orders.length === 0 ? (
                    <EmptyState title="Không tìm thấy đơn mua hàng" description="Hãy thử thay đổi bộ lọc." />
                ) : (
                    <div className="space-y-4">
                        <Table columns={columns} data={visibleOrders} />
                        <Pagination page={currentPage} totalPages={pageCount} onChange={setPage} />
                    </div>
                )}
            </div>
        </MainLayout>
    );
}
