import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Stage VERIFY_2FA: 6-digit code from Google Authenticator
class VerifyOtpUseCase {
  final AuthRepository _repository;

  const VerifyOtpUseCase(this._repository);

  Future<AuthSession> call({required String code}) async =>
      await _repository.verifyTwoFactor(code.trim()) ??
      await _repository.getStatus();
}
