import api from "../../api";

const BASE_URL = "/stock-transfers";


export const getPackingConfirmations =
    async ({
               page = 0,
               size = 6,
               keyword = "",
           } = {}) => {

        const response =
            await api.get(
                `${BASE_URL}/packing-confirmations`,
                {
                    params: {
                        page,
                        size,
                        keyword,
                    },
                }
            );

        return response.data;
    };


export const getPackingResult =
    async (transferId) =>
        (
            await api.get(
                `${BASE_URL}/${transferId}/packing-result`
            )
        ).data;


export const confirmPacking =
    async (transferId) =>
        (
            await api.patch(
                `${BASE_URL}/${transferId}/confirm-packing`
            )
        ).data;


export const getShipment =
    async (transferId) =>
        (
            await api.get(
                `${BASE_URL}/${transferId}/shipment`
            )
        ).data;

export const confirmShipment =
    async (
        transferId,
        deliveryId
    ) => {

        const response =
            await api.patch(
                `${BASE_URL}/${transferId}/confirm-shipment`,
                {
                    deliveryId,
                }
            );

        return response.data;
    };
export const getDispatchHandoverList =
    async (
        keyword = ""
    ) => {

        const response =
            await api.get(
                `${BASE_URL}/dispatch-handover`,
                {
                    params: {
                        keyword,
                    },
                }
            );

        return response.data;
    };