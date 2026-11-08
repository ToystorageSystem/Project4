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

const ROOT = "/business/purchase-orders";
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
                if (!Array.isArray(data)) throw new Error("Invalid supplier list data");
                setSuppliers(data);
            })
            .catch((requestError) => {
                if (!controller.signal.aborted) {
                    setSupplierWarning(purchaseOrderErrorMessage(requestError, "Unable to load suppliers for filtering."));
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
                if (!Array.isArray(data)) throw new Error("Invalid purchase order list data");
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
            setFilterError("Start date cannot be after end date.");
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
            label: "Purchase Order Code",
            render: (value, order) => (
                <div className="space-y-1">
                    <Link to={`${ROOT}/${order.id}`} className="font-semibold text-[#c94810] hover:underline">
                        {value || "—"}
                    </Link>
                    {String(value || "").startsWith("PO-AUTO-") && (
                        <p className="text-xs text-slate-500">Automatically Generated from Goods Receipt</p>
                    )}
                </div>
            ),
        },
        { key: "supplierName", label: "Supplier", render: (value) => value || "—" },
        { key: "warehouseName", label: "Destination Warehouse / Store", render: (value) => value || "—" },
        { key: "status", label: "Status", render: (value) => <PurchaseOrderStatusBadge status={value} /> },
        { key: "totalProducts", label: "Product Count", render: (value) => value ?? "—" },
        { key: "totalOrderedQuantity", label: "Ordered Quantity", render: (value) => value ?? "—" },
        { key: "totalAmount", label: "Total Amount", render: (value) => <span className="whitespace-nowrap font-semibold">{formatPurchaseOrderMoney(value)}</span> },
        { key: "expectedDeliveryDate", label: "Expected Delivery", render: formatExpectedDeliveryDate },
        { key: "createdAt", label: "Created At", render: formatPurchaseOrderDate },
        { key: "id", label: "Actions", render: (_value, order) => <Link className="font-semibold text-[#c94810] hover:underline" to={`${ROOT}/${order.id}`}>Details</Link> },
    ];

    const pageCount = Math.max(1, Math.ceil(orders.length / PAGE_SIZE));
    const currentPage = Math.min(page, pageCount);
    const visibleOrders = orders.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
    const supplierOptions = [
        { value: "", label: "All Suppliers" },
        ...suppliers.map((supplier) => ({
            value: String(supplier.id),
            label: `${supplier.name} (${supplier.code})`,
        })),
    ];

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
                <PageHeader
                    title="Purchase Order Management"
                    description="Search purchase orders by order code, supplier, status, or creation date."
                    actions={<Button type="button" onClick={() => navigate(`${ROOT}/new`)}>+ Create Purchase Order</Button>}
                />
                <Alert type="info">
                    Some orders are created automatically during goods receipt creation. Creating a new draft order does not change warehouse stock quantities.
                </Alert>
                {supplierWarning && <div role="alert"><Alert type="warning">{supplierWarning}</Alert></div>}
                {filterError && <div role="alert"><Alert type="danger">{filterError}</Alert></div>}
                {error && <div role="alert"><Alert type="danger">{error}</Alert></div>}
                <Card>
                    <form onSubmit={applyFilters} className="grid grid-cols-1 items-end gap-3 md:grid-cols-2 xl:grid-cols-5">
                        <SearchInput
                            label="Order Code or Supplier Name"
                            placeholder="Enter purchase order code or supplier name..."
                            value={draft.keyword}
                            onChange={(event) => setDraft((value) => ({ ...value, keyword: event.target.value }))}
                        />
                        <Select
                            label="Status"
                            value={draft.status}
                            options={PURCHASE_ORDER_STATUS_OPTIONS}
                            onChange={(event) => setDraft((value) => ({ ...value, status: event.target.value }))}
                        />
                        <Select
                            label="Supplier"
                            value={draft.supplierId}
                            options={supplierOptions}
                            onChange={(event) => setDraft((value) => ({ ...value, supplierId: event.target.value }))}
                        />
                        <Input
                            label="Created From"
                            type="date"
                            value={draft.createdFrom}
                            onChange={(event) => setDraft((value) => ({ ...value, createdFrom: event.target.value }))}
                        />
                        <Input
                            label="To Date"
                            type="date"
                            value={draft.createdTo}
                            onChange={(event) => setDraft((value) => ({ ...value, createdTo: event.target.value }))}
                        />
                        <div className="flex flex-wrap gap-2 md:col-span-2 xl:col-span-5">
                            <Button type="submit">Search / Filters</Button>
                            <Button type="button" variant="outline" onClick={resetFilters}>Clear Filters</Button>
                            <Button type="button" variant="ghost" onClick={() => setRefresh((value) => value + 1)}>Refresh</Button>
                        </div>
                    </form>
                    <p className="mt-4 text-sm text-slate-500">
                        {loading ? "Loading..." : `Found ${orders.length} purchase orders (client-side pagination, 10 orders per page).`}
                    </p>
                </Card>
                {loading ? (
                    <div className="flex items-center gap-3 rounded-2xl bg-white p-7 text-sm text-slate-600" role="status">
                        <Spinner /> Loading purchase orders...
                    </div>
                ) : error ? null : orders.length === 0 ? (
                    <EmptyState title="Purchase Order Not Found" description="Adjust the filters and try again." />
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

