import api from "../../api";

const BASE_URL = "/goods-receipts";

export const getReceivingInspectionResult = async (receiptId) =>
    (await api.get(`${BASE_URL}/${receiptId}/inspection-result`)).data;

export const confirmReceivingInspection = async (receiptId) =>
    (await api.patch(`${BASE_URL}/${receiptId}/confirm-receiving`)).data;

export const requestReceivingReinspection = async (
    receiptId,
    data
) =>
    (
        await api.patch(
            `${BASE_URL}/${receiptId}/request-reinspection`,
            data
        )
    ).data;