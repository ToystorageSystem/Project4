import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
    listSupplierInvoices,
    supplierInvoiceErrorMessage,
} from "../../../api/business/supplierInvoices";
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
import {
    formatSupplierInvoiceDate,
    formatSupplierInvoiceMoney,
    SUPPLIER_INVOICE_STATUS_OPTIONS,
    SupplierInvoiceReconciliation,
    SupplierInvoiceStatusBadge,
} from "./supplierInvoiceFormat";

const ROOT = "/business-staff/supplier-invoices";
const PAGE_SIZE = 10;
const EMPTY_FILTERS = { keyword: "", status: "" };

export default function SupplierInvoiceListPage() {
    const [draft, setDraft] = useState(EMPTY_FILTERS);
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refresh, setRefresh] = useState(0);
    const [page, setPage] = useState(1);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");

        listSupplierInvoices(filters, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data)) {
                    throw new Error("Invalid supplier invoice list response");
                }
                setInvoices(data);
            })
            .catch((requestError) => {
                if (controller.signal.aborted) return;
                setInvoices([]);
                setError(supplierInvoiceErrorMessage(requestError));
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });

        return () => controller.abort();
    }, [filters, refresh]);

    const applyFilters = (event) => {
        event.preventDefault();
        setFilters({ keyword: draft.keyword.trim(), status: draft.status });
        setPage(1);
    };

    const clearFilters = () => {
        setDraft(EMPTY_FILTERS);
        setFilters(EMPTY_FILTERS);
        setPage(1);
        setRefresh((value) => value + 1);
    };

    const mismatchCount = invoices.filter((item) => item.purchaseOrderMatched === false).length;
    const pageCount = Math.max(1, Math.ceil(invoices.length / PAGE_SIZE));
    const currentPage = Math.min(page, pageCount);
    const visibleInvoices = invoices.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

    const columns = [
        {
            key: "invoiceNumber",
            label: "Số hóa đơn / Mã nội bộ",
            render: (value, invoice) => (
                <div className="space-y-1 whitespace-nowrap">
                    <Link to={`${ROOT}/${encodeURIComponent(invoice.id)}`} className="font-semibold text-[#c94810] hover:underline">{value || "—"}</Link>
                    <p className="text-xs text-slate-500">{invoice.invoiceCode || "—"}</p>
                </div>
            ),
        },
        {
            key: "supplierName",
            label: "Nhà cung cấp",
            render: (value, invoice) => (
                <div>
                    <p className="font-medium">{value || "—"}</p>
                    <p className="text-xs text-slate-500">{invoice.supplierCode || "—"}</p>
                </div>
            ),
        },
        {
            key: "purchaseOrderCode",
            label: "Đơn mua hàng",
            render: (value, invoice) => invoice.purchaseOrderId ? (
                <Link
                    to={`/business-staff/purchase-orders/${encodeURIComponent(invoice.purchaseOrderId)}`}
                    className="whitespace-nowrap font-semibold text-[#c94810] hover:underline"
                >
                    {value || `PO #${invoice.purchaseOrderId}`}
                </Link>
            ) : "—",
        },
        { key: "invoiceDate", label: "Ngày hóa đơn", render: formatSupplierInvoiceDate },
        { key: "dueDate", label: "Hạn thanh toán", render: formatSupplierInvoiceDate },
        {
            key: "totalAmount",
            label: "Giá trị hóa đơn",
            render: (value, invoice) => (
                <div className="whitespace-nowrap">
                    <p className="font-semibold">{formatSupplierInvoiceMoney(value)}</p>
                    <p className="text-xs text-slate-500">Trước thuế: {formatSupplierInvoiceMoney(invoice.subtotal)}</p>
                </div>
            ),
        },
        {
            key: "status",
            label: "Trạng thái",
            render: (status) => <SupplierInvoiceStatusBadge status={status} />,
        },
        {
            key: "purchaseOrderMatched",
            label: "Đối chiếu PO",
            render: (_value, invoice) => <SupplierInvoiceReconciliation invoice={invoice} />,
        },
        {
            key: "hasAttachment",
            label: "Tệp hóa đơn",
            render: (value) => (
                <span className={value ? "font-medium text-emerald-700" : "text-slate-500"}>
                    {value === true ? "Có đính kèm" : value === false ? "Chưa đính kèm" : "—"}
                </span>
            ),
        },
        {
            key: "id",
            label: "Thao tác",
            render: (value) => (
                <Link to={`${ROOT}/${encodeURIComponent(value)}`} className="font-semibold text-[#c94810] hover:underline">Chi tiết</Link>
            ),
        },
    ];

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Quản lý hóa đơn nhà cung cấp"
                    description="Danh sách hóa đơn lấy trực tiếp từ Backend. Tìm kiếm, lọc trạng thái và theo dõi chênh lệch với đơn mua hàng — Issue #10."
                    actions={<Link to={`${ROOT}/new`} className="inline-flex h-10 items-center justify-center rounded-xl bg-[#f25d19] px-4 text-sm font-medium text-white hover:bg-[#d94f12]">+ Tạo hóa đơn</Link>}
                />
                <Alert type="info">
                    Danh sách chỉ đọc. Khi tạo/sửa hóa đơn, hệ thống yêu cầu xác nhận trước khi ghi MySQL chung. Backend đối chiếu giá trị <strong>trước thuế</strong> của hóa đơn với giá trị đơn mua hàng.
                    Trạng thái VERIFIED không đồng nghĩa với việc số tiền đã khớp PO.
                </Alert>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <Card>
                    <form onSubmit={applyFilters} className="grid grid-cols-1 items-end gap-3 md:grid-cols-2">
                        <SearchInput
                            label="Số hóa đơn, mã hóa đơn, tên NCC hoặc mã PO"
                            placeholder="Nhập thông tin cần tìm..."
                            value={draft.keyword}
                            onChange={(event) => setDraft((value) => ({ ...value, keyword: event.target.value }))}
                        />
                        <Select
                            label="Trạng thái hóa đơn"
                            value={draft.status}
                            options={SUPPLIER_INVOICE_STATUS_OPTIONS}
                            onChange={(event) => setDraft((value) => ({ ...value, status: event.target.value }))}
                        />
                        <div className="flex flex-wrap gap-2 md:col-span-2">
                            <Button type="submit">Tìm kiếm / Lọc</Button>
                            <Button type="button" variant="outline" onClick={clearFilters}>Xóa bộ lọc</Button>
                            <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Làm mới</Button>
                        </div>
                    </form>
                    <p className="mt-4 text-sm text-slate-500" aria-live="polite">
                        {loading ? "Đang tải hóa đơn..." : `Tìm thấy ${invoices.length} hóa đơn (phân trang trên giao diện, 10 hóa đơn/trang).`}
                    </p>
                </Card>
                {!loading && !error && mismatchCount > 0 && (
                    <div role="alert">
                        <Alert type="warning">
                            Có <strong>{mismatchCount}</strong> hóa đơn có giá trị trước thuế chưa khớp với PO.
                            Hãy kiểm tra số liệu trước khi xác nhận hoặc thanh toán.
                        </Alert>
                    </div>
                )}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Đang tải danh sách hóa đơn...
                    </div>
                ) : error ? null : invoices.length === 0 ? (
                    <EmptyState title="Không tìm thấy hóa đơn" description="Hãy thử thay đổi từ khóa hoặc trạng thái." />
                ) : (
                    <div className="space-y-4">
                        <Table columns={columns} data={visibleInvoices} />
                        <Pagination page={currentPage} totalPages={pageCount} onChange={setPage} />
                    </div>
                )}
                <p className="text-xs text-slate-500">
                    Chức năng tạo/sửa, hủy hóa đơn và tải file sẽ được bổ sung ở các bước tiếp theo.
                    Trang này không gửi API ghi dữ liệu.
                </p>
            </div>
        </MainLayout>
    );
}
