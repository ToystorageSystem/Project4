import api from "../../api";

const BASE_URL = "/warehouse/putaway";

export const getMyPutawayTasks = async () => {
    const response = await api.get(
        `${BASE_URL}/my-tasks`
    );

    return response.data;
};

export const getPutawayTask = async (taskId) => {
    const response = await api.get(
        `${BASE_URL}/${taskId}`
    );

    return response.data;
};

export const startPutawayTask = async (taskId) => {
    const response = await api.patch(
        `${BASE_URL}/${taskId}/start`
    );

    return response.data;
};

export const putawayItem = async (
    taskId,
    itemId,
    data
) => {
    const response = await api.patch(
        `${BASE_URL}/${taskId}/items/${itemId}`,
        data
    );

    return response.data;
};

export const completePutawayTask = async (taskId) => {
    const response = await api.patch(
        `${BASE_URL}/${taskId}/complete`
    );

    return response.data;
};
