import api from "../api";

const BASE = "/business-manager/reports";
const TYPES = new Set(["imports", "exports", "transfers", "inventory"]);

function endpoint(type, excel = false) {
    if (!TYPES.has(type)) throw new Error("Loại báo cáo không hợp lệ.");
    return `${BASE}/${type}${excel ? "/excel" : ""}`;
}

function paramsFor(type, filters = {}) {
    const params = {};
    if (filters.warehouseId !== "" && filters.warehouseId != null) {
        params.warehouseId = filters.warehouseId;
    }
    // Inventory is a live snapshot: its API does not accept date filters.
    if (type !== "inventory") {
        if (filters.fromDate) params.fromDate = filters.fromDate;
        if (filters.toDate) params.toDate = filters.toDate;
    }
    return params;
}

export async function getBusinessActivityReport(type, filters = {}, signal) {
    const response = await api.get(endpoint(type), {
        params: paramsFor(type, filters),
        signal,
    });
    return response.data;
}

export async function getBusinessActivityExcel(type, filters = {}) {
    return api.get(endpoint(type, true), {
        params: paramsFor(type, filters),
        responseType: "blob",
    });
}
