import '../entities/manifest_detail.dart';
import '../repositories/receiving_repository.dart';

class GetManifestDetailUseCase {
  final ReceivingRepository _repository;

  const GetManifestDetailUseCase(this._repository);

  Future<ManifestDetail> call({required String manifestCode}) =>
      _repository.getManifestDetail(manifestCode);
}
