import '../../../../core/network/api_exception.dart';
import '../entities/package_check.dart';
import '../repositories/receiving_repository.dart';

/// F2: send ALL lines of a package at once (backend replaces old counts).
/// Same rules as backend: damaged ≤ actual, no duplicate barcode.
class SavePackageCheckUseCase {
  final ReceivingRepository _repository;

  const SavePackageCheckUseCase(this._repository);

  Future<PackageCheck> call({
    required String packageCode,
    required List<PackageCountLine> lines,
  }) {
    final seen = <String>{};
    for (final line in lines) {
      if (line.actualQuantity < 0 || line.damagedQuantity < 0) {
        throw ApiException('Quantities cannot be negative: ${line.barcode}',
            statusCode: 400);
      }
      if (line.damagedQuantity > line.actualQuantity) {
        throw ApiException(
            'Damaged quantity cannot exceed actual quantity: ${line.barcode}',
            statusCode: 400);
      }
      if (!seen.add(line.barcode)) {
        throw ApiException('Duplicate barcode in lines: ${line.barcode}',
            statusCode: 400);
      }
    }
    return _repository.savePackageCheck(packageCode, lines);
  }
}
