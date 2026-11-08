import '../entities/receiving_result.dart';
import '../repositories/receiving_repository.dart';

/// Store manager only: add goods to the backroom stock + discrepancy reports
class ConfirmReceivingUseCase {
  final ReceivingRepository _repository;

  const ConfirmReceivingUseCase(this._repository);

  Future<ReceivingResult> call({required String manifestCode, String? note}) {
    final clean = note?.trim();
    return _repository.confirmReceiving(
      manifestCode,
      note: (clean == null || clean.isEmpty) ? null : clean,
    );
  }
}
