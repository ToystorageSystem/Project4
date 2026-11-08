import { friendlyBusinessError } from "../../utils/friendlyBusinessError";
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
/**
 * Lấy danh sách phiếu điều chuyển do người đang đăng nhập tạo.
 * Backend chịu trách nhiệm lọc theo người tạo và phân quyền.
 */
export async function getMyStockTransfers(filters = {}, signal) {
    const response = await api.get("/stock-transfers/mine", {
        params: {
            keyword: filters.keyword || undefined,
            status: filters.status || undefined,
            page: filters.page ?? 0,
            size: filters.size ?? 10,
        },
        signal,
    });

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

// PUT modifies DRAFT / CREATED / PENDING_SOURCE_CONFIRMATION, subject to Backend
// ownership/status rules. Source is immutable; destination changes only in DRAFT.
// For an existing reservation, Backend recalculates reserved stock transactionally.
export async function updateStockTransfer(transferId, payload) {
    const response = await api.put(`/stock-transfers/${encodeURIComponent(transferId)}`, payload);
    return response.data;
}

// Keep the Issue #11 export for any older consumers.
export const updateStockTransferDraft = updateStockTransfer;

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

export function stockTransferErrorMessage(error, fallback = "Unable to load stock transfer data.") {
    if (error?.response?.status === 401) return "Your session has expired. Please sign in again.";
    if (error?.response?.status === 403) return "You do not have permission to manage stock transfers.";
    if (!error?.response) {
        if (!error?.isAxiosError && error?.message) return error.message;
        return "Unable to connect to the system. Check your network connection and try again.";
    }
    const data = error.response.data;
    const message = typeof data === "string" ? data : data?.message;
    return friendlyBusinessError(error, fallback);
}

