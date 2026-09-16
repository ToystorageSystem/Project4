import api from "../../api";

const BASE_URL = "/warehouse/staff/store-returns";

export const getWaitingStoreReturns = async () =>
    (await api.get(BASE_URL)).data;

export const getReturnedGoodsDetail = async (returnId) =>
    (await api.get(`${BASE_URL}/${returnId}`)).data;

export const startReturnedGoodsInspection = async (returnId) =>
    (await api.patch(`${BASE_URL}/${returnId}/start`)).data;

export const scanReturnedPackage = async (returnId, data) =>
    (await api.post(`${BASE_URL}/${returnId}/scan-package`, data)).data;

export const inspectReturnedItem = async (returnId, data) =>
    (await api.put(`${BASE_URL}/${returnId}/items`, data)).data;

export const uploadReturnedItemEvidence = async (returnId, itemId, image) => {
    const formData = new FormData();
    formData.append("image", image);

    const response = await api.post(
        `${BASE_URL}/${returnId}/items/${itemId}/evidence`,
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
    );

    return response.data;
};

export const submitReturnedGoodsInspection = async (returnId) =>
    (await api.patch(`${BASE_URL}/${returnId}/submit`)).data;
