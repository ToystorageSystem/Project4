import api from "../../api";

// Warehouse Manager operations: daily task assignment, staff performance,
// low-stock reporting to Business and the inbound / outbound / transfer overview.
const M = "/warehouse/manager";

export const TASK_TYPES = [
  ["RECEIVING", "Receiving / inspection"],
  ["PUTAWAY", "Putaway"],
  ["ARRANGEMENT", "Arrange goods / shelves"],
  ["PICKING", "Picking"],
  ["PACKING", "Packing & sealing"],
  ["DISPATCH", "Dispatch handover"],
  ["TRANSFER", "Stock transfer"],
  ["STOCK_COUNT", "Stock count"],
  ["DAMAGE_HANDLING", "Damaged goods"],
  ["SHORTAGE_HANDLING", "Shortage / discrepancy"],
  ["STORE_RETURN", "Store returns"],
  ["OTHER", "Other"],
];
export const TASK_TYPE_LABEL = Object.fromEntries(TASK_TYPES);
export const PRIORITIES = ["LOW", "NORMAL", "HIGH", "URGENT"];

// ---- Daily tasks (manager)
export const getTaskBoard = (date) => api.get(`${M}/daily-tasks`, { params: { date } }).then((r) => r.data);
export const getTaskStaff = () => api.get(`${M}/daily-tasks/staff`).then((r) => r.data);
export const createTask = (body) => api.post(`${M}/daily-tasks`, body).then((r) => r.data);
export const createTasksBulk = (staffIds, task) => api.post(`${M}/daily-tasks/bulk`, { staffIds, task }).then((r) => r.data);
export const updateTask = (id, body) => api.put(`${M}/daily-tasks/${id}`, body).then((r) => r.data);
export const cancelTask = (id, reason) => api.post(`${M}/daily-tasks/${id}/cancel`, { reason }).then((r) => r.data);
export const managerCompleteTask = (id, body) => api.post(`${M}/daily-tasks/${id}/complete`, body).then((r) => r.data);
export const copyTasks = (fromDate, toDate) => api.post(`${M}/daily-tasks/copy`, { fromDate, toDate }).then((r) => r.data);

export const getJobs = (date) => api.get(`${M}/daily-tasks/jobs`, { params: { date } }).then((r) => r.data);
export const getAvailability = (date) => api.get(`${M}/daily-tasks/availability`, { params: { date } }).then((r) => r.data);

// ---- Shift roles (manager plans roles per day; staff take work in their role)
export const WORK_ROLES = [
  ["RECEIVING", "Receiving & putaway"],
  ["OUTBOUND", "Picking, packing & dispatch"],
  ["ISSUES", "Damaged, shortages & shelves"],
  ["STOCK_COUNT", "Stock count"],
  ["FLEX", "Flexible (any work)"],
];
export const WORK_ROLE_LABEL = Object.fromEntries(WORK_ROLES);
export const getShifts = (date) => api.get(`${M}/daily-tasks/shifts`, { params: { date } }).then((r) => r.data);
export const setShift = (body) => api.put(`${M}/daily-tasks/shifts`, body).then((r) => r.data);
export const copyShifts = (fromDate, toDate) => api.post(`${M}/daily-tasks/shifts/copy`, { fromDate, toDate }).then((r) => r.data);
export const getStaffQueue = (date) => api.get("/warehouse/staff/daily-tasks/queue", { params: { date } }).then((r) => r.data);
export const takeJob = (date, jobKey) => api.post("/warehouse/staff/daily-tasks/take", { date, jobKey }).then((r) => r.data);

// ---- Inbound schedule (read-only: which deliveries are expected on which day)
export const getInboundSchedule = (from, to) => api.get(`${M}/inbound-schedule`, { params: { from, to } }).then((r) => r.data);
export const DELIVERY_STATUS = {
  EXPECTED: ["Expected", ""],
  ARRIVED: ["Arrived", "warn"],
  RECEIVING: ["Receiving", "warn"],
  RECEIVED: ["Received", "good"],
  LATE: ["Late", "danger"],
  IN_TRANSIT: ["In transit", ""],
};
export const countWindowLabel = (type) => (type === "MID_MONTH" ? "Mid-month count" : type === "BEGINNING_OF_MONTH" ? "First-week count" : "End-of-month count");
export const AVAILABILITY = { FREE: ["Free", "good"], LOADED: ["Many open tasks", "warn"], BUSY: ["Busy", "danger"] };

// ---- Daily tasks (staff)
export const getMyTasks = (date) => api.get("/warehouse/staff/daily-tasks", { params: { date } }).then((r) => r.data);
export const startMyTask = (id) => api.post(`/warehouse/staff/daily-tasks/${id}/start`).then((r) => r.data);
export const completeMyTask = (id, body) => api.post(`/warehouse/staff/daily-tasks/${id}/complete`, body).then((r) => r.data);

// ---- Performance
export const getPerformance = (from, to) => api.get(`${M}/performance`, { params: { from, to } }).then((r) => r.data);
export const getStaffPerformance = (staffId, from, to) =>
  api.get(`${M}/performance/${staffId}`, { params: { from, to } }).then((r) => r.data);
export const addReview = (staffId, body) => api.post(`${M}/performance/${staffId}/reviews`, body).then((r) => r.data);

// ---- Low stock
export const getLowStock = (threshold, onlyLow = true) =>
  api.get(`${M}/low-stock`, { params: { threshold, onlyLow } }).then((r) => r.data);
export const setMinimumLevel = (productId, minimum) =>
  api.patch(`${M}/low-stock/products/${productId}/minimum`, { minimum }).then((r) => r.data);
export const sendLowStockReport = (body) => api.post(`${M}/low-stock/reports`, body).then((r) => r.data);
export const getSentLowStockReports = () => api.get(`${M}/low-stock/reports`).then((r) => r.data);
export const getSentLowStockReport = (id) => api.get(`${M}/low-stock/reports/${id}`).then((r) => r.data);

// ---- Operations overview
export const getOperationsOverview = () => api.get(`${M}/operations/overview`).then((r) => r.data);

// ---- Helpers
export const todayIso = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
export const shiftIso = (iso, days) => {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + days);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
export const firstOfMonthIso = () => `${todayIso().slice(0, 8)}01`;
export const fmtDateTime = (v) => (v ? String(v).replace("T", " ").slice(0, 16) : "—");
export const fmtTime = (v) => (v ? String(v).slice(0, 5) : "—");
