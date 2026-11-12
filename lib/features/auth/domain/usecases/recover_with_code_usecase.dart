import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Stage VERIFY_2FA without the phone: one of the saved recovery codes
class RecoverWithCodeUseCase {
  final AuthRepository _repository;

  const RecoverWithCodeUseCase(this._repository);

  Future<AuthSession> call({required String recoveryCode}) async =>
      await _repository.recoverWithCode(recoveryCode.trim()) ??
      await _repository.getStatus();
}
