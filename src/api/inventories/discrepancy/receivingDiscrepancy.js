import api from "../../api";

const BASE_URL = "/receiving-discrepancies";

export const getReceivingDiscrepancies = async () => (await api.get(BASE_URL)).data;
export const getReceivingDiscrepancy = async (id) => (await api.get(`${BASE_URL}/${id}`)).data;
export const startHandlingDiscrepancy = async (id) => (await api.patch(`${BASE_URL}/${id}/start`)).data;
export const acceptActualQuantity = async (id, resolutionNote) =>
    (await api.patch(`${BASE_URL}/${id}/accept-actual`, { resolutionNote })).data;
export const updateAcceptedQuantity = async (reportId, productId, acceptedQuantity, reason) =>
    (await api.patch(`${BASE_URL}/${reportId}/items/${productId}/accepted-quantity`, { acceptedQuantity, reason })).data;
export const requestDiscrepancyRecount = async (id, resolutionNote) =>
    (await api.patch(`${BASE_URL}/${id}/recount`, { resolutionNote })).data;
export const escalateDiscrepancy = async (id, reason) =>
    (await api.patch(`${BASE_URL}/${id}/escalate`, { reason })).data;
