
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
    fallback = "Không tải được dữ liệu vận chuyển."
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
        return "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    }

    if (status === 403) {
        return "Tài khoản không có quyền xem hàng đang vận chuyển.";
    }

    if (status === 404) {
        return "Không tìm thấy chuyến vận chuyển.";
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
