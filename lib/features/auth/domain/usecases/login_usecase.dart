import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Email + password → session with a stage (what to do next)
class LoginUseCase {
  final AuthRepository _repository;

  const LoginUseCase(this._repository);

  Future<AuthSession> call({required String email, required String password}) =>
      _repository.login(email.trim(), password);
}
