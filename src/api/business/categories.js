import { friendlyBusinessError } from "../../utils/friendlyBusinessError";

import api from "../api";

// Issue #6 - Category Management

// GET /api/categories
// Danh sach, tim kiem, loc va phan trang
export async function listCategories(
    { keyword = "", status = "", page = 0, size = 10 } = {},
    signal
) {
    const response = await api.get("/categories", {
        params: {
            keyword: keyword.trim() || undefined,
            status: status || undefined,
            page,
            size,
        },
        signal,
    });

    return response.data;
}

// GET /api/categories/{id}
// Xem chi tiet danh muc
export async function getCategory(categoryId, signal) {
    const response = await api.get(
        `/categories/${encodeURIComponent(categoryId)}`,
        { signal }
    );

    return response.data;
}

// GET /api/categories/options
// Chi lay danh muc ACTIVE
export async function getActiveCategories(signal) {
    const response = await api.get(
        "/categories/options",
        { signal }
    );

    return response.data;
}

// POST /api/categories
// Tao danh muc moi
export async function createCategory(payload) {
    const response = await api.post(
        "/categories",
        payload
    );

    return response.data;
}

// PUT /api/categories/{id}
// Cap nhat thong tin danh muc
export async function updateCategory(categoryId, payload) {
    const response = await api.put(
        `/categories/${encodeURIComponent(categoryId)}`,
        payload
    );

    return response.data;
}

// PATCH /api/categories/{id}/deactivate
// An danh muc
export async function deactivateCategory(categoryId) {
    const response = await api.patch(
        `/categories/${encodeURIComponent(categoryId)}/deactivate`
    );

    return response.data;
}

// PATCH /api/categories/{id}/activate
// Khoi phuc danh muc
export async function activateCategory(categoryId) {
    const response = await api.patch(
        `/categories/${encodeURIComponent(categoryId)}/activate`
    );

    return response.data;
}

// Chuyen loi Backend thanh thong bao cho Frontend
export function categoryErrorMessage(
    error,
    fallback = "Unable to complete the category operation."
) {
    if (error?.response?.status === 401) {
        return "Your session is invalid. Please sign in again.";
    }

    if (error?.response?.status === 403) {
        return "You do not have permission to manage categories.";
    }

    if (!error?.response) {
        return "Unable to connect to the system. Check your network connection and try again.";
    }

    const data = error.response.data;

    const message =
        typeof data === "string"
            ? data
            : data?.message;

    return friendlyBusinessError(error, fallback);
}
