import { sessionFetch as fetch } from "../../../../api/sessionFetch";

const API_BASE_URL = "/api";

// Kiem tra ID truoc khi goi API.
function requiredId(value, label) {
  if (
    value === null ||
    value === undefined ||
    String(value).trim() === ""
  ) {
    throw new Error(`${label} is required.`);
  }

  return encodeURIComponent(String(value));
}

// Tat ca API su dung chung cookie JSESSIONID.
async function apiRequest(path, options = {}) {
  const { body, headers, ...requestOptions } = options;
  const hasBody = body !== undefined && body !== null;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestOptions,
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "application/json",
      ...(hasBody ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    ...(hasBody
      ? {
          body:
            typeof body === "string"
              ? body
              : JSON.stringify(body),
        }
      : {}),
  });

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
    const serverMessage =
      data && typeof data === "object"
        ? data.message || data.error
        : typeof data === "string"
          ? data
          : null;

    const error = new Error(
      serverMessage || `Request failed (${response.status}).`
    );

    error.status = response.status;
    throw error;
  }

  return data;
}

// ======================================================
// AUTHENTICATION
// ======================================================

// Chi su dung neu can dang nhap hoac chuyen tai khoan.
export function loginWarehouseAdmin(email, password) {
  return apiRequest("/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

// Kiem tra session dang co.
// Khong yeu cau email, password moi.
export function getWarehouseSession() {
  return apiRequest("/auth/me", {
    method: "GET",
  });
}

// ======================================================
// GET WAREHOUSES
// ======================================================

export function getWarehouses(
  page = 0,
  size = 100,
  filters = {}
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (filters.keyword?.trim()) {
    params.set("keyword", filters.keyword.trim());
  }

  if (filters.type && filters.type !== "ALL") {
    params.set("type", filters.type);
  }

  if (filters.status && filters.status !== "ALL") {
    params.set("status", filters.status);
  }

  return apiRequest(
    `/admin/warehouses?${params.toString()}`,
    { method: "GET" }
  );
}

// ======================================================
// SELECTABLE WAREHOUSES
// ======================================================

export function getSelectableWarehouses(type = null) {
  const params = new URLSearchParams();

  if (type) {
    params.set("type", type);
  }

  const query = params.toString();

  return apiRequest(
    `/admin/warehouses/selectable${
      query ? `?${query}` : ""
    }`,
    { method: "GET" }
  );
}

// ======================================================
// WAREHOUSE DETAILS
// ======================================================

export function getWarehouseById(warehouseId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}`,
    { method: "GET" }
  );
}

// ======================================================
// CREATE
// ======================================================

export function createWarehouse(request) {
  return apiRequest("/admin/warehouses", {
    method: "POST",
    body: {
      name: request.name,
      type: request.type,
      address: request.address,
      phone: request.phone,
    },
  });
}

// ======================================================
// UPDATE
// ======================================================

export function updateWarehouse(warehouseId, request) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}`,
    {
      method: "PUT",
      body: {
        name: request.name,
        address: request.address,
        phone: request.phone,
      },
    }
  );
}

// ======================================================
// DEACTIVATION IMPACT
// ======================================================

export function getWarehouseDeactivationImpact(warehouseId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}/deactivation-impact`,
    { method: "GET" }
  );
}

// ======================================================
// DEACTIVATE
// ======================================================

export function deactivateWarehouse(warehouseId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}/inactive`,
    { method: "PATCH" }
  );
}

// ======================================================
// ACTIVATE
// ======================================================

export function activateWarehouse(warehouseId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}/active`,
    { method: "PATCH" }
  );
}

// ======================================================
// GET EMPLOYEES
// ======================================================

export function getWarehouseEmployees(warehouseId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(warehouseId, "Warehouse ID")}/employees`,
    { method: "GET" }
  );
}

// ======================================================
// ASSIGN EMPLOYEE
// ======================================================

export function assignWarehouseEmployee(warehouseId, userId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(
      warehouseId,
      "Warehouse ID"
    )}/employees/${requiredId(userId, "User ID")}/assign`,
    { method: "PATCH" }
  );
}

// ======================================================
// UNASSIGN EMPLOYEE
// ======================================================

export function unassignWarehouseEmployee(warehouseId, userId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(
      warehouseId,
      "Warehouse ID"
    )}/employees/${requiredId(userId, "User ID")}/unassign`,
    { method: "PATCH" }
  );
}

// ======================================================
// ASSIGN MANAGER
// ======================================================

export function assignWarehouseManager(warehouseId, userId) {
  return apiRequest(
    `/admin/warehouses/${requiredId(
      warehouseId,
      "Warehouse ID"
    )}/manager/${requiredId(userId, "User ID")}`,
    { method: "PATCH" }
  );
}

