import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import api from "../api";

// All requests use the group's session-cookie Axios client. Mutations are
// only sent after an explicit user action; this file never seeds the database.
export async function listProducts({ status = "", page = 0, size = 10 } = {}, signal) {
    const response = await api.get("/products", {
        params: { status: status || undefined, page, size }, signal,
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

// This submits a CHANGE REQUEST; existing product values remain unchanged
// until a Business Manager approves the request.
export async function requestProductUpdate(productId, payload) {
    const response = await api.post(`/products/${productId}/change-requests`, payload);
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

export function productErrorMessage(error, fallback = "Không thực hiện được thao tác. Vui lòng thử lại.") {
    if (error?.response?.status === 403) return "Bạn chưa được cấp quyền thực hiện thao tác này.";
    if (!error?.response) return "Không thể kết nối hệ thống. Vui lòng thử lại.";
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return friendlyBusinessError(error, fallback);
}
