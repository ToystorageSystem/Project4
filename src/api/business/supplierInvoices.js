import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
import api from "../api";

// Issue #10 - supplier invoices.
// The list endpoint supports only keyword and status filters.
export async function listSupplierInvoices(filters = {}, signal) {
    const response = await api.get("/supplier-invoices", {
        params: {
            keyword: filters.keyword?.trim() || undefined,
            status: filters.status || undefined,
        },
        signal,
    });
    return response.data;
}

export async function getSupplierInvoiceDetail(invoiceId, signal) {
    const response = await api.get(`/supplier-invoices/${encodeURIComponent(invoiceId)}`, { signal });
    return response.data;
}

export async function getSupplierInvoiceSupplierOptions(signal) {
    const response = await api.get("/supplier-invoices/options/suppliers", { signal });
    return response.data;
}

export async function getSupplierInvoicePurchaseOrderOptions(supplierId, signal) {
    const response = await api.get(`/supplier-invoices/options/suppliers/${encodeURIComponent(supplierId)}/purchase-orders`, { signal });
    return response.data;
}

export async function createSupplierInvoice(payload) {
    const response = await api.post("/supplier-invoices", payload);
    return response.data;
}

export async function updateSupplierInvoice(invoiceId, payload) {
    const response = await api.put(`/supplier-invoices/${encodeURIComponent(invoiceId)}`, payload);
    return response.data;
}

export async function cancelSupplierInvoice(invoiceId, reason) {
    const response = await api.patch(`/supplier-invoices/${encodeURIComponent(invoiceId)}/cancel`, { reason });
    return response.data;
}

export async function uploadSupplierInvoiceAttachment(invoiceId, file) {
    const formData = new FormData();
    formData.append("file", file);
    // Do not set Content-Type manually: the browser supplies the multipart boundary.
    const response = await api.post(`/supplier-invoices/${encodeURIComponent(invoiceId)}/attachment`, formData);
    return response.data;
}

export function supplierInvoiceErrorMessage(error, fallback = "Không tải được danh sách hóa đơn nhà cung cấp.") {
    if (error?.response?.status === 403) {
        return "Bạn không có quyền thực hiện thao tác này.";
    }
    if (error?.response?.status === 401) {
        return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    }
    if (!error?.response) {
        return "Không thể kết nối đến hệ thống. Vui lòng kiểm tra mạng và thử lại.";
    }
    const body = error.response.data;
    const message = typeof body === "string" ? body : body?.message;
    return friendlyBusinessError(error, fallback);
}
