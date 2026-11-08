import '../../../../core/network/api_call.dart';
import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/manifest_summary.dart';
import '../../domain/entities/package_check.dart';
import '../../domain/entities/receiving_enums.dart';
import '../../domain/entities/receiving_result.dart';
import '../../domain/repositories/receiving_repository.dart';
import '../datasources/receiving_remote_data_source.dart';

class ReceivingRepositoryImpl implements ReceivingRepository {
  final ReceivingRemoteDataSource _remote;

  ReceivingRepositoryImpl(this._remote);

  @override
  Future<List<ManifestSummary>> getManifests() =>
      apiCall(() => _remote.getManifests());

  @override
  Future<ManifestDetail> getManifestDetail(String manifestCode) =>
      apiCall(() => _remote.getManifestDetail(manifestCode));

  @override
  Future<ManifestDetail> scanPackage({
    required String manifestCode,
    required String packageCode,
    required SealStatus sealStatus,
    required PackageCondition packageCondition,
    String? notes,
  }) =>
      apiCall(() => _remote.scanPackage(
            manifestCode: manifestCode,
            packageCode: packageCode,
            sealStatus: sealStatus.apiValue,
            packageCondition: packageCondition.apiValue,
            notes: notes,
          ));

  @override
  Future<ManifestDetail> completeReceiving(String manifestCode) =>
      apiCall(() => _remote.completeReceiving(manifestCode));

  @override
  Future<PackageCheck> getPackageCheck(String packageCode) =>
      apiCall(() => _remote.getPackageCheck(packageCode));

  @override
  Future<PackageCheck> savePackageCheck(
          String packageCode, List<PackageCountLine> lines) =>
      apiCall(() => _remote.savePackageCheck(packageCode, lines));

  @override
  Future<ReceivingResult> finishInspection(String manifestCode) =>
      apiCall(() => _remote.finishInspection(manifestCode));

  @override
  Future<ReceivingResult> confirmReceiving(String manifestCode, {String? note}) =>
      apiCall(() => _remote.confirmReceiving(manifestCode, note: note));
}
