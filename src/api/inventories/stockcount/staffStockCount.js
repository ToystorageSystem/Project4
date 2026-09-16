import api from "../../api";

const BASE_URL = "/warehouse/staff/stock-counts";

export const getStaffStockCountTasks = async () =>
    (await api.get(BASE_URL)).data;

export const getStaffStockCountDetail = async (stockCountId) =>
    (await api.get(`${BASE_URL}/${stockCountId}`)).data;

export const startStaffStockCount = async (stockCountId) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/start`)).data;

export const saveStaffStockCountItem = async (stockCountId, data) =>
    (await api.put(`${BASE_URL}/${stockCountId}/items`, data)).data;

export const submitStaffStockCount = async (stockCountId) =>
    (await api.patch(`${BASE_URL}/${stockCountId}/submit`)).data;
