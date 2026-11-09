import '../../../../core/network/api_exception.dart';
import '../entities/manifest_detail.dart';
import '../entities/receiving_enums.dart';
import '../repositories/receiving_repository.dart';

/// F1: receive one package (check the seal + outside condition).
/// Same rule as backend: abnormal package → notes are required.
class ScanPackageUseCase {
  final ReceivingRepository _repository;

  const ScanPackageUseCase(this._repository);

  static bool isAbnormal(SealStatus seal, PackageCondition condition) =>
      seal != SealStatus.intact || condition != PackageCondition.good;

  Future<ManifestDetail> call({
    required String manifestCode,
    required String packageCode,
    required SealStatus sealStatus,
    required PackageCondition packageCondition,
    String? notes,
  }) {
    final cleanNotes = notes?.trim();
    if (isAbnormal(sealStatus, packageCondition) &&
        (cleanNotes == null || cleanNotes.isEmpty)) {
      throw const ApiException(
        'Notes are required for an abnormal package',
        statusCode: 400,
      );
    }
    return _repository.scanPackage(
      manifestCode: manifestCode,
      packageCode: packageCode.trim(),
      sealStatus: sealStatus,
      packageCondition: packageCondition,
      notes: (cleanNotes == null || cleanNotes.isEmpty) ? null : cleanNotes,
    );
  }
}
