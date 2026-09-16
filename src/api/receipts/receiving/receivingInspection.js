import api from "../../api";

const BASE_URL = "/warehouse/goods-receipts";

export const getReceivingList = async () =>
    (await api.get(`${BASE_URL}/receiving`)).data;

export const getReceivingDetail = async (receiptId) =>
    (await api.get(`${BASE_URL}/${receiptId}/receiving-detail`)).data;

export const startReceivingInspection = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/start-receiving`)).data;

export const saveBatchInspection = async (receiptId, data) =>
    (await api.put(`${BASE_URL}/${receiptId}/inspections`, data)).data;

export const finishReceivingInspection = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/finish-inspection`)).data;
