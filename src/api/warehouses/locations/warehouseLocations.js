import api from "../../api.js";

const BASE_URL = "/warehouse/locations";

export const getWarehouseLocations = async () => {
    const response = await api.get(BASE_URL);
    return response.data;
};

export const getWarehouseLocation = async (id) => {
    const response = await api.get(`${BASE_URL}/${id}`);
    return response.data;
};

export const createWarehouseLocation = async (data) => {
    const response = await api.post(BASE_URL, data);
    return response.data;
};

export const updateWarehouseLocation = async (id, data) => {
    const response = await api.put(
        `${BASE_URL}/${id}`,
        data
    );

    return response.data;
};

export const updateWarehouseLocationStatus = async (
    id,
    status
) => {
    const response = await api.patch(
        `${BASE_URL}/${id}/status`,
        {
            status,
        }
    );

    return response.data;
};