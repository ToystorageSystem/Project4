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

export function supplierProductErrorMessage(error, fallback = "Không thực hiện được thao tác.") {
    if (error?.response?.status === 401) return "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.";
    if (error?.response?.status === 403) return "Tài khoản không có quyền thao tác liên kết sản phẩm - nhà cung cấp (403).";
    if (!error?.response) return "Không kết nối được Backend. Vui lòng kiểm tra server.";
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return typeof message === "string" && message.trim() ? message : fallback;
}
