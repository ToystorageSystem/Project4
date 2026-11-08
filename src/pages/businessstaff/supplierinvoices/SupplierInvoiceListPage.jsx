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

const ROOT = "/business/supplier-invoices";
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
                    throw new Error("Invalid invoice list data");
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
            label: "Invoice Number / Internal ID",
            render: (value, invoice) => (
                <div className="space-y-1 whitespace-nowrap">
                    <Link to={`${ROOT}/${encodeURIComponent(invoice.id)}`} className="font-semibold text-[#c94810] hover:underline">{value || "—"}</Link>
                    <p className="text-xs text-slate-500">{invoice.invoiceCode || "—"}</p>
                </div>
            ),
        },
        {
            key: "supplierName",
            label: "Supplier",
            render: (value, invoice) => (
                <div>
                    <p className="font-medium">{value || "—"}</p>
                    <p className="text-xs text-slate-500">{invoice.supplierCode || "—"}</p>
                </div>
            ),
        },
        {
            key: "purchaseOrderCode",
            label: "Purchase Orders",
            render: (value, invoice) => invoice.purchaseOrderId ? (
                <Link
                    to={`/business/purchase-orders/${encodeURIComponent(invoice.purchaseOrderId)}`}
                    className="whitespace-nowrap font-semibold text-[#c94810] hover:underline"
                >
                    {value || `PO #${invoice.purchaseOrderId}`}
                </Link>
            ) : "—",
        },
        { key: "invoiceDate", label: "Invoice Date", render: formatSupplierInvoiceDate },
        { key: "dueDate", label: "Due Date", render: formatSupplierInvoiceDate },
        {
            key: "totalAmount",
            label: "Invoice Amount",
            render: (value, invoice) => (
                <div className="whitespace-nowrap">
                    <p className="font-semibold">{formatSupplierInvoiceMoney(value)}</p>
                    <p className="text-xs text-slate-500">Before Tax: {formatSupplierInvoiceMoney(invoice.subtotal)}</p>
                </div>
            ),
        },
        {
            key: "status",
            label: "Status",
            render: (status) => <SupplierInvoiceStatusBadge status={status} />,
        },
        {
            key: "purchaseOrderMatched",
            label: "Purchase Order Reconciliation",
            render: (_value, invoice) => <SupplierInvoiceReconciliation invoice={invoice} />,
        },
        {
            key: "hasAttachment",
            label: "Invoice File",
            render: (value) => (
                <span className={value ? "font-medium text-emerald-700" : "text-slate-500"}>
                    {value === true ? "Attached" : value === false ? "Not Attached" : "—"}
                </span>
            ),
        },
        {
            key: "id",
            label: "Actions",
            render: (value) => (
                <Link to={`${ROOT}/${encodeURIComponent(value)}`} className="font-semibold text-[#c94810] hover:underline">Details</Link>
            ),
        },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Supplier Invoice Management"
                    description="Track supplier invoices, payment status, and differences from purchase order amounts."
                    actions={<Link to={`${ROOT}/new`} className="inline-flex h-10 items-center justify-center rounded-xl bg-[#f25d19] px-4 text-sm font-medium text-white hover:bg-[#d94f12]">+ Create Invoice</Link>}
                />
                <Alert type="info">
                    Search and view invoices here. Creating or editing an invoice requires confirmation. Reconciliation uses the <strong>pre-tax amount</strong>. Confirmed invoices may still differ from the purchase order amount.
                </Alert>
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <Card>
                    <form onSubmit={applyFilters} className="grid grid-cols-1 items-end gap-3 md:grid-cols-2">
                        <SearchInput
                            label="Invoice number, internal code, supplier, or purchase order code"
                            placeholder="Enter search terms..."
                            value={draft.keyword}
                            onChange={(event) => setDraft((value) => ({ ...value, keyword: event.target.value }))}
                        />
                        <Select
                            label="Invoice Status"
                            value={draft.status}
                            options={SUPPLIER_INVOICE_STATUS_OPTIONS}
                            onChange={(event) => setDraft((value) => ({ ...value, status: event.target.value }))}
                        />
                        <div className="flex flex-wrap gap-2 md:col-span-2">
                            <Button type="submit">Search / Filters</Button>
                            <Button type="button" variant="outline" onClick={clearFilters}>Clear Filters</Button>
                            <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Refresh</Button>
                        </div>
                    </form>
                    <p className="mt-4 text-sm text-slate-500" aria-live="polite">
                        {loading ? "Loading invoices..." : `Found ${invoices.length} invoices (client-side pagination, 10 invoices per page).`}
                    </p>
                </Card>
                {!loading && !error && mismatchCount > 0 && (
                    <div role="alert">
                        <Alert type="warning">
                            Yes <strong>{mismatchCount}</strong> invoices have a pre-tax amount that differs from the purchase order. Review before confirming or paying.
                        </Alert>
                    </div>
                )}
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Loading invoice list...
                    </div>
                ) : error ? null : invoices.length === 0 ? (
                    <EmptyState title="No Invoices Found" description="Try changing the keyword or status." />
                ) : (
                    <div className="space-y-4">
                        <Table columns={columns} data={visibleInvoices} />
                        <Pagination page={currentPage} totalPages={pageCount} onChange={setPage} />
                    </div>
                )}
                <p className="text-xs text-slate-500">
                    Invoice creation, editing, cancellation, and file upload will be added in later steps. This page is currently for viewing and searching only.
                </p>
            </div>
        </MainLayout>
    );
}
