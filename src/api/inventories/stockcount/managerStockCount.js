import api from "../../api";

const BASE_URL = "/stock-counts";

export const getStockCount = async (stockCountId) =>
    (await api.get(`${BASE_URL}/${stockCountId}`)).data;

export const startStockCount = async (stockCountId) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/start`)).data;

export const countStockCountItem = async (stockCountId, itemId, data) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/items/${itemId}/count`, data)).data;

export const finishStockCount = async (stockCountId) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/finish`)).data;

export const confirmStockCount = async (stockCountId) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/confirm`)).data;
