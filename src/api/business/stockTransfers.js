import api from "../api";

// Issue #11 - creation, detail, and explicit, confirmed stock transfer state changes.
export async function getStockTransferSources(signal) {
    const response = await api.get("/stock-transfers/creation-options/sources", { signal });
    return response.data;
}

export async function getStockTransferDestinations(signal) {
    const response = await api.get("/stock-transfers/creation-options/destinations", { signal });
    return response.data;
}

export async function getStockTransferSourceProducts(sourceId, signal) {
    const response = await api.get(
        `/stock-transfers/creation-options/sources/${encodeURIComponent(sourceId)}/products`,
        { signal },
    );
    return response.data;
}

// POST writes a DRAFT record to the shared database; it does not submit/reserve stock.
export async function createStockTransferDraft(payload) {
    const response = await api.post("/stock-transfers", payload);
    return response.data;
}

export async function getStockTransferDetail(transferId, signal) {
    const response = await api.get(`/stock-transfers/${encodeURIComponent(transferId)}`, { signal });
    return response.data;
}

// PUT modifies an existing DRAFT transfer; the source warehouse is immutable.
export async function updateStockTransferDraft(transferId, payload) {
    const response = await api.put(`/stock-transfers/${encodeURIComponent(transferId)}`, payload);
    return response.data;
}

// PATCH DRAFT -> CREATED. Backend reserves source inventory inside a transaction.
export async function submitStockTransfer(transferId) {
    const response = await api.patch(`/stock-transfers/${encodeURIComponent(transferId)}/submit`);
    return response.data;
}

// PATCH allowed statuses -> CANCELLED with a required reason.
export async function cancelStockTransfer(transferId, reason) {
    const response = await api.patch(`/stock-transfers/${encodeURIComponent(transferId)}/cancel`, { reason });
    return response.data;
}

export function stockTransferErrorMessage(error, fallback = "Không tải được dữ liệu điều chuyển hàng.") {
    if (error?.response?.status === 401) return "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.";
    if (error?.response?.status === 403) return "Tài khoản không có quyền truy cập chức năng điều chuyển hàng (403).";
    if (!error?.response) {
        if (!error?.isAxiosError && error?.message) return error.message;
        return "Không kết nối được Backend. Hãy kiểm tra Spring Boot đang chạy.";
    }
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return typeof message === "string" && message.trim() ? message : fallback;
}
