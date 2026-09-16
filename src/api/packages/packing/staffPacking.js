import api from "../../api";

const BASE_URL = "/warehouse/staff/packing";

export const createPackage = async (transferId) =>
    (await api.post(`${BASE_URL}/transfers/${transferId}/packages`)).data;

export const scanPackageItem = async (packageId, data) =>
    (await api.post(`${BASE_URL}/packages/${packageId}/scan`, data)).data;

export const getPackage = async (packageId) =>
    (await api.get(`${BASE_URL}/packages/${packageId}`)).data;

export const sealPackage = async (packageId, data) =>
    (await api.patch(`${BASE_URL}/packages/${packageId}/seal`, data)).data;

export const completePacking = async (transferId) => {
    await api.patch(`${BASE_URL}/transfers/${transferId}/complete`);
};
