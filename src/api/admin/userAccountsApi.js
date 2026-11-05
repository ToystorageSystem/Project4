
const API_BASE_URL = "/api";

// Chuyen ID thanh gia tri hop le tren URL.
function userPath(userId) {
  if (
    userId === null ||
    userId === undefined ||
    String(userId).trim() === ""
  ) {
    throw new Error("User ID is required.");
  }

  return `/admin/users/${encodeURIComponent(
    String(userId)
  )}`;
}

// Ham goi API chung.
// Trinh duyet tu gui JSESSIONID qua credentials.
async function apiRequest(path, options = {}) {
  const { body, headers, ...requestOptions } = options;

  const hasBody =
    body !== undefined && body !== null;

  const response = await fetch(
    `${API_BASE_URL}${path}`,
    {
      ...requestOptions,
      credentials: "include",
      cache: "no-store",
      headers: {
        Accept: "application/json",
        ...(hasBody
          ? { "Content-Type": "application/json" }
          : {}),
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
      data &&
      typeof data === "object"
        ? data.message || data.error
        : typeof data === "string"
          ? data
          : null;

    const error = new Error(
      message ||
        `Request failed (${response.status}).`
    );

    error.status = response.status;

    throw error;
  }

  return data;
}

// ========================================
// AUTHENTICATION
// ========================================

export function loginAdmin(email, password) {
  return apiRequest("/auth/login", {
    method: "POST",
    body: {
      email,
      password,
    },
  });
}

// Kiem tra session Admin chung.
export function getAdminSession() {
  return apiRequest("/auth/me", { method: "GET" });
}

// ========================================
// GET USERS
// ========================================

export function getAdminUsers(
  page = 0,
  size = 10,
  filters = {}
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (filters.keyword?.trim()) {
    params.set(
      "keyword",
      filters.keyword.trim()
    );
  }

  if (filters.roleId != null) {
    params.set(
      "roleId",
      String(filters.roleId)
    );
  }

  if (filters.warehouseId != null) {
    params.set(
      "warehouseId",
      String(filters.warehouseId)
    );
  }

  if (
    filters.status &&
    filters.status !== "ALL"
  ) {
    params.set(
      "status",
      filters.status
    );
  }

  return apiRequest(
    `/admin/users?${params.toString()}`,
    { method: "GET" }
  );
}

// ========================================
// GET USER DETAILS
// ========================================

export function getAdminUserById(userId) {
  return apiRequest(userPath(userId), {
    method: "GET",
  });
}

// ========================================
// GET ROLES AND WAREHOUSES
// ========================================

// Backend yeu cau ROLE_MANAGE.
export function getAdminRoles() {
  return apiRequest("/admin/roles", {
    method: "GET",
  });
}

// Backend yeu cau WAREHOUSE_READ.
export function getSelectableWarehouses() {
  return apiRequest(
    "/admin/warehouses/selectable",
    { method: "GET" }
  );
}

// ========================================
// CREATE USER
// ========================================

// Request:
// name, email, phone, identityCard,
// password, roleIds, warehouseId.
export function createAdminUser(request) {
  return apiRequest("/admin/users", {
    method: "POST",
    body: {
      name: request.name,
      email: request.email,
      phone: request.phone,
      identityCard: request.identityCard,
      password: request.password,
      roleIds: request.roleIds,
      warehouseId:
        request.warehouseId ?? null,
    },
  });
}

// ========================================
// UPDATE USER INFORMATION
// ========================================

// Backend khong thay doi role tai API nay.
export function updateAdminUser(
  userId,
  request
) {
  return apiRequest(userPath(userId), {
    method: "PUT",
    body: {
      name: request.name,
      email: request.email,
      phone: request.phone,
      identityCard: request.identityCard,
      warehouseId:
        request.warehouseId ?? null,
    },
  });
}

// ========================================
// UPDATE USER ROLES
// ========================================

export function updateAdminUserRoles(
  userId,
  roleIds
) {
  return apiRequest(
    `${userPath(userId)}/roles`,
    {
      method: "PUT",
      body: { roleIds },
    }
  );
}

// ========================================
// LOCK USER
// ========================================

export function lockAdminUser(userId) {
  return apiRequest(
    `${userPath(userId)}/lock`,
    { method: "PATCH" }
  );
}

// ========================================
// UNLOCK USER
// ========================================

export function unlockAdminUser(userId) {
  return apiRequest(
    `${userPath(userId)}/unlock`,
    { method: "PATCH" }
  );
}

// ========================================
// DEACTIVATE USER
// ========================================

export function deactivateAdminUser(userId) {
  return apiRequest(
    `${userPath(userId)}/inactive`,
    { method: "PATCH" }
  );
}

// ========================================
// RESET PASSWORD
// ========================================

// Backend nhan newPassword.
// Khong luu mat khau vao localStorage.
export function resetAdminUserPassword(
  userId,
  newPassword
) {
  return apiRequest(
    `${userPath(userId)}/reset-password`,
    {
      method: "PATCH",
      body: { newPassword },
    }
  );
}
