
import { useEffect, useState } from "react";
import { aggregateWarehouseProducts } from "../../../utils/businessInventorySummary";
import {
    getInventoryReport,
    getInventoryWarehouseOptions,
    getInventoryCategoryOptions,
    getInventoryBrandOptions,
    exportInventoryReport,
    inventoryReportErrorMessage,
} from "../../../api/business/inventoryReport";
import MainLayout from "../../../components/layout/MainLayout";
import { businessStaffMenuItems } from "../../../components/icons/businessMenuItems";

const initialFilters = {
    keyword: "",
    warehouseId: "",
    categoryId: "",
    brandId: "",
};

const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500";

function formatNumber(value) {
    return Number(value ?? 0).toLocaleString("en-GB");
}

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString("en-GB");
}

function StockStatusBadge({ status, onHandQuantity, availableQuantity }) {
    // Hết hàng thực tế khác với còn hàng nhưng chưa thể cấp phát/bán.
    const unavailable = status === "OUT_OF_STOCK"
        && Number(onHandQuantity ?? 0) > 0
        && Number(availableQuantity ?? 0) === 0;
    const config = {
        IN_STOCK: {
            text: "In Stock",
            className: "bg-green-100 text-green-700",
        },
        LOW_STOCK: {
            text: "Low Stock",
            className: "bg-yellow-100 text-yellow-800",
        },
        OUT_OF_STOCK: {
            text: unavailable ? "Unavailable" : "Out of Stock",
            className: "bg-red-100 text-red-700",
        },
    };

    const item = config[status] ?? {
        text: status || "Unknown",
        className: "bg-slate-100 text-slate-700",
    };

    return (
        <span className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${item.className}`}>
            {item.text}
        </span>
    );
}


function ProductLocationsDialog({ product, onClose }) {
    // Lay so luong ton kho cua dung vi tri da chon.
    const onHand = Number(product.onHandQuantity ?? 0);
    const available = Number(product.availableQuantity ?? 0);
    const unavailable = onHand > 0 && available === 0;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            role="dialog"
            aria-modal="true"
            aria-label="Inventory Details by Warehouse"
        >
            <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl">
                <div className="mb-5 flex items-start justify-between gap-4">
                    <div>
                        <h2 className="text-xl font-semibold text-slate-900">
                            Inventory Details by Warehouse
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            {product.productCode} — {product.productName}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    >
                        Close
                    </button>
                </div>

                <div className="mb-5 grid gap-3">
                    <div className="rounded-lg bg-slate-50 p-4">
                        <p className="mb-1 text-xs font-medium text-slate-500">
                            Warehouse / Store
                        </p>

                        <p className="font-semibold text-slate-900">
                            {product.warehouseName || "—"}
                        </p>
                    </div>

                </div>

                <div className="mb-5 grid gap-3 sm:grid-cols-3">
                    <div className="rounded-lg border border-slate-200 p-4 text-center">
                        <p className="mb-2 text-xs text-slate-500">
                            Physical Stock
                        </p>

                        <p className="text-2xl font-bold text-slate-900">
                            {formatNumber(product.onHandQuantity)}
                        </p>
                    </div>

                    <div className="rounded-lg border border-slate-200 p-4 text-center">
                        <p className="mb-2 text-xs text-slate-500">
                            Reserved Stock
                        </p>

                        <p className="text-2xl font-bold text-amber-600">
                            {formatNumber(product.reservedQuantity)}
                        </p>
                    </div>

                    <div className="rounded-lg border border-slate-200 p-4 text-center">
                        <p className="mb-2 text-xs text-slate-500">
                            Available Stock
                        </p>

                        <p className="text-2xl font-bold text-green-700">
                            {formatNumber(product.availableQuantity)}
                        </p>
                    </div>
                </div>

                <div className="space-y-3 rounded-lg bg-slate-50 p-4 text-sm">
                    <div className="flex items-center justify-between gap-4">
                        <span className="text-slate-500">
                            Stock Status
                        </span>

                        <StockStatusBadge
                            status={product.stockStatus}
                            onHandQuantity={product.onHandQuantity}
                            availableQuantity={product.availableQuantity}
                        />
                    </div>

                    <div className="flex items-center justify-between gap-4">
                        <span className="text-slate-500">
                            Last Updated
                        </span>

                        <strong className="text-slate-900">
                            {formatDate(product.lastUpdatedAt)}
                        </strong>
                    </div>
                </div>

                {unavailable && (
                    <p className="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
                        Some stock is not available for allocation. Contact the warehouse team for details.
                    </p>
                )}
            </div>
        </div>
    );
}


export default function InventoryReportPage() {
    const [draftFilters, setDraftFilters] = useState(initialFilters);
    const [filters, setFilters] = useState(initialFilters);
    const [page, setPage] = useState(0);

    const [report, setReport] = useState({
        items: [],
        totalElements: 0,
        totalPages: 0,
    });

    const [options, setOptions] = useState({
        warehouses: [],
        categories: [],
        brands: [],
    });

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [optionsError, setOptionsError] = useState("");
    const [exporting, setExporting] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);

    // Load warehouse, category and brand options.
    useEffect(() => {
        const controller = new AbortController();

        Promise.all([
            getInventoryWarehouseOptions(controller.signal),
            getInventoryCategoryOptions(controller.signal),
            getInventoryBrandOptions(controller.signal),
        ])
            .then(([warehouses, categories, brands]) => {
                if (!controller.signal.aborted) {
                    setOptions({
                        warehouses: warehouses ?? [],
                        categories: categories ?? [],
                        brands: brands ?? [],
                    });
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setOptionsError(inventoryReportErrorMessage(err));
                }
            });

        return () => controller.abort();
    }, []);

    // Load inventory whenever the filters change; paginate aggregated rows locally.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        (async () => {
            // Backend phan trang theo vi tri. Lay het cac trang truoc khi gom theo kho + san pham.
            // Khong hien thi tong cong sai neu khong tai duoc du du lieu.
            const all = [];
            let currentPage = 0;
            let totalPages = 1;
            do {
                if (currentPage >= 50) {
                    throw new Error("Inventory is too large for this view. Please select a warehouse or narrow the filters.");
                }
                const data = await getInventoryReport(
                    { ...filters, page: currentPage, size: 100 }, controller.signal
                );
                if (!Array.isArray(data?.items)) throw new Error("Invalid inventory response.");
                all.push(...data.items);
                totalPages = Number(data.totalPages) || 0;
                currentPage += 1;
            } while (currentPage < totalPages);
            if (!controller.signal.aborted) {
                const items = aggregateWarehouseProducts(all);
                setReport({ items, totalElements: items.length, totalPages: Math.ceil(items.length / 10) });
            }
        })()
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setError(err?.message?.startsWith("Inventory is too large") || err?.message === "Invalid inventory response."
                        ? err.message : inventoryReportErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters]);

    function updateDraft(name, value) {
        setDraftFilters((current) => ({
            ...current,
            [name]: value,
        }));
    }

    function applyFilters(event) {
        event.preventDefault();
        setPage(0);
        setFilters({
            ...draftFilters,
            keyword: draftFilters.keyword.trim(),
        });
    }

    function resetFilters() {
        setDraftFilters({ ...initialFilters });
        setFilters({ ...initialFilters });
        setPage(0);
    }

    async function handleExport() {
        setExporting(true);
        setError("");

        try {
            const { blob, filename } = await exportInventoryReport(filters);

            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");

            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();

            window.setTimeout(() => URL.revokeObjectURL(url), 1000);
        }  catch (err) {
            setError(
                inventoryReportErrorMessage(err, "Unable to export Excel file.")
            );
        } finally {
            setExporting(false);
        }
    }

    const allItems = report?.items ?? [];
    const items = allItems.slice(page * 10, (page + 1) * 10);
    const totalPages = report?.totalPages ?? 0;

    return (
        <MainLayout title="Business Operations" items={businessStaffMenuItems}>
            <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">
                        Inventory Report
                    </h1>
                    <p className="mt-1 text-sm text-slate-500">
                        Monitor inventory across main warehouses and stores
                    </p>
                </div>

                <button
                    type="button"
                    onClick={handleExport}
                    disabled={exporting}
                    className="rounded-lg bg-green-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                >
                    {exporting ? "Exporting..." : "Export Detailed Excel"}
                </button>
            </div>

            <form
                onSubmit={applyFilters}
                className="space-y-4 rounded-xl border border-slate-200 bg-white p-5"
            >
                <h2 className="font-semibold text-slate-800">
                    Search & Filters
                </h2>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    <input
                        className={inputClass}
                        placeholder="Product code, name, or barcode..."
                        value={draftFilters.keyword}
                        onChange={(e) => updateDraft("keyword", e.target.value)}
                    />

                    <select
                        className={inputClass}
                        value={draftFilters.warehouseId}
                        onChange={(e) => updateDraft("warehouseId", e.target.value)}
                    >
                        <option value="">All Warehouses / Stores</option>
                        {options.warehouses.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>

                    <select
                        className={inputClass}
                        value={draftFilters.categoryId}
                        onChange={(e) => updateDraft("categoryId", e.target.value)}
                    >
                        <option value="">All Categories</option>
                        {options.categories.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>

                    <select
                        className={inputClass}
                        value={draftFilters.brandId}
                        onChange={(e) => updateDraft("brandId", e.target.value)}
                    >
                        <option value="">All Brands</option>
                        {options.brands.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>

                </div>

                <div className="flex flex-wrap gap-3">
                    <button
                        type="submit"
                        className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700"
                    >
                        Search / Filters
                    </button>

                    <button
                        type="button"
                        onClick={resetFilters}
                        className="rounded-lg border border-slate-300 px-5 py-2 text-sm text-slate-700"
                    >
                        Reset
                    </button>
                </div>

                {optionsError && (
                    <p role="alert" className="text-sm text-red-600">
                        Unable to load filters: {optionsError}
                    </p>
                )}
            </form>

            <section className="rounded-xl border border-slate-200 bg-white">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b p-5">
                    <h2 className="font-semibold text-slate-800">
                        Inventory List
                    </h2>
                    <span className="text-sm text-slate-500">
                        {formatNumber(report?.totalElements)} product / warehouse rows
                    </span>
                </div>

                {error && (
                    <p role="alert" className="m-5 rounded-lg bg-red-50 p-3 text-red-700">
                        {error}
                    </p>
                )}

                {loading ? (
                    <p className="p-8 text-center text-slate-500">
                        Loading inventory report...
                    </p>
                ) : error ? null : (
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[1300px] text-left text-sm">
                            <thead className="bg-slate-100 text-slate-700">
                                <tr>
                                    <th className="p-3">Warehouse / Store</th>
                                    <th className="p-3">SKU</th>
                                    <th className="p-3">Product Name</th>
                                    <th className="p-3">Barcode</th>
                                    <th className="p-3">Category / Brand</th>
                                    <th className="p-3 text-right">Actual</th>
                                    <th className="p-3 text-right">Reserved</th>
                                    <th className="p-3 text-right">Available</th>
                                    <th className="p-3">Availability</th>
                                    <th className="p-3">Updated At</th>
                                    <th className="p-3">Details</th>
                                </tr>
                            </thead>

                            <tbody>
                                {items.map((item) => (
                                    <tr
                                        key={item.inventoryBalanceId}
                                        className="border-b border-slate-100 hover:bg-slate-50"
                                    >
                                        <td className="p-3">{item.warehouseName}</td>
                                        <td className="p-3 font-medium">{item.productCode}</td>
                                        <td className="p-3">{item.productName}</td>
                                        <td className="p-3">{item.barcode || "—"}</td>
                                        <td className="p-3">
                                            {item.categoryName || "—"}
                                            <div className="text-xs text-slate-500">
                                                {item.brandName || "—"}
                                            </div>
                                        </td>
                                        <td className="p-3 text-right">
                                            {formatNumber(item.onHandQuantity)}
                                        </td>
                                        <td className="p-3 text-right">
                                            {formatNumber(item.reservedQuantity)}
                                        </td>
                                        <td className="p-3 text-right font-semibold">
                                            {formatNumber(item.availableQuantity)}
                                        </td>
                                        <td className="p-3">
                                            {formatDate(item.lastUpdatedAt)}
                                        </td>
                                        <td className="p-3">
                                            <button
                                                type="button"
                                                onClick={() => setSelectedProduct(item)}
                                                className="font-medium text-blue-600 hover:underline"
                                            >
                                                View
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {items.length === 0 && (
                            <p className="p-8 text-center text-slate-500">
                                No matching inventory records found.
                            </p>
                        )}
                    </div>
                )}

                <div className="flex items-center justify-between border-t p-4">
                    <span className="text-sm text-slate-500">
                        Page {page + 1} / {Math.max(totalPages, 1)}
                    </span>

                    <div className="flex gap-2">
                        <button
                            type="button"
                            disabled={loading || page === 0}
                            onClick={() => setPage((current) => Math.max(0, current - 1))}
                            className="rounded border px-4 py-2 text-sm disabled:opacity-40"
                        >
                            Previous
                        </button>

                        <button
                            type="button"
                            disabled={loading || page + 1 >= totalPages}
                            onClick={() => setPage((current) => current + 1)}
                            className="rounded border px-4 py-2 text-sm disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                </div>
            </section>

            {selectedProduct && (
                <ProductLocationsDialog
                    product={selectedProduct}
                    onClose={() => setSelectedProduct(null)}
                />
            )}
        </div>
      </MainLayout>
    );
}

