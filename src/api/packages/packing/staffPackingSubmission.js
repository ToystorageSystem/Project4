import api from "../../api";

const BASE_URL = "/warehouse/staff/packing-submissions";

export const previewPackingSubmission = async (transferId) =>
    (await api.get(`${BASE_URL}/transfers/${transferId}`)).data;

export const submitPacking = async (transferId) =>
    (await api.post(`${BASE_URL}/transfers/${transferId}/submit`)).data;
