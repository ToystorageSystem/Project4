import '../entities/two_factor_setup.dart';
import '../repositories/auth_repository.dart';

/// Stage SETUP_2FA: get secret + QR data (each call = NEW secret)
class SetupTwoFactorUseCase {
  final AuthRepository _repository;

  const SetupTwoFactorUseCase(this._repository);

  Future<TwoFactorSetup> call({required String accountEmail}) =>
      _repository.setupTwoFactor(accountEmail: accountEmail);
}
