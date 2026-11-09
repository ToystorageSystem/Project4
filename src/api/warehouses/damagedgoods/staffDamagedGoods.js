import api from "../../api";

const BASE_URL = "/warehouse/staff/damaged-goods";

export const createDamagedGoodsReport = async (request, image = null) => {
    const formData = new FormData();

    formData.append(
        "request",
        new Blob(
            [JSON.stringify(request)],
            { type: "application/json" }
        )
    );

    if (image) {
        formData.append("image", image);
    }

    const response = await api.post(
        BASE_URL,
        formData,
        {
            headers: {
                "Content-Type": "multipart/form-data",
            },
        }
    );

    return response.data;
};

export const getMyDamagedGoodsReports = async () => {
    const response = await api.get(BASE_URL);
    return response.data;
};

export const getDamagedGoodsReportDetail = async (reportId) => {
    const response = await api.get(
        `${BASE_URL}/${reportId}`
    );

    return response.data;
};
