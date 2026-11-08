import api from "../../api";

const BASE_URL = "/receiving-incident-reports";

export const getReceivingIncidentReports = async ({
                                                      keyword = "",
                                                      page = 0,
                                                      size = 20,
                                                  } = {}) =>
    (
        await api.get(
            BASE_URL,
            {
                params: {
                    keyword,
                    page,
                    size,
                },
            }
        )
    ).data;

export const getReceivingIncidentReportDetail = async (id) =>
    (
        await api.get(`${BASE_URL}/${id}`)
    ).data;

export const updateReceivingIncidentReport = async (id, data) =>
    (
        await api.patch(
            `${BASE_URL}/${id}`,
            data
        )
    ).data;

export const updateReceivingIncidentReportItem = async (
    reportId,
    itemId,
    data
) =>
    (
        await api.patch(
            `${BASE_URL}/${reportId}/items/${itemId}`,
            data
        )
    ).data;
