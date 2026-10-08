import api from "../../api";

const BASE_URL = "/warehouse/store-returns";

export const getStoreReturns = async () =>
    (await api.get(BASE_URL)).data;

export const getStoreReturn = async (returnId) =>
    (await api.get(`${BASE_URL}/${returnId}`)).data;

export const inspectStoreReturnItem = async (returnId, itemId, data) =>
    (await api.patch(`${BASE_URL}/${returnId}/items/${itemId}/inspect`, data)).data;

export const resolveStoreReturnAsWarehouse = async (
    returnId,
    discrepancyId,
    reason
) => {
    await api.patch(
        `${BASE_URL}/${returnId}/discrepancies/${discrepancyId}/warehouse`,
        { reason }
    );
};

export const sendStoreReturnDiscrepancyToStore = async (
    returnId,
    discrepancyId,
    reason
) => {
    await api.patch(
        `${BASE_URL}/${returnId}/discrepancies/${discrepancyId}/send-store`,
        { reason }
    );
};

export const confirmStoreReturn = async (returnId) =>
    (await api.patch(`${BASE_URL}/${returnId}/confirm`)).data;
