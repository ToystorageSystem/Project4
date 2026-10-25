// Backend: "ROLE_" + roles.roles_code (see CustomUserDetailsService).
// Keep these exact values in sync with MySQL roles.roles_code.
export const ROLE = Object.freeze({
    BUSINESS_STAFF: "ROLE_ROLE-BIZ-STAFF",
    BUSINESS_MANAGER: "ROLE_ROLE-BIZ-MANAGER",
});

export function hasAuthority(user, authority) {
    return Array.isArray(user?.authorities) &&
        user.authorities.includes(authority);
}

export function getHomePath(user) {
    // Multi-role account: manager landing page takes precedence.
    if (hasAuthority(user, ROLE.BUSINESS_MANAGER)) {
        return "/business-manager/dashboard";
    }
    if (hasAuthority(user, ROLE.BUSINESS_STAFF)) {
        return "/business-staff/dashboard";
    }
    // Do not alter the existing landing page of other teams in step 1.
    return "/warehouse/dashboard";
}

export function getBusinessRoleLabel(user) {
    if (hasAuthority(user, ROLE.BUSINESS_MANAGER)) return "Business Manager";
    if (hasAuthority(user, ROLE.BUSINESS_STAFF)) return "Business Staff";
    return null;
}
