export const normalizeRole = (value) =>
  String(value || "")
    .toUpperCase()
    .replace(/[ -]+/g, "_")
    .replace(/^(ROLE_)+/, "");
export function hasRole(user, ...roles) {
  return (user?.authorities || []).some((a) =>
    roles.map(normalizeRole).includes(normalizeRole(typeof a === "string" ? a : a?.authority)),
  );
}
export const isManager = (user) =>
  hasRole(user, "WAREHOUSE_MANAGER", "WH_MANAGER", "ROLE_WH_MANAGER");
export const isStaff = (user) =>
  hasRole(user, "WAREHOUSE_STAFF", "WH_STAFF", "ROLE_WH_STAFF");
export const isDelivery = (user) => hasRole(user, "DELIVERY_STAFF", "DELIVERY");
export const isAdmin = (user) => hasRole(user, "ADMIN", "ADMINISTRATOR", "SYS_ADMIN");
export const isBusiness = (user) => hasRole(user, "BIZ_OPS", "BUSINESS_OPERATIONS", "BIZ_MANAGER", "BUSINESS_MANAGER", "BIZ_STAFF", "BUSINESS_STAFF");
// Store module (web is for Store Manager only; Store Staff uses the Flutter app).
export const isStoreManager = (user) => hasRole(user, "STORE_MANAGER");
export function homePath(user) {
  if (isAdmin(user)) return "/admin/dashboard";
  if (isBusiness(user)) return "/business/dashboard";
  if (isManager(user) || isStaff(user) || isDelivery(user)) return "/warehouse/dashboard";
  if (isStoreManager(user)) return "/store/dashboard";
  return "/workspace";
}
export const hasPermission = (user, permission) =>
  user?.authorities?.includes(permission) === true;
export function authPath(user) {
  if (!user) return "/login";
  return (
    {
      CHANGE_PASSWORD: "/change-password",
      SETUP_2FA: "/2fa/setup",
      VERIFY_2FA: "/2fa",
    }[user.stage] ||
    (user.stage === "AUTHENTICATED" ? homePath(user) : "/login")
  );
}
export function roleLabel(user) {
  return isAdmin(user) ? "Administrator" : isBusiness(user) ? "Business Operations" : isManager(user)
    ? "Warehouse Manager"
    : isStaff(user)
      ? "Warehouse Staff"
      : isDelivery(user)
        ? "Delivery Staff"
        : isStoreManager(user)
          ? "Store Manager"
          : "System Account";
}
export function canAccess(user, key) {
  if (key.startsWith("admin-")) return isAdmin(user);
  if (key.startsWith("business-")) return isBusiness(user);
  if (key.startsWith("store-manager")) return isStoreManager(user);
  if (key === "dashboard") return isManager(user) || isStaff(user) || isDelivery(user);
  if (key === "inventory") return isManager(user) || isStaff(user);
  if (key === "stock-counts") return isManager(user);
  if (key === "receiving-pending") return isStaff(user);
  if (key === "stores") return hasPermission(user, "STORE_VIEW");
  if (key === "trips") return isDelivery(user);
  if (
    [
      "staff-damage",
      "picking",
      "staff-packing",
      "staff-counts",
      "staff-putaway",
      "receiving-process",
      "store-returns",
    ].includes(key)
  )
    return isStaff(user);
  return ["receiving-in-progress", "receiving-waiting-review", "receiving-completed", "receiving-incidents", "discrepancies", "putaway", "packing-confirmation", "dispatch-handover", "shipment-history", "damaged-goods", "warehouse-locations", "return-review"].includes(key) && isManager(user);
}
