import api from "../../api";

const getBaseUrl = (deliveryId) =>
    `/delivery-staff/trips/${deliveryId}/handover`;

export const scanHandoverPackage = async (deliveryId, data) => {
    const response = await api.post(
        `${getBaseUrl(deliveryId)}/packages/scan`,
        data
    );
    return response.data;
};

export const completeHandover = async (deliveryId, data) => {
    const response = await api.patch(
        `${getBaseUrl(deliveryId)}/complete`,
        data
    );
    return response.data;
};
