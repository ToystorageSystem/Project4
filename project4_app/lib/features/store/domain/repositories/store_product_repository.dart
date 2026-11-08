import '../../../../core/models/page_result.dart';
import '../entities/store_product.dart';
import '../entities/store_product_detail.dart';

/// Products of the signed-in user's store (backend finds the store itself).
/// Throws ApiException: 403 "Access denied" / "User is not assigned to a store",
/// 404 "Product not found".
abstract interface class StoreProductRepository {
  Future<PageResult<StoreProduct>> searchProducts({
    required String keyword,
    int? categoryId,
    required int page,
    required int size,
  });

  Future<StoreProductDetail> getByBarcode(String barcode);

  Future<StoreProductDetail> getDetail(int productId);
}
