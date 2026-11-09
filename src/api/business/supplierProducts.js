import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import api from "../api";

// Issue #8: Supplier <-> Product relations. Use the team's session Axios client.
// Only explicit user actions may call the write endpoints.
export async function getLinksByProduct(productId, signal) {
    const response = await api.get(`/products/${productId}/suppliers`, { signal });
    return response.data;
}

export async function getLinksBySupplier(supplierId, signal) {
    const response = await api.get(`/suppliers/${supplierId}/products`, { signal });
    return response.data;
}

export async function createSupplierProductLink(payload) {
    const response = await api.post("/supplier-products", payload);
    return response.data;
}

export async function updateSupplierProductLink(linkId, payload) {
    const response = await api.put(`/supplier-products/${linkId}`, payload);
    return response.data;
}

export async function setDefaultSupplierLink(linkId) {
    const response = await api.patch(`/supplier-products/${linkId}/default`);
    return response.data;
}

// Backend DELETE is permanent and does not currently write activity_logs.
export async function deleteSupplierProductLink(linkId) {
    await api.delete(`/supplier-products/${linkId}`);
}

export function supplierProductErrorMessage(error, fallback = "Unable to complete the action.") {
    if (error?.response?.status === 401) return "Your session has expired. Please sign in again.";
    if (error?.response?.status === 403) return "You do not have permission to change a product's supplier.";
    if (!error?.response) return "Unable to connect to the system. Check your network connection and try again.";
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return friendlyBusinessError(error, fallback);
}

