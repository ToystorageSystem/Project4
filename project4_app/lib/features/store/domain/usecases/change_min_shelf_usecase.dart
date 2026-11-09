import '../../../../core/network/api_exception.dart';
import '../entities/store_product_detail.dart';
import '../repositories/store_product_repository.dart';

/// Store manager: change the minimum quantity that should stay on the shelf.
/// shelfQuantity <= min → "low stock" (needs restocking).
class ChangeMinShelfUseCase {
  static const maxValue = 100000;

  final StoreProductRepository _repository;

  const ChangeMinShelfUseCase(this._repository);

  Future<StoreProductDetail> call({
    required StoreProductDetail product,
    required int minShelfQuantity,
  }) async {
    if (minShelfQuantity < 0 || minShelfQuantity > maxValue) {
      throw const ApiException('Minimum must be between 0 and $maxValue',
          statusCode: 400);
    }
    if (minShelfQuantity == product.minShelfQuantity) return product;
    return _repository.changeMinShelf(
      productId: product.productId,
      minShelfQuantity: minShelfQuantity,
    );
  }
}
