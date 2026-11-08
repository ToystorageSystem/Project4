// Ma role duoc backend sinh: "ROLE_" + roles.roles_code.
// DB can co roles_code = ROLE-BIZ-OPS va tai khoan can duoc gan role nay.
export const ROLE = Object.freeze({
    BUSINESS_OPERATIONS: "ROLE_ROLE-BIZ-OPS",
});

export function hasAuthority(user, authority) {
    return Array.isArray(user?.authorities) && user.authorities.includes(authority);
}

export function getHomePath(user) {
    // Một role nghiệp vụ: không phân luồng Staff / Manager nữa.
    if (hasAuthority(user, ROLE.BUSINESS_OPERATIONS)) return "/business/dashboard";
    // Khong thay doi duong dan cua Warehouse va role khac.
    return "/warehouse/dashboard";
}

export function getBusinessRoleLabel(user) {
    return hasAuthority(user, ROLE.BUSINESS_OPERATIONS) ? "Business Operations" : null;
}
