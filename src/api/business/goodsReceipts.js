import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
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

// Danh sach phieu nhap phuc vu Business Staff theo doi (chi doc).
export async function getBusinessGoodsReceipts(filters = {}, signal) {
    const params = { page: filters.page ?? 0, size: filters.size ?? 10 };
    const keyword = String(filters.keyword ?? "").trim();
    if (keyword) params.keyword = keyword;
    for (const key of ["purchaseOrderId", "warehouseId", "status", "fromDate", "toDate"]) {
        if (filters[key] !== "" && filters[key] != null) params[key] = filters[key];
    }
    const response = await api.get("/goods-receipts/business", { params, signal });
    return response.data;
}

export function goodsReceiptErrorMessage(error, fallback = "Unable to complete your request. Please try again.") {
    if (error?.response?.status === 403) {
        return "You do not have permission to perform this action.";
    }
    if (!error?.response) {
        return "Unable to connect to the system. Check your network connection and try again.";
    }
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return friendlyBusinessError(error, fallback);
}
