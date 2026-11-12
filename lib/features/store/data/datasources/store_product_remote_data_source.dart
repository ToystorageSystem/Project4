import 'package:dio/dio.dart';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/models/page_result.dart';
import '../../domain/entities/move_direction.dart';
import '../models/store_product_detail_model.dart';
import '../models/store_product_model.dart';

class StoreProductRemoteDataSource {
  final Dio _dio;

  StoreProductRemoteDataSource(this._dio);

  Future<PageResult<StoreProductModel>> searchProducts({
    required String keyword,
    int? categoryId,
    required int page,
    required int size,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeProducts,
      queryParameters: {
        if (keyword.isNotEmpty) 'keyword': keyword,
        'categoryId': ?categoryId, // null-aware element: skipped when null
        'page': page,
        'size': size,
      },
    );
    final data = res.data ?? const <String, dynamic>{};
    final content = (data['content'] as List<dynamic>? ?? const [])
        .map((e) => StoreProductModel.fromJson(e as Map<String, dynamic>))
        .toList();
    return PageResult<StoreProductModel>(
      content: content,
      page: (data['page'] as num?)?.toInt() ?? page,
      size: (data['size'] as num?)?.toInt() ?? size,
      totalElements: (data['totalElements'] as num?)?.toInt() ?? content.length,
      totalPages: (data['totalPages'] as num?)?.toInt() ?? 1,
    );
  }

  Future<StoreProductDetailModel> getByBarcode(String barcode) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeProductByBarcode(barcode),
    );
    return StoreProductDetailModel.fromJson(res.data!);
  }

  Future<StoreProductDetailModel> getDetail(int productId) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeProduct(productId),
    );
    return StoreProductDetailModel.fromJson(res.data!);
  }

  // ---------- Shelf placement (PUT/POST: CSRF header added by interceptor) ----------

  Future<StoreProductDetailModel> assignShelf(
    int productId,
    int? shelfId,
  ) async {
    final res = await _dio.put<Map<String, dynamic>>(
      ApiConstants.assignShelf(productId),
      data: {'shelfId': shelfId}, // null = remove from shelf
    );
    return StoreProductDetailModel.fromJson(res.data!);
  }

  Future<StoreProductDetailModel> moveStock({
    required int productId,
    required MoveDirection direction,
    required int quantity,
    String? note,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      ApiConstants.moveStock(productId),
      data: {
        'direction': direction.apiValue,
        'quantity': quantity,
        'note': ?note,
      },
    );
    return StoreProductDetailModel.fromJson(res.data!);
  }

  Future<StoreProductDetailModel> changeMinShelf(
    int productId,
    int minShelfQuantity,
  ) async {
    final res = await _dio.put<Map<String, dynamic>>(
      ApiConstants.minShelf(productId),
      data: {'minShelfQuantity': minShelfQuantity},
    );
    return StoreProductDetailModel.fromJson(res.data!);
  }
}
