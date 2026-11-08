import api from "../../api";


const BASE_URL =
    "/warehouse/shipment-history";


export const getShipmentHistory =
    async ({
               page = 0,
               size = 6,
               keyword = "",
           } = {}) => {

        const response =
            await api.get(
                BASE_URL,
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