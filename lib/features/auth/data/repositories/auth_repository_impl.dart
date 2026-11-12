import '../../../../core/network/api_call.dart';
import '../../../../core/network/session_store.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/entities/two_factor_setup.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_data_source.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remote;
  final SessionStore _session; // local side: cookies + CSRF token

  AuthRepositoryImpl(this._remote, this._session);

  @override
  Future<AuthSession> login(String email, String password) async {
    // Start clean: old cookies of another account must not be reused
    await _session.clear();
    return apiCall(() => _remote.login(email, password));
  }

  @override
  Future<AuthSession?> verifyTwoFactor(String code) =>
      apiCall(() => _remote.verifyTwoFactor(code));

  @override
  Future<AuthSession?> recoverWithCode(String recoveryCode) =>
      apiCall(() => _remote.recoverWithCode(recoveryCode));

  @override
  Future<AuthSession> getStatus() => apiCall(() => _remote.getStatus());

  @override
  Future<AuthSession?> changePassword(
    String currentPassword,
    String newPassword,
  ) => apiCall(() => _remote.changePassword(currentPassword, newPassword));

  @override
  Future<TwoFactorSetup> setupTwoFactor({required String accountEmail}) =>
      apiCall(() => _remote.setupTwoFactor(accountEmail: accountEmail));

  @override
  Future<AuthSession?> confirmTwoFactor(String code) =>
      apiCall(() => _remote.confirmTwoFactor(code));

  @override
  Future<void> logout() async {
    try {
      await apiCall(() => _remote.logout());
    } catch (_) {
      // Server may already have ended the session – ignore
    } finally {
      await _session.clear();
    }
  }
}
