import '../entities/manifest_summary.dart';
import '../repositories/receiving_repository.dart';

/// Manifests coming to my store (not COMPLETED yet)
class GetManifestsUseCase {
  final ReceivingRepository _repository;

  const GetManifestsUseCase(this._repository);

  Future<List<ManifestSummary>> call() => _repository.getManifests();
}
