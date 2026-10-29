
import { useEffect, useState } from "react";
import {
    getInventoryReport,
    getInventoryWarehouseOptions,
    getInventoryCategoryOptions,
    getInventoryBrandOptions,
    getInventoryProductLocations,
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
    stockStatus: "",
};

const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500";

function formatNumber(value) {
    return Number(value ?? 0).toLocaleString("vi-VN");
}

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString("vi-VN");
}

function StockStatusBadge({ status }) {
    const config = {
        IN_STOCK: {
            text: "Còn hàng",
            className: "bg-green-100 text-green-700",
        },
        LOW_STOCK: {
            text: "Sắp hết hàng",
            className: "bg-yellow-100 text-yellow-800",
        },
        OUT_OF_STOCK: {
            text: "Hết hàng",
            className: "bg-red-100 text-red-700",
        },
    };

    const item = config[status] ?? {
        text: status || "Không xác định",
        className: "bg-slate-100 text-slate-700",
    };

    return (
        <span className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${item.className}`}>
            {item.text}
        </span>
    );
}

function ProductLocationsDialog({ product, onClose }) {
    const [locations, setLocations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        getInventoryProductLocations(product.productId, controller.signal)
            .then((data) => {
                if (!controller.signal.aborted) {
                    setLocations(Array.isArray(data) ? data : []);
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setError(inventoryReportErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [product.productId]);

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            role="dialog"
            aria-modal="true"
            aria-label="Chi tiết tồn kho sản phẩm"
        >
            <div className="w-full max-w-5xl rounded-xl bg-white p-6 shadow-xl">
                <div className="mb-5 flex items-start justify-between gap-4">
                    <div>
                        <h2 className="text-xl font-semibold text-slate-900">
                            Chi tiết tồn kho sản phẩm
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            {product.productCode} — {product.productName}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border px-3 py-2 text-sm"
                    >
                        Đóng
                    </button>
                </div>

                {loading && <p>Đang tải vị trí tồn kho...</p>}

                {error && (
                    <p role="alert" className="text-sm text-red-600">
                        {error}
                    </p>
                )}

                {!loading && !error && (
                    <div className="max-h-[65vh] overflow-auto">
                        <table className="w-full min-w-[800px] text-left text-sm">
                            <thead className="bg-slate-100">
                                <tr>
                                    <th className="p-3">Kho / Cửa hàng</th>
                                    <th className="p-3">Vị trí</th>
                                    <th className="p-3 text-right">Thực tế</th>
                                    <th className="p-3 text-right">Đã giữ</th>
                                    <th className="p-3 text-right">Khả dụng</th>
                                    <th className="p-3">Trạng thái</th>
                                </tr>
                            </thead>

                            <tbody>
                                {locations.map((item) => (
                                    <tr
                                        key={`${item.warehouseId}-${item.locationId}`}
                                        className="border-b"
                                    >
                                        <td className="p-3">{item.warehouseName}</td>
                                        <td className="p-3">{item.locationCode}</td>
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
                                            <StockStatusBadge status={item.stockStatus} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {locations.length === 0 && (
                            <p className="py-6 text-center text-slate-500">
                                Không có dữ liệu tồn kho.
                            </p>
                        )}
                    </div>
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

    // Load inventory whenever applied filters or page change.
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError("");

        getInventoryReport(
            { ...filters, page, size: 10 },
            controller.signal
        )
            .then((data) => {
                if (!controller.signal.aborted) {
                    setReport(data);
                }
            })
            .catch((err) => {
                if (!controller.signal.aborted) {
                    setError(inventoryReportErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters, page]);

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
        } catch (err) {
            setError(
                inventoryReportErrorMessage(err, "Không xuất được file Excel.")
            );
        } finally {
            setExporting(false);
        }
    }

    const items = report?.items ?? [];
    const totalPages = report?.totalPages ?? 0;

    return (
        <MainLayout title="Business Staff" items={businessStaffMenuItems}>
            <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">
                        Báo cáo tồn kho
                    </h1>
                    <p className="mt-1 text-sm text-slate-500">
                        Theo dõi số lượng tồn kho tại kho tổng và cửa hàng
                    </p>
                </div>

                <button
                    type="button"
                    onClick={handleExport}
                    disabled={exporting}
                    className="rounded-lg bg-green-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                >
                    {exporting ? "Đang xuất..." : "Xuất Excel"}
                </button>
            </div>

            <form
                onSubmit={applyFilters}
                className="space-y-4 rounded-xl border border-slate-200 bg-white p-5"
            >
                <h2 className="font-semibold text-slate-800">
                    Tìm kiếm và bộ lọc
                </h2>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    <input
                        className={inputClass}
                        placeholder="Mã, tên sản phẩm hoặc barcode..."
                        value={draftFilters.keyword}
                        onChange={(e) => updateDraft("keyword", e.target.value)}
                    />

                    <select
                        className={inputClass}
                        value={draftFilters.warehouseId}
                        onChange={(e) => updateDraft("warehouseId", e.target.value)}
                    >
                        <option value="">Tất cả kho / cửa hàng</option>
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
                        <option value="">Tất cả danh mục</option>
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
                        <option value="">Tất cả thương hiệu</option>
                        {options.brands.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>

                    <select
                        className={inputClass}
                        value={draftFilters.stockStatus}
                        onChange={(e) => updateDraft("stockStatus", e.target.value)}
                    >
                        <option value="">Tất cả trạng thái</option>
                        <option value="IN_STOCK">Còn hàng</option>
                        <option value="LOW_STOCK">Sắp hết hàng</option>
                        <option value="OUT_OF_STOCK">Hết hàng</option>
                    </select>
                </div>

                <div className="flex flex-wrap gap-3">
                    <button
                        type="submit"
                        className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700"
                    >
                        Tìm kiếm / Lọc
                    </button>

                    <button
                        type="button"
                        onClick={resetFilters}
                        className="rounded-lg border border-slate-300 px-5 py-2 text-sm text-slate-700"
                    >
                        Đặt lại
                    </button>
                </div>

                {optionsError && (
                    <p role="alert" className="text-sm text-red-600">
                        Không tải được bộ lọc: {optionsError}
                    </p>
                )}
            </form>

            <section className="rounded-xl border border-slate-200 bg-white">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b p-5">
                    <h2 className="font-semibold text-slate-800">
                        Danh sách tồn kho
                    </h2>
                    <span className="text-sm text-slate-500">
                        {formatNumber(report?.totalElements)} dòng dữ liệu
                    </span>
                </div>

                {error && (
                    <p role="alert" className="m-5 rounded-lg bg-red-50 p-3 text-red-700">
                        {error}
                    </p>
                )}

                {loading ? (
                    <p className="p-8 text-center text-slate-500">
                        Đang tải báo cáo tồn kho...
                    </p>
                ) : error ? null : (
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[1300px] text-left text-sm">
                            <thead className="bg-slate-100 text-slate-700">
                                <tr>
                                    <th className="p-3">Kho / Cửa hàng</th>
                                    <th className="p-3">Mã SP</th>
                                    <th className="p-3">Tên sản phẩm</th>
                                    <th className="p-3">Barcode</th>
                                    <th className="p-3">Danh mục / Thương hiệu</th>
                                    <th className="p-3">Vị trí</th>
                                    <th className="p-3 text-right">Thực tế</th>
                                    <th className="p-3 text-right">Đã giữ</th>
                                    <th className="p-3 text-right">Khả dụng</th>
                                    <th className="p-3 text-right">Tồn tối thiểu</th>
                                    <th className="p-3">Trạng thái</th>
                                    <th className="p-3">Cập nhật</th>
                                    <th className="p-3">Chi tiết</th>
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
                                        <td className="p-3">{item.locationCode || "—"}</td>
                                        <td className="p-3 text-right">
                                            {formatNumber(item.onHandQuantity)}
                                        </td>
                                        <td className="p-3 text-right">
                                            {formatNumber(item.reservedQuantity)}
                                        </td>
                                        <td className="p-3 text-right font-semibold">
                                            {formatNumber(item.availableQuantity)}
                                        </td>
                                        <td className="p-3 text-right">
                                            {formatNumber(item.minimumStockLevel)}
                                        </td>
                                        <td className="p-3">
                                            <StockStatusBadge status={item.stockStatus} />
                                        </td>
                                        <td className="p-3">
                                            {formatDate(item.lastUpdatedAt)}
                                        </td>
                                        <td className="p-3">
                                            <button
                                                type="button"
                                                onClick={() => setSelectedProduct({
                                                    productId: item.productId,
                                                    productCode: item.productCode,
                                                    productName: item.productName,
                                                })}
                                                className="font-medium text-blue-600 hover:underline"
                                            >
                                                Xem
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {items.length === 0 && (
                            <p className="p-8 text-center text-slate-500">
                                Không có dữ liệu tồn kho phù hợp.
                            </p>
                        )}
                    </div>
                )}

                <div className="flex items-center justify-between border-t p-4">
                    <span className="text-sm text-slate-500">
                        Trang {page + 1} / {Math.max(totalPages, 1)}
                    </span>

                    <div className="flex gap-2">
                        <button
                            type="button"
                            disabled={loading || page === 0}
                            onClick={() => setPage((current) => Math.max(0, current - 1))}
                            className="rounded border px-4 py-2 text-sm disabled:opacity-40"
                        >
                            Trước
                        </button>

                        <button
                            type="button"
                            disabled={loading || page + 1 >= totalPages}
                            onClick={() => setPage((current) => current + 1)}
                            className="rounded border px-4 py-2 text-sm disabled:opacity-40"
                        >
                            Sau
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
