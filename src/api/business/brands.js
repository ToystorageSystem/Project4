import { friendlyBusinessError } from "../../utils/friendlyBusinessError";

import api from "../api";

// Brand Management

// GET /api/brands
// Danh sach, tim kiem, loc va phan trang
export async function listBrands(
    { keyword = "", status = "", page = 0, size = 10 } = {},
    signal
) {
    const response = await api.get("/brands", {
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

// GET /api/brands/{id}
// Xem chi tiet thuong hieu
export async function getBrand(brandId, signal) {
    const response = await api.get(
        `/brands/${encodeURIComponent(brandId)}`,
        { signal }
    );

    return response.data;
}

// GET /api/brands/options
// Chi lay thuong hieu ACTIVE
export async function getActiveBrands(signal) {
    const response = await api.get(
        "/brands/options",
        { signal }
    );

    return response.data;
}

// POST /api/brands
// Tao thuong hieu moi
export async function createBrand(payload) {
    const response = await api.post(
        "/brands",
        payload
    );

    return response.data;
}

// PUT /api/brands/{id}
// Cap nhat thong tin thuong hieu
export async function updateBrand(brandId, payload) {
    const response = await api.put(
        `/brands/${encodeURIComponent(brandId)}`,
        payload
    );

    return response.data;
}

// PATCH /api/brands/{id}/deactivate
// An thuong hieu
export async function deactivateBrand(brandId) {
    const response = await api.patch(
        `/brands/${encodeURIComponent(brandId)}/deactivate`
    );

    return response.data;
}

// PATCH /api/brands/{id}/activate
// Khoi phuc thuong hieu
export async function activateBrand(brandId) {
    const response = await api.patch(
        `/brands/${encodeURIComponent(brandId)}/activate`
    );

    return response.data;
}

// Chuyen loi Backend thanh thong bao cho Frontend
export function brandErrorMessage(
    error,
    fallback = "Unable to complete the brand operation."
) {
    if (error?.response?.status === 401) {
        return "Your session is invalid. Please sign in again.";
    }

    if (error?.response?.status === 403) {
        return "You do not have permission to manage brands.";
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

