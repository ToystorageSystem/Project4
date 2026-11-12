import '../../lib/core/constants/role_codes.dart';
import '../../lib/core/utils/validators.dart';
import '../../lib/features/auth/domain/entities/user_info.dart';

void check(bool value, String message) {
  if (!value) throw StateError(message);
}

void main() {
  for (final code in [
    'ROLE_WAREHOUSE_STAFF',
    'ROLE_ROLE_WH_STAFF',
    'ROLE-WH-STAFF',
  ]) {
    final user = UserInfo(
      id: 1,
      name: 'Staff',
      email: 'staff@example.com',
      authorities: [code],
    );
    check(user.isWarehouseStaff, 'warehouse role $code');
    check(!user.isStoreUser, 'no cross-role store access');
  }
  for (final code in ['ROLE_DELIVERY_STAFF', 'ROLE_ROLE-DRIVER', 'DRIVER']) {
    final user = UserInfo(
      id: 1,
      name: 'Driver',
      email: 'driver@example.com',
      authorities: [code],
    );
    check(user.isDriver, 'driver role $code');
    check(!user.canManageFleet, 'driver is not dispatcher');
  }
  check(
    RoleCodes.normalize('ROLE_ROLE-STORE-MANAGER') == RoleCodes.storeManager,
    'store manager alias',
  );
  check(!RoleCodes.isRole('STORE_VIEW'), 'permission is not role');
  check(Validators.newPassword('Ab12!x') == null, 'six-character password');
  check(Validators.newPassword('abc12') != null, 'short password');
  check(Validators.newPassword('😀' * 20) != null, 'BCrypt byte limit');
  check(
    Validators.newPassword('abcdef', currentPassword: 'abcdef') != null,
    'new differs from current',
  );
  print('PASS: role aliases, role isolation and password contracts');
}
