import api from "../api";
import { friendlyBusinessError } from "../../utils/friendlyBusinessError";

// Issue #9: read APIs and explicit create/update actions for DRAFT orders.
export async function listPurchaseOrders(filters = {}, signal) {
    const response = await api.get("/purchase-orders", {
        params: {
            keyword: filters.keyword || undefined,
            status: filters.status || undefined,
            supplierId: filters.supplierId || undefined,
            createdFrom: filters.createdFrom || undefined,
            createdTo: filters.createdTo || undefined,
        },
        signal,
    });
    return response.data;
}

export async function getPurchaseOrderDetail(orderId, signal) {
    const response = await api.get(`/purchase-orders/${encodeURIComponent(orderId)}`, { signal });
    return response.data;
}

export async function getPurchaseOrderSuppliers(signal) {
    const response = await api.get("/purchase-orders/options/suppliers", { signal });
    return response.data;
}

export async function getPurchaseOrderWarehouses(signal) {
    const response = await api.get("/purchase-orders/options/main-warehouses", { signal });
    return response.data;
}

export async function getPurchaseOrderProductsBySupplier(supplierId, signal) {
    const response = await api.get(`/purchase-orders/options/suppliers/${encodeURIComponent(supplierId)}/products`, { signal });
    return response.data;
}

export async function createPurchaseOrder(payload) {
    const response = await api.post("/purchase-orders", payload);
    return response.data;
}

export async function updatePurchaseOrder(orderId, payload) {
    const response = await api.put(`/purchase-orders/${encodeURIComponent(orderId)}`, payload);
    return response.data;
}

export async function submitPurchaseOrder(orderId) {
    const response = await api.patch(`/purchase-orders/${encodeURIComponent(orderId)}/submit`);
    return response.data;
}

export async function cancelPurchaseOrder(orderId, reason, supplierAgreed = false) {
    // Business Operations: huy truc tiep, backend kiem tra GR/hoa don.
    const response = await api.patch(`/purchase-orders/${encodeURIComponent(orderId)}/cancel`, {
        reason: String(reason).trim(),
        supplierAgreed: Boolean(supplierAgreed),
    });
    return response.data;
}

export async function getPurchaseOrderCancellationHistory(orderId, signal) {
    // Lấy nhật ký hủy từ activity_logs: chỉ danh tính và thời gian.
    const response = await api.get(`/purchase-orders/${encodeURIComponent(orderId)}/cancellation-history`, { signal });
    return response.data;
}

export async function listPurchaseOrderCancellationRequests(orderId, signal) {
    const response = await api.get(`/purchase-orders/${encodeURIComponent(orderId)}/cancellation-requests`, { signal });
    return response.data;
}

export function purchaseOrderErrorMessage(error, fallback = "Unable to load purchase orders.") {
    if (error?.response?.status === 403) {
        return "Your account does not have permission to perform this action.";
    }
    if (!error?.response) {
        return "Unable to connect to the system. Check your connection and try again.";
    }
    return friendlyBusinessError(error, fallback);
}

