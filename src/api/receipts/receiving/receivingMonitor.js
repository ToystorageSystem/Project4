import api from "../../api";

const BASE_URL = "/goods-receipts";

export const getReceivingMonitoringList = async ({
                                                     keyword = "",
                                                     page = 0,
                                                     size = 12,
                                                 } = {}) =>
    (
        await api.get(`${BASE_URL}/monitoring`, {
            params: { keyword, page, size },
        })
    ).data;

export const getWaitingReviewList = async ({
                                               keyword = "",
                                               page = 0,
                                               size = 10,
                                           } = {}) =>
    (
        await api.get(`${BASE_URL}/waiting-review`, {
            params: { keyword, page, size },
        })
    ).data;

export const getCompletedReceivingList = async ({
                                                    keyword = "",
                                                    page = 0,
                                                    size = 20,
                                                } = {}) =>
    (
        await api.get(`${BASE_URL}/completed-receiving`, {
            params: { keyword, page, size },
        })
    ).data;

export const getReceivingProgress = async (receiptId) =>
    (await api.get(`${BASE_URL}/${receiptId}/progress`)).data;
