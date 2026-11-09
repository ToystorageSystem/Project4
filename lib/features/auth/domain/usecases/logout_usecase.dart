import '../repositories/auth_repository.dart';

/// Sign out on the server and forget the local cookies (never throws)
class LogoutUseCase {
  final AuthRepository _repository;

  const LogoutUseCase(this._repository);

  Future<void> call() => _repository.logout();
}
