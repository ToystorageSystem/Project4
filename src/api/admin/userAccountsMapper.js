
/**
 * Chuyen du lieu UserResponse cua Spring Boot
 * sang cau truc dung trong React.
 */
export function mapBackendUser(user = {}) {
  const roleInfos = Array.isArray(user.roles)
    ? user.roles
    : [];

  const roles = roleInfos
    .map((role) =>
      String(role.roleCode || "").replace(
        /^ROLE[-_]/,
        ""
      )
    )
    .filter(Boolean);

  const roleIds = roleInfos
    .map((role) => Number(role.id))
    .filter((id) => Number.isFinite(id));

  return {
    id: user.id,
    code: user.userCode || "",
    name: user.name || "",
    email: user.email || "",
    phone: user.phone || "",
    identityCard: user.identityCard || "",

    role: roles[0] || "UNASSIGNED",
    roles,
    roleIds,
    roleInfos,

    location: user.warehouseName || "Not assigned",
    warehouseId: user.warehouseId ?? null,
    warehouseCode: user.warehouseCode || null,

    status: user.status || "INACTIVE",
    mustChangePassword: Boolean(
      user.mustChangePassword
    ),
    createdAt: user.createdAt || null,
    updatedAt: user.updatedAt || null,
  };
}

/**
 * Chuyen Page<UserResponse> tu Spring Boot.
 */
export function mapBackendUserPage(data = {}) {
  const content = Array.isArray(data.content)
    ? data.content
    : [];

  return {
    users: content.map(mapBackendUser),
    totalElements: data.totalElements ?? content.length,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? 0,
    size: data.size ?? content.length,
  };
}
