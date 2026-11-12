import api from "../../api";

const BASE_URL = "/damaged-goods";

export const getDamagedGoodsReports = async () =>
    (await api.get(BASE_URL)).data;

export const getDamagedGoodsReport = async (reportId) =>
    (await api.get(`${BASE_URL}/${reportId}`)).data;

export const startDamagedGoodsInspection = async (reportId) =>
    (await api.patch(`${BASE_URL}/${reportId}/start-inspection`)).data;

export const handleDamagedGoodsItem = async (reportId, itemId, data) =>
    (await api.patch(`${BASE_URL}/${reportId}/items/${itemId}/handle`, data)).data;
