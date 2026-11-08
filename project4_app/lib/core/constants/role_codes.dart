/// Must match backend roles.roles_code.
/// Backend sends authorities like "ROLE_ROLE-STORE-STAFF" (prefix "ROLE_")
/// plus permission codes like "STORE_VIEW" → use [normalize] before comparing.
class RoleCodes {
  static const admin = 'ROLE-ADMIN';
  static const warehouseManager = 'ROLE-WH-MANAGER';
  static const businessManager = 'ROLE-BIZ-MANAGER';
  static const warehouseStaff = 'ROLE-WH-STAFF';
  static const storeManager = 'ROLE-STORE-MANAGER';
  static const storeStaff = 'ROLE-STORE-STAFF';
  static const driver = 'ROLE-DRIVER';
  static const businessStaff = 'ROLE-BIZ-STAFF';

  static const _labels = {
    admin: 'Admin',
    warehouseManager: 'Warehouse manager',
    businessManager: 'Business manager',
    warehouseStaff: 'Warehouse staff',
    storeManager: 'Store manager',
    storeStaff: 'Store staff',
    driver: 'Driver',
    businessStaff: 'Business staff',
  };

  /// "ROLE_ROLE-STORE-STAFF" → "ROLE-STORE-STAFF"; "ROLE-ADMIN" → "ROLE-ADMIN"
  static String normalize(String authority) {
    final a = authority.trim();
    return a.startsWith('ROLE_') ? a.substring(5) : a;
  }

  /// true for role codes (ROLE-...), false for permissions (STORE_VIEW...)
  static bool isRole(String authority) => normalize(authority).startsWith('ROLE-');

  /// "ROLE-WH-MANAGER" → "Warehouse manager" (unknown code → shown as-is)
  static String label(String code) => _labels[normalize(code)] ?? normalize(code);
}
