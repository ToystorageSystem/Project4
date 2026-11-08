import { sessionFetch as fetch } from "../../../../api/sessionFetch";

const BASE_URL = "/api/admin/activity-logs";

// Chuyen ngay tu input HTML sang dinh dang Backend yeu cau.
function formatDateTime(value, endOfDay = false) {
  if (!value) return null;

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return `${value}T${
      endOfDay ? "23:59:59.999999" : "00:00:00"
    }`;
  }

  return value;
}

// Tao tham so tim kiem, loc va phan trang.
function buildQuery(filters = {}, includePagination = false) {
  const params = new URLSearchParams();

  if (includePagination) {
    params.set("page", String(filters.page ?? 0));
    params.set("size", String(filters.size ?? 5));
  }

  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }

  if (filters.action && filters.action !== "ALL") {
    params.set("action", filters.action);
  }

  if (filters.entityType && filters.entityType !== "ALL") {
    params.set("entityType", filters.entityType);
  }

  const fromDate = formatDateTime(filters.fromDate);
  const toDate = formatDateTime(filters.toDate, true);

  if (fromDate) {
    params.set("fromDate", fromDate);
  }

  if (toDate) {
    params.set("toDate", toDate);
  }

  return params.toString();
}

// Xu ly loi HTTP tu Backend.
async function throwIfNotOk(response) {
  if (response.ok) return;

  let message = `Request failed (${response.status}).`;

  try {
    const data = await response.json();
    message = data.message || data.error || message;
  } catch {
    // Giu thong bao HTTP neu Backend khong tra JSON.
  }

  const error = new Error(message);
  error.status = response.status;

  throw error;
}

// Ham GET API dung chung.
// credentials giup trinh duyet gui JSESSIONID.
async function getJson(url, options = {}) {
  const response = await fetch(url, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "application/json",
    },
    ...options,
  });

  await throwIfNotOk(response);

  return response.json();
}

// ======================================================
// GET ACTIVITY LOGS
// Tim kiem, loc va phan trang.
// ======================================================

export function getAdminActivityLogs(
  filters = {},
  options = {}
) {
  const query = buildQuery(filters, true);

  return getJson(
    `${BASE_URL}?${query}`,
    options
  );
}

// ======================================================
// GET ACTIVITY LOG DETAIL
// Lay chi tiet mot nhat ky theo ID.
// ======================================================

export function getAdminActivityLogById(
  logId,
  options = {}
) {
  if (
    logId === null ||
    logId === undefined ||
    logId === ""
  ) {
    throw new Error("Activity log ID is required.");
  }

  return getJson(
    `${BASE_URL}/${encodeURIComponent(String(logId))}`,
    options
  );
}

// ======================================================
// EXPORT ACTIVITY LOGS TO CSV
// Xuat tat ca ban ghi phu hop bo loc, khong chi 1 trang.
// ======================================================

export async function downloadAdminActivityLogsCsv(
  filters = {}
) {
  const query = buildQuery(filters);

  const url = query
    ? `${BASE_URL}/export?${query}`
    : `${BASE_URL}/export`;

  const response = await fetch(url, {
    method: "GET",
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "text/csv",
    },
  });

  await throwIfNotOk(response);

  const blob = await response.blob();

  const disposition =
    response.headers.get("content-disposition") || "";

  const matched = disposition.match(
    /filename="?([^";]+)"?/i
  );

  const fileName = (
    matched?.[1] || "activity-logs.csv"
  ).replace(/[\\/]/g, "_");

  const objectUrl = URL.createObjectURL(blob);

  const link = document.createElement("a");

  link.href = objectUrl;
  link.download = fileName;

  document.body.appendChild(link);
  link.click();
  link.remove();

  setTimeout(() => {
    URL.revokeObjectURL(objectUrl);
  }, 1000);

  return fileName;
}

