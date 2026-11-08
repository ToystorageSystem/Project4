import '../../../../core/network/api_exception.dart';
import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Returns the new session, or null = password changed but the server
/// ended the session → user must sign in again with the new password.
class ChangePasswordUseCase {
  final AuthRepository _repository;

  const ChangePasswordUseCase(this._repository);

  Future<AuthSession?> call({
    required String currentPassword,
    required String newPassword,
  }) async {
    final session = await _repository.changePassword(currentPassword, newPassword);
    if (session != null) return session;
    try {
      return await _repository.getStatus();
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await _repository.logout();
        return null;
      }
      rethrow;
    }
  }
}
