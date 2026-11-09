import '../../../../core/models/page_result.dart';
import '../../../../core/network/api_call.dart';
import '../../domain/entities/move_direction.dart';
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

  @override
  Future<StoreProductDetail> assignShelf({required int productId, int? shelfId}) =>
      apiCall(() => _remote.assignShelf(productId, shelfId));

  @override
  Future<StoreProductDetail> moveStock({
    required int productId,
    required MoveDirection direction,
    required int quantity,
    String? note,
  }) =>
      apiCall(() => _remote.moveStock(
            productId: productId,
            direction: direction,
            quantity: quantity,
            note: note,
          ));

  @override
  Future<StoreProductDetail> changeMinShelf({
    required int productId,
    required int minShelfQuantity,
  }) =>
      apiCall(() => _remote.changeMinShelf(productId, minShelfQuantity));
}
