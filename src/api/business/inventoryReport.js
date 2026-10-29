
import api from "../api";

// Issue #15 - Inventory Report (Business Staff)
// All endpoints are read-only.

// Build query parameters for filtering and pagination.
function buildInventoryParams(filters = {}, includePagination = true) {
    const params = {};

    const keyword = filters.keyword?.trim();
    if (keyword) {
        params.keyword = keyword;
    }

    if (filters.warehouseId != null && filters.warehouseId !== "") {
        params.warehouseId = filters.warehouseId;
    }

    if (filters.categoryId != null && filters.categoryId !== "") {
        params.categoryId = filters.categoryId;
    }

    if (filters.brandId != null && filters.brandId !== "") {
        params.brandId = filters.brandId;
    }

    if (
        filters.stockStatus &&
        filters.stockStatus !== "ALL"
    ) {
        params.stockStatus = filters.stockStatus;
    }

    if (includePagination) {
        params.page = filters.page ?? 0;
        params.size = filters.size ?? 10;
    }

    return params;
}

// 1. Get paginated inventory report.
// GET /api/inventory-report
export async function getInventoryReport(filters = {}, signal) {
    const response = await api.get("/inventory-report", {
        params: buildInventoryParams(filters),
        signal,
    });

    return response.data;
}

// 2. Get inventory locations for a product.
// GET /api/inventory-report/products/{productId}/locations
export async function getInventoryProductLocations(productId, signal) {
    const response = await api.get(
        `/inventory-report/products/${encodeURIComponent(productId)}/locations`,
        { signal }
    );

    return response.data;
}

// 3. Get warehouse and store options.
// GET /api/inventory-report/options/warehouses
export async function getInventoryWarehouseOptions(signal) {
    const response = await api.get(
        "/inventory-report/options/warehouses",
        { signal }
    );

    return response.data;
}

// 4. Get category options.
// GET /api/inventory-report/options/categories
export async function getInventoryCategoryOptions(signal) {
    const response = await api.get(
        "/inventory-report/options/categories",
        { signal }
    );

    return response.data;
}

// 5. Get brand options.
// GET /api/inventory-report/options/brands
export async function getInventoryBrandOptions(signal) {
    const response = await api.get(
        "/inventory-report/options/brands",
        { signal }
    );

    return response.data;
}

// 6. Export inventory report to Excel.
// GET /api/inventory-report/export
export async function exportInventoryReport(filters = {}) {
    const response = await api.get("/inventory-report/export", {
        params: buildInventoryParams(filters, false),
        responseType: "blob",
    });

    const disposition =
        response.headers?.["content-disposition"] || "";

    const match = disposition.match(
        /filename\s*=\s*"?([^";]+)"?/i
    );

    const filename = match?.[1] ||
        `inventory-report-${new Date().toISOString().slice(0, 10)}.xlsx`;

    return {
        blob: response.data,
        filename,
    };
}

// Shared error message handler.
export function inventoryReportErrorMessage(
    error,
    fallback = "Không tải được báo cáo tồn kho."
) {
    if (error?.response?.status === 401) {
        return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    }

    if (error?.response?.status === 403) {
        return "Tài khoản không có quyền xem báo cáo tồn kho.";
    }

    if (!error?.response) {
        if (error?.code === "ERR_CANCELED") {
            return "";
        }

        return "Không kết nối được Backend. Hãy kiểm tra Spring Boot.";
    }

    const data = error.response.data;

    const message =
        typeof data === "string"
            ? data
            : data?.message;

    return typeof message === "string" && message.trim()
        ? message
        : fallback;
}
