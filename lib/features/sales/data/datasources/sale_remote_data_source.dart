import 'package:dio/dio.dart';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/models/page_result.dart';
import '../../domain/entities/cart_item.dart';
import '../models/sale_detail_model.dart';
import '../models/sale_summary_model.dart';

/// /api/store/sales (POST gets the CSRF header from the interceptor)
class SaleRemoteDataSource {
  final Dio _dio;

  SaleRemoteDataSource(this._dio);

  /// 201 Created + SaleDetail
  Future<SaleDetailModel> createSale(List<SaleLine> lines, String? note) async {
    final res = await _dio.post<Map<String, dynamic>>(
      ApiConstants.storeSales,
      data: SaleDetailModel.createBody(lines, note),
    );
    return SaleDetailModel.fromJson(res.data!);
  }

  Future<PageResult<SaleSummaryModel>> getSales({
    required int page,
    required int size,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeSales,
      queryParameters: {'page': page, 'size': size},
    );
    final data = res.data ?? const <String, dynamic>{};
    final content = (data['content'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(SaleSummaryModel.fromJson)
        .toList();
    return PageResult<SaleSummaryModel>(
      content: content,
      page: (data['page'] as num?)?.toInt() ?? page,
      size: (data['size'] as num?)?.toInt() ?? size,
      totalElements: (data['totalElements'] as num?)?.toInt() ?? content.length,
      totalPages: (data['totalPages'] as num?)?.toInt() ?? 1,
    );
  }

  Future<SaleDetailModel> getSale(int saleId) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeSale(saleId),
    );
    return SaleDetailModel.fromJson(res.data!);
  }
}
