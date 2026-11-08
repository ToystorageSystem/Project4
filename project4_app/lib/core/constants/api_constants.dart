/// All backend paths + backend messages the app must recognise.
/// Backend: Spring Boot, SESSION cookie (JSESSIONID) + CSRF header.
class ApiConstants {
  // Android emulator: 10.0.2.2 = localhost of your computer.
  // Real phone: your computer's LAN IP, e.g. http://192.168.1.10:8080
  // Cookies are stored per host → always use ONE baseUrl.
  static const baseUrl = 'http://10.0.2.2:8080';

  // ---------- Auth ----------
  static const csrf = '/api/auth/csrf';
  static const login = '/api/auth/login';
  static const status = '/api/auth/status';
  static const logout = '/api/auth/logout';
  static const changePassword = '/api/auth/change-password';
  static const twoFactorVerify = '/api/auth/2fa/verify';
  static const twoFactorRecover = '/api/auth/2fa/recover';
  static const twoFactorSetup = '/api/auth/2fa/setup';
  static const twoFactorConfirm = '/api/auth/2fa/confirm';

  /// Auth endpoints that answer 401 as part of the normal flow
  /// (wrong password, wrong stage...). A 401 here must NOT be treated
  /// as "session expired".
  static const authFlowPaths = {
    csrf,
    login,
    status,
    logout,
    twoFactorVerify,
    twoFactorRecover,
  };

  // ---------- Store: product lookup ----------
  static const storeProducts = '/api/store/products';
  static String storeProduct(int id) => '/api/store/products/$id';
  static String storeProductByBarcode(String barcode) =>
      '/api/store/products/barcode/${Uri.encodeComponent(barcode.trim())}';

  // ---------- Store: receiving ----------
  static const _receiving = '/api/store/receiving';
  static const manifests = '$_receiving/manifests';
  static String manifest(String code) =>
      '$_receiving/manifests/${Uri.encodeComponent(code)}';
  static String scanPackage(String manifestCode) =>
      '${manifest(manifestCode)}/packages/scan';
  static String completeReceiving(String manifestCode) =>
      '${manifest(manifestCode)}/complete-receiving';
  static String finishInspection(String manifestCode) =>
      '${manifest(manifestCode)}/finish-inspection';
  static String confirmReceiving(String manifestCode) =>
      '${manifest(manifestCode)}/confirm';
  static String packageItems(String packageCode) =>
      '$_receiving/packages/${Uri.encodeComponent(packageCode)}/items';

  // ---------- Backend messages with special meaning ----------
  static const msgAccessDenied = 'Access denied';
  static const msgNotAssignedToStore = 'User is not assigned to a store';
}
