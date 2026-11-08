import '../../../../core/models/page_result.dart';
import '../../../../core/network/api_call.dart';
import '../../domain/entities/store_product.dart';
import '../../domain/entities/store_product_detail.dart';
import '../../domain/repositories/store_product_repository.dart';
import '../datasources/store_product_remote_data_source.dart';

class StoreProductRepositoryImpl implements StoreProductRepository {
  final StoreProductRemoteDataSource _remote;

  StoreProductRepositoryImpl(this._remote);

  @override
  Future<PageResult<StoreProduct>> searchProducts({
    required String keyword,
    int? categoryId,
    required int page,
    required int size,
  }) =>
      apiCall(() => _remote.searchProducts(
            keyword: keyword,
            categoryId: categoryId,
            page: page,
            size: size,
          ));

  @override
  Future<StoreProductDetail> getByBarcode(String barcode) =>
      apiCall(() => _remote.getByBarcode(barcode));

  @override
  Future<StoreProductDetail> getDetail(int productId) =>
      apiCall(() => _remote.getDetail(productId));
}
