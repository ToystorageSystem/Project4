import '../entities/manifest_detail.dart';
import '../repositories/receiving_repository.dart';

/// End of F1: packages not scanned are counted as missing
class CompleteReceivingUseCase {
  final ReceivingRepository _repository;

  const CompleteReceivingUseCase(this._repository);

  Future<ManifestDetail> call({required String manifestCode}) =>
      _repository.completeReceiving(manifestCode);
}
