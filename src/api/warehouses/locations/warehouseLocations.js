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
// Products assigned to a location / zone (which goods are stored where).
export const getLocationProductOptions = async (keyword) => {
    const response = await api.get("/warehouse/location-products/options", { params: { keyword } });
    return response.data;
};

export const getAllLocationProducts = async () => {
    const response = await api.get("/warehouse/location-products");
    return response.data;
};

export const getLocationProducts = async (locationId) => {
    const response = await api.get(`/warehouse/location-products/locations/${locationId}`);
    return response.data;
};

export const setLocationProducts = async (locationId, productIds) => {
    const response = await api.put(`/warehouse/location-products/locations/${locationId}`, { productIds });
    return response.data;
};
