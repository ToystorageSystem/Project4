import api from "../../api";

const BASE_URL = "/warehouse/picking";

export const getPickingTransfers = async () =>
    (await api.get(`${BASE_URL}/transfers`)).data;

export const getPickingTransfer = async (transferId) =>
    (await api.get(`${BASE_URL}/transfers/${transferId}`)).data;

export const startPicking = async (transferId) =>
    (await api.patch(`${BASE_URL}/transfers/${transferId}/start`)).data;

export const pickTransferItem = async (transferId, itemId, data) =>
    (await api.patch(`${BASE_URL}/transfers/${transferId}/items/${itemId}`, data)).data;

export const completePicking = async (transferId) =>
    (await api.patch(`${BASE_URL}/transfers/${transferId}/complete`)).data;
