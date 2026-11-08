import { friendlyBusinessError } from "../../utils/friendlyBusinessError";

import api from "../api";

// Issue #13 - In-Transit Shipment Tracking (Business Staff)
// Read-only API. Khong thay doi du lieu van chuyen.

// 1. Tao tham so tim kiem, loc va phan trang.
function buildInTransitShipmentParams(filters = {}) {
    const params = {};

    // Tim kiem theo ma chuyen giao, bang ke,
    // phieu dieu chuyen hoac kien hang.
    const keyword = filters.keyword?.trim();

    if (keyword) {
        params.keyword = keyword;
    }

    // Loc theo kho / cua hang xuat.
    if (
        filters.fromWarehouseId != null &&
        filters.fromWarehouseId !== ""
    ) {
        params.fromWarehouseId = filters.fromWarehouseId;
    }

    // Loc theo kho / cua hang nhan.
    if (
        filters.toWarehouseId != null &&
        filters.toWarehouseId !== ""
    ) {
        params.toWarehouseId = filters.toWarehouseId;
    }

    // Backend Issue #13 su dung "status".
    if (
        filters.status &&
        filters.status !== "ALL"
    ) {
        params.status = filters.status;
    }

    // Phan trang.
    params.page = filters.page ?? 0;
    params.size = filters.size ?? 10;

    return params;
}

// 2. Lay danh sach chuyen van chuyen.
// GET /api/in-transit-shipments
export async function getInTransitShipments(
    filters = {},
    signal
) {
    const response = await api.get(
        "/in-transit-shipments",
        {
            params: buildInTransitShipmentParams(filters),
            signal,
        }
    );

    return response.data;
}

// 3. Lay chi tiet mot chuyen van chuyen.
// GET /api/in-transit-shipments/{deliveryId}
export async function getInTransitShipmentDetail(
    deliveryId,
    signal
) {
    const response = await api.get(
        `/in-transit-shipments/${encodeURIComponent(deliveryId)}`,
        { signal }
    );

    return response.data;
}

// 4. Xu ly thong bao loi API.
export function inTransitShipmentErrorMessage(
    error,
    fallback = "Unable to load shipment data."
) {
    // Request bi huy khi chuyen trang / unmount.
    if (
        error?.code === "ERR_CANCELED" ||
        error?.name === "CanceledError"
    ) {
        return "";
    }

    const status = error?.response?.status;

    if (status === 401) {
        return "You are not signed in, or your session has expired. Please sign in again.";
    }

    if (status === 403) {
        return "You do not have permission to view in-transit shipments.";
    }

    if (status === 404) {
        return "Shipment not found.";
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
