import { friendlyBusinessError } from "../../utils/friendlyBusinessError";

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
    fallback = "Unable to complete the supplier operation."
) {
    if (
        error?.code === "ERR_CANCELED" ||
        error?.name === "CanceledError"
    ) {
        return "";
    }

    const status = error?.response?.status;

    if (status === 401) {
        return "Your session has expired. Please sign in again.";
    }

    if (status === 403) {
        return "Your account does not have permission to use this feature.";
    }

    const data = error?.response?.data;

    const message =
        typeof data === "string"
            ? data
            : data?.message;

    if (status === 404) {
        return friendlyBusinessError(error, "Supplier not found.");
    }

    if (!error?.response) {
        return "Unable to connect to the system. Check your network connection and try again.";
    }

    return friendlyBusinessError(error, fallback);
}