import api from "../api";

// These GET requests use the existing session-cookie client (baseURL: /api).
export async function getGoodsReceiptSupplierOptions(signal) {
    const response = await api.get("/goods-receipts/creation-options/suppliers", { signal });
    return response.data;
}

export async function getGoodsReceiptMainWarehouseOptions(signal) {
    const response = await api.get("/goods-receipts/creation-options/main-warehouses", { signal });
    return response.data;
}

export async function getActiveProductsForGoodsReceipt(page = 0, signal) {
    const response = await api.get("/products", {
        params: { status: "ACTIVE", page, size: 100 },
        signal,
    });
    return response.data;
}

// The only mutation in this module. Never invoke on mount or as a side effect.
export async function createMainWarehouseGoodsReceipt(payload) {
    const response = await api.post("/goods-receipts", payload);
    return response.data;
}

export async function getGoodsReceiptById(receiptId, signal) {
    const response = await api.get(`/goods-receipts/${encodeURIComponent(receiptId)}`, { signal });
    return response.data;
}

export function goodsReceiptErrorMessage(error, fallback = "Không thể thực hiện yêu cầu này.") {
    if (error?.response?.status === 403) {
        return "Tài khoản không có quyền thực hiện thao tác này (403).";
    }
    if (!error?.response) {
        return "Không kết nối được Backend. Kiểm tra Spring Boot đang chạy.";
    }
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return typeof message === "string" && message.trim() ? message : fallback;
}
