import api from "../api";

const BASE_URL = "/stock-transfers";

export const getShipment = async (transferId) =>
    (await api.get(`${BASE_URL}/${transferId}/shipment`)).data;

export const confirmShipment = async (transferId, data) =>
    (await api.patch(`${BASE_URL}/${transferId}/confirm-shipment`, data)).data;
