import '../entities/manifest_detail.dart';
import '../entities/manifest_summary.dart';
import '../entities/package_check.dart';
import '../entities/receiving_enums.dart';
import '../entities/receiving_result.dart';

/// Receiving goods at the store by manifest. Backend finds the user's store.
/// Throws ApiException (400 rule broken, 403 no permission, 404, 409 conflict).
abstract interface class ReceivingRepository {
  Future<List<ManifestSummary>> getManifests();
  Future<ManifestDetail> getManifestDetail(String manifestCode);
  Future<ManifestDetail> scanPackage({
    required String manifestCode,
    required String packageCode,
    required SealStatus sealStatus,
    required PackageCondition packageCondition,
    String? notes,
  });
  Future<ManifestDetail> completeReceiving(String manifestCode);
  Future<PackageCheck> getPackageCheck(String packageCode);
  Future<PackageCheck> savePackageCheck(String packageCode, List<PackageCountLine> lines);
  Future<ReceivingResult> finishInspection(String manifestCode);
  Future<ReceivingResult> confirmReceiving(String manifestCode, {String? note});
}
