import { formatBusinessStatus } from "../../../utils/viStatus";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
    getBusinessGoodsReceipts,
    getGoodsReceiptMainWarehouseOptions,
    goodsReceiptErrorMessage,
} from "../../../api/business/goodsReceipts";
import MainLayout from "../../../components/layout/MainLayout";
import PageHeader from "../../../components/layout/PageHeader";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";
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
import { formatReceiptDate, receiptStatusLabels } from "./receiptText";

const ROOT = "/business/goods-receipts";
const PAGE_SIZE = 10;
const BASE_FILTERS = { keyword: "", status: "", warehouseId: "", fromDate: "", toDate: "" };
const STATUS_OPTIONS = [
    { value: "", label: "All Statuses" },
    ...Object.entries(receiptStatusLabels).map(([value, label]) => ({ value, label })),
];

function ReceiptStatus({ status }) {
    const color = status === "COMPLETED"
        ? "bg-green-100 text-green-700"
        : status === "CANCELLED"
            ? "bg-red-100 text-red-700"
            : status === "RECEIVING" || status === "INSPECTED"
                ? "bg-blue-100 text-blue-700"
                : "bg-amber-100 text-amber-800";
    return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${color}`}>
        {receiptStatusLabels[status] || (status ? formatBusinessStatus(status) : "—")}
    </span>;
}

export default function GoodsReceiptListPage() {
    const [searchParams] = useSearchParams();
    const poParam = Number(searchParams.get("purchaseOrderId"));
    const purchaseOrderId = Number.isSafeInteger(poParam) && poParam > 0 ? poParam : null;
    const [draft, setDraft] = useState(BASE_FILTERS);
    const [filters, setFilters] = useState(BASE_FILTERS);
    const [page, setPage] = useState(0);
    const [reload, setReload] = useState(0);
    const [warehouses, setWarehouses] = useState([]);
    const [optionsError, setOptionsError] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [filterError, setFilterError] = useState("");
    const [result, setResult] = useState({ items: [], totalPages: 0, totalElements: 0 });

    useEffect(() => {
        const controller = new AbortController();
        getGoodsReceiptMainWarehouseOptions(controller.signal)
            .then((data) => { if (!controller.signal.aborted) setWarehouses(Array.isArray(data) ? data : []); })
            .catch((err) => {
                if (!controller.signal.aborted) setOptionsError(goodsReceiptErrorMessage(err, "Unable to load warehouses."));
            });
        return () => controller.abort();
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError("");
        getBusinessGoodsReceipts({ ...filters, purchaseOrderId, page, size: PAGE_SIZE }, controller.signal)
            .then((data) => {
                if (controller.signal.aborted) return;
                if (!Array.isArray(data?.items)) throw new Error("Invalid goods receipt list data.");
                setResult(data);
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setError(err?.isAxiosError ? goodsReceiptErrorMessage(err) : err.message);
                }
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [filters, purchaseOrderId, page, reload]);

    function applyFilters(event) {
        event.preventDefault();
        if (draft.fromDate && draft.toDate && draft.fromDate > draft.toDate) {
            setFilterError("From date cannot be after to date.");
            return;
        }
        setFilterError("");
        setPage(0);
        setFilters({ ...draft, keyword: draft.keyword.trim() });
    }

    function resetFilters() {
        setDraft({ ...BASE_FILTERS });
        setFilters({ ...BASE_FILTERS });
        setFilterError("");
        setPage(0);
    }

    const columns = [
        { key: "receiptCode", label: "Goods Receipt Code", render: (code, item) => (
            <Link to={`${ROOT}/${item.id}`} className="font-semibold text-[#c94810] hover:underline">{code}</Link>
        ) },
        { key: "purchaseOrderCode", label: "Purchase Orders", render: (code, item) => (
            <Link to={`/business/purchase-orders/${item.purchaseOrderId}`} className="text-blue-700 hover:underline">
                {code || "—"}
            </Link>
        ) },
        { key: "supplierName", label: "Supplier" },
        { key: "warehouseName", label: "Destination Warehouse / Store" },
        { key: "totalProducts", label: "Product Lines", render: (n) => n ?? 0 },
        { key: "totalExpectedQuantity", label: "Expected Quantity", render: (n) => n ?? 0 },
        { key: "status", label: "Status", render: (status) => <ReceiptStatus status={status} /> },
        { key: "createdAt", label: "Created At", render: formatReceiptDate },
        { key: "id", label: "Actions", render: (id) => (
            <Link to={`${ROOT}/${id}`} className="font-semibold text-[#c94810] hover:underline">View Details</Link>
        ) },
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader title="Track Goods Receipts"
                    description="Track goods receipts and warehouse receiving progress."
                    actions={<Link to={ROOT} className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white hover:bg-orange-700">
                        + Create Goods Receipt
                    </Link>} />
                <Alert type="info">
                    Business Operations can track goods receipts. Warehouse staff inspect and complete receiving; stock balances increase only after the receipt is completed. The list covers all warehouses.
                </Alert>
                {purchaseOrderId && <Alert type="info">
                    Viewing goods receipts linked to purchase order # {purchaseOrderId}. To view all receipts,
                    <Link to={`${ROOT}/list`} className="ml-1 font-semibold underline">click here</Link>.
                </Alert>}
                {optionsError && <Alert type="warning">{optionsError}</Alert>}
                {filterError && <Alert type="danger">{filterError}</Alert>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <Card>
                    <form onSubmit={applyFilters} className="grid grid-cols-1 items-end gap-3 md:grid-cols-2 xl:grid-cols-3">
                        <SearchInput label="Goods Receipt or Purchase Order Code" placeholder="Enter receipt or purchase order code..."
                            value={draft.keyword} onChange={(e) => setDraft((p) => ({ ...p, keyword: e.target.value }))} />
                        <Select label="Status" value={draft.status} options={STATUS_OPTIONS}
                            onChange={(e) => setDraft((p) => ({ ...p, status: e.target.value }))} />
                        <Select label="Destination Warehouse / Store" value={draft.warehouseId}
                            options={[{ value: "", label: "All Main Warehouses" }, ...warehouses.map((w) => ({ value: String(w.id), label: w.name }))]}
                            onChange={(e) => setDraft((p) => ({ ...p, warehouseId: e.target.value }))} />
                        <Input label="From Date" type="date" value={draft.fromDate}
                            onChange={(e) => setDraft((p) => ({ ...p, fromDate: e.target.value }))} />
                        <Input label="To Date" type="date" value={draft.toDate}
                            onChange={(e) => setDraft((p) => ({ ...p, toDate: e.target.value }))} />
                        <div className="flex flex-wrap gap-2 xl:col-span-3">
                            <Button type="submit">Search / Filters</Button>
                            <Button type="button" variant="outline" onClick={resetFilters}>Clear Filters</Button>
                            <Button type="button" variant="ghost" onClick={() => setReload((n) => n + 1)}>Refresh</Button>
                        </div>
                    </form>
                    <p className="mt-4 text-sm text-slate-500">
                        {loading ? "Loading..." : `Found ${result.totalElements ?? 0} goods receipts.`}
                    </p>
                </Card>
                {loading ? <div className="flex items-center gap-2 rounded-xl bg-white p-6"><Spinner />Loading goods receipts...</div>
                    : error ? null
                        : !result.items?.length ? <EmptyState title="No Matching Goods Receipts" description="Adjust the filters and try again." />
                            : <div className="space-y-4">
                                <Table data={result.items} columns={columns} />
                                <Pagination page={page + 1} totalPages={Math.max(1, result.totalPages || 0)}
                                    onChange={(nextPage) => setPage(nextPage - 1)} />
                            </div>}
            </div>
        </MainLayout>
    );
}
