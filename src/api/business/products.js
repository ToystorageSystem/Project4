import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import api from "../api";

// All requests use the group's session-cookie Axios client. Mutations are
// only sent after an explicit user action; this file never seeds the database.
export async function listProducts(
    { status = "", keyword = "", categoryId = "", page = 0, size = 10 } = {},
    signal
) {
    const response = await api.get("/products", {
        params: {
            status: status || undefined,
            keyword: keyword.trim() || undefined,
            categoryId: categoryId || undefined,
            page,
            size,
        },
        signal,
    });
    return response.data;
}

export async function getProduct(productId, signal) {
    const response = await api.get(`/products/${productId}`, { signal });
    return response.data;
}

export async function createProduct(payload) {
    const response = await api.post("/products", payload);
    return response.data;
}

// Tao Product va gan mot nha cung cap trong cung mot transaction o Backend.
export async function createProductWithSupplier(payload) {
    const response = await api.post("/products/with-supplier", payload);
    return response.data;
}

// Cập nhật trực tiếp thông tin sản phẩm. API BE sẽ ghi lịch sử vào activity_logs.
export async function updateProduct(productId, payload) {
    const response = await api.put(`/products/${productId}`, payload);
    return response.data;
}

export async function getProductHistory(productId, { page = 0, size = 10 } = {}, signal) {
    const response = await api.get(`/products/${productId}/history`, {
        params: { page, size }, signal,
    });
    return response.data;
}

export async function changeProductVisibility(productId, active) {
    const action = active ? "activate" : "deactivate";
    const response = await api.patch(`/products/${productId}/${action}`);
    return response.data;
}

export async function getCategoryOptions(signal) {
    const response = await api.get("/categories/options", { signal });
    return response.data;
}

export async function uploadProductImage(file) {
    const data = new FormData();
    data.append("image", file);
    const response = await api.post("/products/images", data);
    return response.data;
}

// Easy one-sheet import with Supplier Name (existing or new).
export async function previewProductExcelSimple(file) {
    const data = new FormData();
    data.append("file", file);
    const response = await api.post("/products/import-simple/preview", data);
    return response.data;
}

export async function importProductExcelSimple(file) {
    const data = new FormData();
    data.append("file", file);
    const response = await api.post("/products/import-simple", data);
    return response.data;
}

export async function previewProductExcel(file) {
    const data = new FormData();
    data.append("file", file);
    const response = await api.post("/products/import/preview", data);
    return response.data;
}

export async function importProductExcel(file) {
    const data = new FormData();
    data.append("file", file);
    const response = await api.post("/products/import", data);
    return response.data;
}

export async function getMyProductRequests({ page = 0, size = 10 } = {}, signal) {
    const response = await api.get("/product-approvals/my-requests", {
        params: { page, size }, signal,
    });
    return response.data;
}

export function productErrorMessage(error, fallback = "Unable to complete the action. Please try again.") {
    if (error?.response?.status === 403) return "You do not have permission to perform this action.";
    if (!error?.response) return "Unable to connect to the system. Check your network connection and try again.";
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return friendlyBusinessError(error, fallback);
}

