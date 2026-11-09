import '../entities/auth_session.dart';
import '../entities/two_factor_setup.dart';

/// Auth with backend SESSION (cookie) + CSRF. Implemented by AuthRepositoryImpl.
/// Every method throws ApiException on failure.
/// Methods returning `AuthSession?` → null when the backend answered
/// without session info (caller then asks GET /api/auth/status).
abstract interface class AuthRepository {
  Future<AuthSession> login(String email, String password);
  Future<AuthSession?> verifyTwoFactor(String code);
  Future<AuthSession?> recoverWithCode(String recoveryCode);
  Future<AuthSession> getStatus();
  Future<AuthSession?> changePassword(
    String currentPassword,
    String newPassword,
  );
  Future<TwoFactorSetup> setupTwoFactor({required String accountEmail});
  Future<AuthSession?> confirmTwoFactor(String code);

  /// Tell the server, then forget cookies + CSRF token (never throws)
  Future<void> logout();
}
