import api from "../../api";

const BASE_URL = "/warehouse/receiving-shortages";

export const createReceivingShortageReport = async ({
    receiptId,
    productId,
    description,
    image,
}) => {
    const formData = new FormData();
    formData.append("productId", String(productId));
    formData.append("description", description);

    if (image) formData.append("image", image);

    const response = await api.post(
        `${BASE_URL}/goods-receipts/${receiptId}`,
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
    );

    return response.data;
};

export const getMyReceivingShortageReports = async () =>
    (await api.get(`${BASE_URL}/my-reports`)).data;
