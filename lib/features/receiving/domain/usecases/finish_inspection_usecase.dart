import '../entities/receiving_result.dart';
import '../repositories/receiving_repository.dart';

/// End of F2 for the whole manifest → result (shortage / surplus / damaged)
class FinishInspectionUseCase {
  final ReceivingRepository _repository;

  const FinishInspectionUseCase(this._repository);

  Future<ReceivingResult> call({required String manifestCode}) =>
      _repository.finishInspection(manifestCode);
}
