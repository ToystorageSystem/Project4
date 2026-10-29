
import api from "../api";

// Issue #14 - Shipment Manifest (Business Staff)
// Chuc nang chi doc du lieu, khong thay doi database.

// Tao tham so tim kiem, loc va phan trang.
function buildShipmentManifestParams(filters = {}) {
    const params = {};

    const keyword = filters.keyword?.trim();

    if (keyword) {
        params.keyword = keyword;
    }

    if (
        filters.fromWarehouseId != null &&
        filters.fromWarehouseId !== ""
    ) {
        params.fromWarehouseId = filters.fromWarehouseId;
    }

    if (
        filters.toWarehouseId != null &&
        filters.toWarehouseId !== ""
    ) {
        params.toWarehouseId = filters.toWarehouseId;
    }

    if (
        filters.transportStatus &&
        filters.transportStatus !== "ALL"
    ) {
        params.transportStatus = filters.transportStatus;
    }

    if (filters.fromDate) {
        params.fromDate = filters.fromDate;
    }

    if (filters.toDate) {
        params.toDate = filters.toDate;
    }

    params.page = filters.page ?? 0;
    params.size = filters.size ?? 10;

    return params;
}

// 1. Lay danh sach bang ke di hang.
// GET /api/shipment-manifests
export async function getShipmentManifests(
    filters = {},
    signal
) {
    const response = await api.get(
        "/shipment-manifests",
        {
            params: buildShipmentManifestParams(filters),
            signal,
        }
    );

    return response.data;
}

// 2. Lay chi tiet bang ke.
// GET /api/shipment-manifests/{manifestId}
export async function getShipmentManifestDetail(
    manifestId,
    signal
) {
    const response = await api.get(
        `/shipment-manifests/${encodeURIComponent(manifestId)}`,
        { signal }
    );

    return response.data;
}

// 3. Xu ly thong bao loi.
export function shipmentManifestErrorMessage(
    error,
    fallback = "Khong tai duoc du lieu bang ke di hang."
) {
    if (error?.code === "ERR_CANCELED") {
        return "";
    }

    if (error?.response?.status === 401) {
        return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    }

    if (error?.response?.status === 403) {
        return "Tài khoản không có quyền xem bảng kê đi hàng.";
    }

    if (error?.response?.status === 404) {
        return "Không tìm thấy bảng kê đi hàng.";
    }

    if (!error?.response) {
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
