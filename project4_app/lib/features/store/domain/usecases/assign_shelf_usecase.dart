import '../../../../core/network/api_exception.dart';
import '../entities/store_product_detail.dart';
import '../repositories/store_product_repository.dart';

/// Put a product on a shelf / change its shelf / remove it from its shelf.
/// Same rules as backend:
///  - changing shelf: the goods on the shelf move with the product
///  - removing: only when shelf quantity is 0
class AssignShelfUseCase {
  final StoreProductRepository _repository;

  const AssignShelfUseCase(this._repository);

  Future<StoreProductDetail> call({
    required StoreProductDetail product,
    int? shelfId, // null = remove from shelf
  }) async {
    if (shelfId == null && product.shelf == null) return product; // nothing to do
    if (shelfId != null && shelfId == product.shelf?.id) return product; // same shelf
    if (shelfId == null && product.shelfQuantity > 0) {
      throw const ApiException('Move shelf stock to backroom first', statusCode: 400);
    }
    return _repository.assignShelf(productId: product.productId, shelfId: shelfId);
  }
}
