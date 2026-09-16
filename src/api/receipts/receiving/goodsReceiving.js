import api from "../../api";

const BASE_URL = "/goods-receipts";

export const confirmVehicleArrival = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/confirm-arrival`)).data;

export const startGoodsReceiving = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/start-receiving`)).data;

export const inspectGoodsReceiptProduct = async (receiptId, data) =>
    (await api.post(`${BASE_URL}/${receiptId}/inspections`, data)).data;

export const finishGoodsReceiptInspection = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/finish-inspection`)).data;
