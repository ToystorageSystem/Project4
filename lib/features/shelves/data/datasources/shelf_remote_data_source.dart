import 'package:dio/dio.dart';

import '../../../../core/constants/api_constants.dart';
import '../models/low_shelf_item_model.dart';
import '../models/shelf_detail_model.dart';
import '../models/shelf_summary_model.dart';

/// GET /api/store/shelves, /shelves/{id}, /products/low-shelf
class ShelfRemoteDataSource {
  final Dio _dio;

  ShelfRemoteDataSource(this._dio);

  Future<List<ShelfSummaryModel>> getShelves() async {
    final res = await _dio.get<List<dynamic>>(ApiConstants.storeShelves);
    return (res.data ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(ShelfSummaryModel.fromJson)
        .toList();
  }

  Future<ShelfDetailModel> getShelf(int shelfId) async {
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.storeShelf(shelfId),
    );
    return ShelfDetailModel.fromJson(res.data!);
  }

  Future<List<LowShelfItemModel>> getLowShelf() async {
    final res = await _dio.get<List<dynamic>>(ApiConstants.lowShelf);
    return (res.data ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(LowShelfItemModel.fromJson)
        .toList();
  }
}
