import api from "../../api";

const BASE_URL = "/delivery-staff/trips";

export const getMyTrips = async () => {
    const response = await api.get(BASE_URL);
    return response.data;
};

export const getTripDetail = async (deliveryId) => {
    const response = await api.get(`${BASE_URL}/${deliveryId}`);
    return response.data;
};

export const acceptTrip = async (deliveryId) => {
    const response = await api.patch(`${BASE_URL}/${deliveryId}/accept`);
    return response.data;
};

export const rejectTrip = async (deliveryId, reason) => {
    const response = await api.patch(`${BASE_URL}/${deliveryId}/reject`, { reason });
    return response.data;
};

export const markArrived = async (deliveryId) => {
    const response = await api.patch(`${BASE_URL}/${deliveryId}/arrived`);
    return response.data;
};

export const completeDelivery = async (deliveryId) => {
    const response = await api.patch(`${BASE_URL}/${deliveryId}/delivered`);
    return response.data;
};

export const failDelivery = async (deliveryId) => {
    const response = await api.patch(`${BASE_URL}/${deliveryId}/failed`);
    return response.data;
};
