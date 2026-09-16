import api from "../../api";

const BASE_URL = "/putaway-plans";

export const createPutawayPlan = async (
    receiptId,
    data
) => {
    const response = await api.post(
        `${BASE_URL}/goods-receipts/${receiptId}`,
        data
    );

    return response.data;
};

export const getPutawayPlans = async () => {
    const response = await api.get(BASE_URL);
    return response.data;
};

export const getPutawayPlan = async (taskId) => {
    const response = await api.get(
        `${BASE_URL}/${taskId}`
    );

    return response.data;
};

export const confirmPutawayCompletion = async (taskId) => {
    const response = await api.patch(
        `${BASE_URL}/${taskId}/confirm-completion`
    );

    return response.data;
};
