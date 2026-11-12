import api from "../api";

const BASE = "/purchase-orders";
const FILTER_FIELDS = ["keyword", "status", "supplierId", "createdFrom", "createdTo"];

function buildParams(filters = {}) {
    const params = {};
    for (const key of FILTER_FIELDS) {
        const raw = filters[key];
        const value = typeof raw === "string" ? raw.trim() : raw;
        if (value !== undefined && value !== null && value !== "") {
            params[key] = value;
        }
    }
    return params;
}

export async function listBusinessManagerPurchaseOrders(filters = {}, signal) {
    const response = await api.get(BASE, { params: buildParams(filters), signal });
    if (!Array.isArray(response.data)) {
        throw new Error("Invalid purchase order list response.");
    }
    return response.data;
}

export async function listPurchaseOrderCancellationApprovals(status = "PENDING", signal) {
    const response = await api.get(`${BASE}/cancellation-requests`, {
        params: status ? { status } : {}, signal,
    });
    return response.data;
}

export async function reviewPurchaseOrderCancellation(requestId, action, note = "") {
    if (!["approve", "reject"].includes(action)) throw new Error("Invalid review decision");
    const response = await api.patch(`${BASE}/cancellation-requests/${encodeURIComponent(requestId)}/${action}`, {
        note: String(note).trim(),
    });
    return response.data;
}

export async function getBusinessManagerPurchaseOrderDetail(id, signal) {
    if (!Number.isSafeInteger(Number(id)) || Number(id) <= 0) {
        throw new Error("Invalid purchase order ID.");
    }
    const response = await api.get(`${BASE}/${id}`, { signal });
    return response.data;
}

