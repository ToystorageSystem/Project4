import api from "../api";

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

export async function cancelPurchaseOrder(orderId, reason) {
    const response = await api.patch(`/purchase-orders/${encodeURIComponent(orderId)}/cancel`, {
        reason: String(reason).trim(),
    });
    return response.data;
}

export function purchaseOrderErrorMessage(error, fallback = "Không tải được dữ liệu đơn mua hàng.") {
    if (error?.response?.status === 403) {
        return "Tài khoản không có quyền thực hiện thao tác với đơn mua hàng (403).";
    }
    if (!error?.response) {
        return "Không kết nối được Backend. Hãy kiểm tra Spring Boot đang chạy.";
    }
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return typeof message === "string" && message.trim() ? message : fallback;
}
