
const BASE_URL = "/api/admin/suspicious-alerts";

// ======================================================
// HELPERS
// ======================================================

// Kiem tra ID truoc khi goi API.
function requiredId(value) {
  if (
    value === null ||
    value === undefined ||
    String(value).trim() === ""
  ) {
    throw new Error("Alert ID is required.");
  }

  return encodeURIComponent(String(value));
}

// Dinh dang ngay cho LocalDateTime cua Spring Boot.
function formatDateTime(value, endOfDay = false) {
  if (!value) return null;

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return `${value}T${
      endOfDay ? "23:59:59.999999" : "00:00:00"
    }`;
  }

  return value;
}

// Tao query string cho Search, Filter va Pagination.
function buildQuery(filters = {}) {
  const params = new URLSearchParams();

  params.set("page", String(filters.page ?? 0));
  params.set("size", String(filters.size ?? 5));

  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }

  if (
    filters.alertLevel &&
    filters.alertLevel !== "ALL"
  ) {
    params.set("alertLevel", filters.alertLevel);
  }

  if (
    filters.status &&
    filters.status !== "ALL"
  ) {
    params.set("status", filters.status);
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

// Goi Backend bang session JSESSIONID.
async function apiRequest(path, options = {}) {
  const {
    method = "GET",
    body,
    signal,
  } = options;

  const headers = {
    Accept: "application/json",
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(
    `${BASE_URL}${path}`,
    {
      method,
      credentials: "include",
      cache: "no-store",
      headers,
      signal,
      ...(body !== undefined
        ? { body: JSON.stringify(body) }
        : {}),
    }
  );

  const responseText = await response.text();

  let data = null;

  if (responseText) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = responseText;
    }
  }

  if (!response.ok) {
    const message =
      data && typeof data === "object"
        ? data.message || data.error
        : null;

    const error = new Error(
      message || `Request failed (${response.status}).`
    );

    error.status = response.status;
    throw error;
  }

  return data;
}

// Kiem tra ghi chu xu ly.
function validateResolutionNote(note) {
  const value = String(note ?? "").trim();

  if (!value) {
    throw new Error("Resolution note is required.");
  }

  if (value.length > 2000) {
    throw new Error(
      "Resolution note must not exceed 2000 characters."
    );
  }

  return value;
}

// ======================================================
// GET SECURITY ALERTS
//
// GET /api/admin/suspicious-alerts
// ======================================================

export function getSecurityAlerts(
  filters = {},
  options = {}
) {
  const query = buildQuery(filters);

  return apiRequest(`?${query}`, {
    method: "GET",
    signal: options.signal,
  });
}

// ======================================================
// GET SECURITY ALERT DETAIL
//
// GET /api/admin/suspicious-alerts/{alertId}
// ======================================================

export function getSecurityAlertById(
  alertId,
  options = {}
) {
  return apiRequest(
    `/${requiredId(alertId)}`,
    {
      method: "GET",
      signal: options.signal,
    }
  );
}

// ======================================================
// GET USER ACTIVITY HISTORY
//
// GET /api/admin/suspicious-alerts/{alertId}/user-history
// ======================================================

export function getSecurityAlertUserHistory(
  alertId,
  options = {}
) {
  return apiRequest(
    `/${requiredId(alertId)}/user-history`,
    {
      method: "GET",
      signal: options.signal,
    }
  );
}

// ======================================================
// START INVESTIGATION
//
// NEW -> INVESTIGATING
//
// PATCH /api/admin/suspicious-alerts/{alertId}/investigating
// ======================================================

export function startSecurityAlertInvestigation(alertId) {
  return apiRequest(
    `/${requiredId(alertId)}/investigating`,
    {
      method: "PATCH",
    }
  );
}

// ======================================================
// RESOLVE SECURITY ALERT
//
// INVESTIGATING -> RESOLVED
//
// PATCH /api/admin/suspicious-alerts/{alertId}/resolved
// ======================================================

export function resolveSecurityAlert(alertId, note) {
  const resolutionNote = validateResolutionNote(note);

  return apiRequest(
    `/${requiredId(alertId)}/resolved`,
    {
      method: "PATCH",
      body: {
        resolutionNote,
      },
    }
  );
}

// ======================================================
// MARK FALSE POSITIVE
//
// INVESTIGATING -> FALSE_POSITIVE
//
// PATCH /api/admin/suspicious-alerts/{alertId}/false-positive
// ======================================================

export function markSecurityAlertFalsePositive(
  alertId,
  note
) {
  const resolutionNote = validateResolutionNote(note);

  return apiRequest(
    `/${requiredId(alertId)}/false-positive`,
    {
      method: "PATCH",
      body: {
        resolutionNote,
      },
    }
  );
}
