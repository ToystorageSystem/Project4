import 'package:dio/dio.dart';

import '../../../../core/constants/api_constants.dart';
import '../../domain/entities/package_check.dart';
import '../models/json_read.dart';
import '../models/manifest_detail_model.dart';
import '../models/manifest_summary_model.dart';
import '../models/package_check_model.dart';
import '../models/receiving_result_model.dart';

/// /api/store/receiving/** (POST/PUT get the CSRF header from the interceptor)
class ReceivingRemoteDataSource {
  final Dio _dio;

  ReceivingRemoteDataSource(this._dio);

  Future<List<ManifestSummaryModel>> getManifests() async {
    final res = await _dio.get<List<dynamic>>(ApiConstants.manifests);
    return readList(res.data).map(ManifestSummaryModel.fromJson).toList();
  }

  Future<ManifestDetailModel> getManifestDetail(String manifestCode) async {
    final res = await _dio.get<Map<String, dynamic>>(
        ApiConstants.manifest(manifestCode));
    return ManifestDetailModel.fromJson(res.data!);
  }

  Future<ManifestDetailModel> scanPackage({
    required String manifestCode,
    required String packageCode,
    required String sealStatus,
    required String packageCondition,
    String? notes,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      ApiConstants.scanPackage(manifestCode),
      data: {
        'packageCode': packageCode,
        'sealStatus': sealStatus,
        'packageCondition': packageCondition,
        'notes': notes,
      },
    );
    return ManifestDetailModel.fromJson(res.data!);
  }

  Future<ManifestDetailModel> completeReceiving(String manifestCode) async {
    final res = await _dio.post<Map<String, dynamic>>(
        ApiConstants.completeReceiving(manifestCode));
    return ManifestDetailModel.fromJson(res.data!);
  }

  Future<PackageCheckModel> getPackageCheck(String packageCode) async {
    final res = await _dio.get<Map<String, dynamic>>(
        ApiConstants.packageItems(packageCode));
    return PackageCheckModel.fromJson(res.data!);
  }

  Future<PackageCheckModel> savePackageCheck(
      String packageCode, List<PackageCountLine> lines) async {
    final res = await _dio.put<Map<String, dynamic>>(
      ApiConstants.packageItems(packageCode),
      data: PackageCheckModel.countBody(lines),
    );
    return PackageCheckModel.fromJson(res.data!);
  }

  Future<ReceivingResultModel> finishInspection(String manifestCode) async {
    final res = await _dio.post<Map<String, dynamic>>(
        ApiConstants.finishInspection(manifestCode));
    return ReceivingResultModel.fromJson(res.data!);
  }

  Future<ReceivingResultModel> confirmReceiving(String manifestCode,
      {String? note}) async {
    final res = await _dio.post<Map<String, dynamic>>(
      ApiConstants.confirmReceiving(manifestCode),
      data: {'note': note},
    );
    return ReceivingResultModel.fromJson(res.data!);
  }
}
