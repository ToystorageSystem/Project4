import api from "../api";

// All endpoints use the shared JSESSIONID-based Axios client.
export async function listPendingProductApprovals({ page = 0, size = 10 } = {}, signal) {
    const response = await api.get("/product-approvals", {
        params: { page, size },
        signal,
    });
    return response.data;
}

export async function getProductApprovalRequest(requestId, signal) {
    const response = await api.get(`/product-approvals/${requestId}`, { signal });
    return response.data;
}

export async function approveProductApprovalRequest(requestId) {
    const response = await api.patch(`/product-approvals/${requestId}/approve`);
    return response.data;
}

export async function rejectProductApprovalRequest(requestId, reason) {
    const response = await api.patch(`/product-approvals/${requestId}/reject`, {
        reason: reason.trim(),
    });
    return response.data;
}
