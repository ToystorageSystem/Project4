import '../../../../core/network/api_exception.dart';
import '../entities/auth_session.dart';
import '../repositories/auth_repository.dart';

/// Is there a live session? null = not signed in (401/403).
class GetAuthStatusUseCase {
  final AuthRepository _repository;

  const GetAuthStatusUseCase(this._repository);

  Future<AuthSession?> call() async {
    try {
      return await _repository.getStatus();
    } on ApiException catch (e) {
      if (e.isUnauthorized || e.isForbidden) return null;
      rethrow;
    }
  }
}
