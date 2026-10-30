
import api from "../api";

// Issue #16 - Lam viec voi nha cung cap.
// Feedback, price comparison and price decision.

const BASE_URL = "/business-staff/purchase-orders";

// 1. Xem bang so sanh gia cua Purchase Order.
// GET /api/business-staff/purchase-orders/{id}/price-comparison
export async function getSupplierPriceComparison(
    purchaseOrderId,
    signal
) {
    const response = await api.get(
        `${BASE_URL}/${encodeURIComponent(purchaseOrderId)}/price-comparison`,
        { signal }
    );

    return response.data;
}


/**
 * Issue #16 - Doc lai phan hoi nha cung cap da luu.
 * Method: GET
 * Khong thay doi du lieu database.
 */
export async function getSupplierFeedback(
    purchaseOrderId,
    signal
) {
    const response = await api.get(
        `${BASE_URL}/${encodeURIComponent(purchaseOrderId)}/supplier-feedback`,
        { signal }
    );

    return response.data;
}

// 2. Ghi nhan phan hoi nha cung cap.
// PUT /api/business-staff/purchase-orders/{id}/supplier-feedback
//
// Payload:
// {
//     supplierResponseStatus: "...",
//     supplierResponseNote: "...",
//     items: [...]
// }
export async function recordSupplierFeedback(
    purchaseOrderId,
    payload
) {
    const response = await api.put(
        `${BASE_URL}/${encodeURIComponent(purchaseOrderId)}/supplier-feedback`,
        payload
    );

    return response.data;
}

// 3. Chap nhan hoac tu choi gia moi theo tung PO item.
// POST /api/business-staff/purchase-orders/{id}/items/{itemId}/price-decision
//
// Payload:
// {
//     decision: "ACCEPT" | "REJECT",
//     reason: "..."
// }
export async function decideSupplierPrice(
    purchaseOrderId,
    purchaseOrderItemId,
    payload
) {
    const response = await api.post(
        `${BASE_URL}/${encodeURIComponent(purchaseOrderId)}/items/${encodeURIComponent(purchaseOrderItemId)}/price-decision`,
        payload
    );

    return response.data;
}

// 4. Chuyen loi Backend thanh thong bao cho giao dien.
export function supplierFeedbackErrorMessage(
    error,
    fallback = "Không thực hiện được thao tác với nhà cung cấp."
) {
    if (
        error?.code === "ERR_CANCELED" ||
        error?.name === "CanceledError"
    ) {
        return "";
    }

    const status = error?.response?.status;

    if (status === 401) {
        return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    }

    if (status === 403) {
        return "Tài khoản không có quyền thực hiện chức năng này.";
    }

    const data = error?.response?.data;

    const message =
        typeof data === "string"
            ? data
            : data?.message;

    if (status === 404) {
        return message || "Không tìm thấy dữ liệu yêu cầu.";
    }

    if (!error?.response) {
        return "Không kết nối được Backend. Hãy kiểm tra Spring Boot.";
    }

    return typeof message === "string" && message.trim()
        ? message
        : fallback;
}