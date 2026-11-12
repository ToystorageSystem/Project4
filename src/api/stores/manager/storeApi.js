import api from "../../api";
// Store module API (backend prefix /api/store). The shared axios client adds /api, cookies and CSRF.
const enc = encodeURIComponent;
export const qs = (params = {}) =>
  new URLSearchParams(Object.entries(params).filter(([, v]) => v !== "" && v != null)).toString();

// GET paths, used with useResource(path).
export const STORE_API = Object.freeze({
  dashboard: "/store/dashboard",
  manifests: "/store/receiving/manifests",
  manifest: (code) => `/store/receiving/manifests/${enc(code)}`,
  packageItems: (code) => `/store/receiving/packages/${enc(code)}/items`,
  shelves: "/store/shelves",
  shelf: (id) => `/store/shelves/${enc(id)}`,
  lowShelf: "/store/products/low-shelf",
  products: (params) => `/store/products?${qs(params)}`,
  product: (id) => `/store/products/${enc(id)}`,
  sales: (params) => `/store/sales?${qs(params)}`,
  salesSummary: (params) => `/store/sales/summary?${qs(params)}`,
  sale: (id) => `/store/sales/${enc(id)}`,
  stockTransactions: (params) => `/store/stock-transactions?${qs(params)}`,
});

export const getPackageItems = async (packageCode) => (await api.get(STORE_API.packageItems(packageCode))).data;
export const findProductByBarcode = async (barcode) =>
  (await api.get(`/store/products/barcode/${enc(barcode.trim())}`)).data;
export const finishInspection = async (manifestCode) =>
  (await api.post(`${STORE_API.manifest(manifestCode)}/finish-inspection`)).data;
export const confirmReceiving = async (manifestCode, note) =>
  (await api.post(`${STORE_API.manifest(manifestCode)}/confirm`, { note: note?.trim() || null })).data;
export const changeMinShelf = async (productId, minShelfQuantity) =>
  (await api.put(`/store/products/${enc(productId)}/min-shelf`, { minShelfQuantity })).data;
