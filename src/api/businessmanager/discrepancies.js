import api from "../api";

const BASE = "/business-manager/discrepancies";

// Shared JSESSIONID session. The Backend enforces Business Manager permissions.
export async function listBusinessManagerDiscrepancies(filters = {}, signal) {
    const params = {};
    for (const key of ["referenceType", "discrepancyType", "status"]) {
        if (filters[key]) params[key] = filters[key];
    }
    const response = await api.get(BASE, { params, signal });
    return response.data;
}

export async function getBusinessManagerDiscrepancy(id) {
    const response = await api.get(`${BASE}/${id}`);
    return response.data;
}

// This PATCH updates the source document quantities and resolves the discrepancy.
export async function resolveBusinessManagerDiscrepancy(id, payload) {
    const response = await api.patch(`${BASE}/${id}/resolve`, payload);
    return response.data;
}
