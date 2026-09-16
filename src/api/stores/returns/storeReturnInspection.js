import api from "../../api";

const BASE_URL = "/warehouse/store-returns";

export const getStoreReturns = async () =>
    (await api.get(BASE_URL)).data;

export const getStoreReturn = async (returnId) =>
    (await api.get(`${BASE_URL}/${returnId}`)).data;

export const inspectStoreReturnItem = async (returnId, itemId, data) =>
    (await api.patch(`${BASE_URL}/${returnId}/items/${itemId}/inspect`, data)).data;
