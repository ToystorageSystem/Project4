import api from "../api";

// Business Operations: low-stock reports sent by Warehouse Managers.
const BASE = "/business/low-stock-reports";

export const listLowStockReports = (status) =>
  api.get(BASE, { params: { status: status && status !== "ALL" ? status : undefined } }).then((r) => r.data);
export const getLowStockReport = (id) => api.get(`${BASE}/${id}`).then((r) => r.data);
export const acknowledgeLowStockReport = (id, note) => api.patch(`${BASE}/${id}/acknowledge`, { note }).then((r) => r.data);
export const resolveLowStockReport = (id, note) => api.patch(`${BASE}/${id}/resolve`, { note }).then((r) => r.data);
