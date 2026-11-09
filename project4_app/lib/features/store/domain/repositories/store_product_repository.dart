import '../../../../core/models/page_result.dart';
import '../entities/move_direction.dart';
import '../entities/store_product.dart';
import '../entities/store_product_detail.dart';

/// Products of the signed-in user's store (backend finds the store itself).
/// Throws ApiException: 403 "Access denied" / "User is not assigned to a store",
/// 404 "Product not found", 409 changed by someone else.
abstract interface class StoreProductRepository {
  Future<PageResult<StoreProduct>> searchProducts({
    required String keyword,
    int? categoryId,
    required int page,
    required int size,
  });

  Future<StoreProductDetail> getByBarcode(String barcode);

  Future<StoreProductDetail> getDetail(int productId);

  // ---------- Shelf placement (all return the updated product) ----------

  /// PUT /products/{id}/shelf – shelfId null = remove from its shelf
  Future<StoreProductDetail> assignShelf({required int productId, int? shelfId});

  /// POST /products/{id}/move – backroom ↔ shelf
  Future<StoreProductDetail> moveStock({
    required int productId,
    required MoveDirection direction,
    required int quantity,
    String? note,
  });

  /// PUT /products/{id}/min-shelf – store manager only
  Future<StoreProductDetail> changeMinShelf({
    required int productId,
    required int minShelfQuantity,
  });
}
