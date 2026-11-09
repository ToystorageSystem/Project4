import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Stage SETUP_2FA: first code from the app → 2FA ON (+ recovery codes)
class ConfirmTwoFactorUseCase {
  final AuthRepository _repository;

  const ConfirmTwoFactorUseCase(this._repository);

  Future<AuthSession> call({required String code}) async =>
      await _repository.confirmTwoFactor(code.trim()) ??
      await _repository.getStatus();
}
