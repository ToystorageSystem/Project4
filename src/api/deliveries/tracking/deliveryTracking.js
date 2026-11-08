import api from "../../api";

export const updateDeliveryLocation = async (deliveryId, data) => {
    const response = await api.post(
        `/deliveries/${deliveryId}/tracking/location`,
        data
    );
    return response.data;
};

export const getLatestDeliveryLocation = async (deliveryId) => {
    const response = await api.get(
        `/deliveries/${deliveryId}/tracking/latest`
    );
    return response.data;
};
