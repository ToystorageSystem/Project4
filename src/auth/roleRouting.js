import { homePath, isBusiness, hasRole } from "./access";
export const ROLE = Object.freeze({ BUSINESS_OPERATIONS:"ROLE_ROLE-BIZ-OPS" });
export function hasAuthority(user, authority) { return authority === ROLE.BUSINESS_OPERATIONS ? isBusiness(user) : user?.authorities?.includes(authority) || hasRole(user, authority); }
export const getHomePath = homePath;
export const getBusinessRoleLabel = user => isBusiness(user) ? "Business Operations" : null;
