
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

// Ham goi API chung.
// credentials: "include" giup gui session JSESSIONID.
async function apiRequest(path, options = {}) {
  const { body, headers, ...requestOptions } = options;

  const hasBody =
    body !== null && body !== undefined;

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
      data && typeof data === "object"
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

export function loginRoleAdmin(email, password) {
  return apiRequest("/auth/login", {
    method: "POST",
    body: {
      email,
      password,
    },
  });
}

// ========================================
// GET ROLES
// ========================================

export function getRoles(search = "") {
  const params = new URLSearchParams();

  if (search.trim()) {
    params.set("search", search.trim());
  }

  const query = params.toString();

  return apiRequest(
    `/admin/roles${query ? `?${query}` : ""}`,
    { method: "GET" }
  );
}

// ========================================
// GET ROLE BY ID
// ========================================

export function getRoleById(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}`,
    { method: "GET" }
  );
}

// ========================================
// GET ALL PERMISSIONS
// ========================================

export function getPermissions() {
  return apiRequest("/admin/permissions", {
    method: "GET",
  });
}

// ========================================
// GET PERMISSIONS OF ROLE
// ========================================

export function getRolePermissions(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}/permissions`,
    { method: "GET" }
  );
}

// ========================================
// GET USERS ASSIGNED TO ROLE
// ========================================

export function getRoleUsers(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}/users`,
    { method: "GET" }
  );
}

// ========================================
// CREATE ROLE
// ========================================

export function createRole(request) {
  return apiRequest("/admin/roles", {
    method: "POST",
    body: {
      roleCode: request.roleCode,
      roleName: request.roleName,
      description: request.description || "",
    },
  });
}

// ========================================
// UPDATE ROLE
// ========================================

export function updateRole(roleId, request) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}`,
    {
      method: "PUT",
      body: {
        roleName: request.roleName,
        description: request.description || "",
      },
    }
  );
}

// ========================================
// LOCK ROLE
// ========================================

export function lockRole(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}/lock`,
    { method: "PATCH" }
  );
}

// ========================================
// UNLOCK ROLE
// ========================================

export function unlockRole(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}/unlock`,
    { method: "PATCH" }
  );
}

// ========================================
// DELETE ROLE
// ========================================

export function deleteRole(roleId) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}`,
    { method: "DELETE" }
  );
}

// ========================================
// CHECK PERMISSION IMPACT
// ========================================

// Chi kiem tra tac dong, khong sua Database.
export function getPermissionImpact(
  roleId,
  permissionId
) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}` +
      `/permissions/${requiredId(permissionId, "Permission ID")}/impact`,
    { method: "GET" }
  );
}

// ========================================
// GRANT PERMISSION
// ========================================

export function grantRolePermission(
  roleId,
  permissionId
) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}` +
      `/permissions/${requiredId(permissionId, "Permission ID")}`,
    { method: "POST" }
  );
}

// ========================================
// REVOKE PERMISSION
// ========================================

export function revokeRolePermission(
  roleId,
  permissionId
) {
  return apiRequest(
    `/admin/roles/${requiredId(roleId, "Role ID")}` +
      `/permissions/${requiredId(permissionId, "Permission ID")}`,
    { method: "DELETE" }
  );
}
