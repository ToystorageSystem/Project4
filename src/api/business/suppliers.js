import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import api from "../api";

// This API uses the shared Axios client and the existing JSESSIONID cookie.
// The Backend owns validation, authorization, audit and status transitions.
export async function listSuppliers(params, signal) {
    const response = await api.get("/suppliers", { params, signal });
    return response.data;
}

export async function getSupplier(id, signal) {
    const response = await api.get(`/suppliers/${id}`, { signal });
    return response.data;
}

export async function getSupplierProducts(id, signal) {
    const response = await api.get(`/business-staff/suppliers/${id}/products`, { signal });
    return response.data;
}

export async function createSupplier(payload) {
    const response = await api.post("/suppliers", payload);
    return response.data;
}

export async function updateSupplier(id, payload) {
    const response = await api.put(`/suppliers/${id}`, payload);
    return response.data;
}

export async function changeSupplierStatus(id, active) {
    const operation = active ? "activate" : "deactivate";
    const response = await api.patch(`/suppliers/${id}/${operation}`);
    return response.data;
}

export function supplierErrorMessage(error, fallback = "Đã xảy ra lỗi. Vui lòng thử lại.") {
    if (error?.response?.status === 403) {
        return "Bạn không có quyền thực hiện thao tác này.";
    }
    if (!error?.response) {
        return "Không thể kết nối đến hệ thống. Vui lòng kiểm tra mạng và thử lại.";
    }
    const message = error.response.data?.message;
    return friendlyBusinessError(error, fallback);
}
