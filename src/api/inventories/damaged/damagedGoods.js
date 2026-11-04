import api from "../../api";

const BASE_URL = "/damaged-goods";


export const getDamagedGoodsReports =
    async () => {

        const response =
            await api.get(
                BASE_URL
            );

        return response.data;
    };


export const getDamagedGoodsReport =
    async (
        reportId
    ) => {

        const response =
            await api.get(
                `${BASE_URL}/${reportId}`
            );

        return response.data;
    };


export const startDamagedGoodsInspection =
    async (
        reportId
    ) => {

        const response =
            await api.patch(
                `${BASE_URL}/${reportId}/start-inspection`
            );

        return response.data;
    };


export const handleDamagedGoodsItem =
    async ({
               reportId,
               itemId,
               confirmedQuantity,
               disposition,
               resolutionNote,
           }) => {

        const response =
            await api.patch(
                `${BASE_URL}/${reportId}/items/${itemId}/handle`,
                {
                    confirmedQuantity,
                    disposition,
                    resolutionNote,
                }
            );

        return response.data;
    };