import '../../../../core/constants/role_codes.dart';

/// The signed-in user (from LoginResponse / GET /api/auth/status).
/// Pure Dart, no JSON.
class UserInfo {
  final int id;
  final String? userCode;
  final String name;
  final String email;

  /// Raw authorities from backend: "ROLE_ROLE-STORE-STAFF", "STORE_VIEW"...
  final List<String> authorities;

  const UserInfo({
    required this.id,
    this.userCode,
    required this.name,
    required this.email,
    this.authorities = const [],
  });

  // ---------- Roles ----------

  /// Only role codes, normalized: ["ROLE-STORE-STAFF", ...]
  List<String> get roles => authorities
      .where(RoleCodes.isRole)
      .map(RoleCodes.normalize)
      .toSet()
      .toList(growable: false);

  bool hasRole(String code) => roles.contains(RoleCodes.normalize(code));

  /// Permission codes (no "ROLE"), e.g. STORE_VIEW
  bool hasPermission(String permission) => authorities.contains(permission);

  bool get isWarehouseStaff => hasRole(RoleCodes.warehouseStaff);
  bool get isWarehouseManager => hasRole(RoleCodes.warehouseManager);
  bool get isDriver => hasRole(RoleCodes.driver);
  bool get canManageFleet => isWarehouseManager || isStoreManager || isAdmin;
  bool get isAdmin => hasRole(RoleCodes.admin);
  bool get isStoreManager => hasRole(RoleCodes.storeManager);
  bool get isStoreStaff => hasRole(RoleCodes.storeStaff);

  /// Can use the store screens (backend also checks the user's store)
  bool get isStoreUser => isStoreManager || isStoreStaff;

  /// "Store manager, Admin"
  String get roleLabels =>
      roles.isEmpty ? '—' : roles.map(RoleCodes.label).join(', ');

  // ---------- Display ----------

  /// "Nguyen Van A" → "NA"
  String get initials {
    final source = name.trim().isNotEmpty ? name : email;
    final parts = source.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return '?';
    if (parts.length == 1) return parts.first[0].toUpperCase();
    return (parts.first[0] + parts.last[0]).toUpperCase();
  }
}
