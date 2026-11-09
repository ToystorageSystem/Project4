import '../../../../core/network/api_exception.dart';
import '../entities/move_direction.dart';
import '../entities/store_product_detail.dart';
import '../repositories/store_product_repository.dart';

/// Move goods backroom → shelf or shelf → backroom. Same rules as backend.
class MoveStockUseCase {
  static const maxQuantity = 100000;
  static const maxNoteLength = 255;

  final StoreProductRepository _repository;

  const MoveStockUseCase(this._repository);

  /// How many pieces can be moved in this direction
  static int available(StoreProductDetail p, MoveDirection direction) =>
      direction == MoveDirection.toShelf ? p.backroomQuantity : p.shelfQuantity;

  Future<StoreProductDetail> call({
    required StoreProductDetail product,
    required MoveDirection direction,
    required int quantity,
    String? note,
  }) async {
    if (direction == MoveDirection.toShelf && product.shelf == null) {
      throw const ApiException('Product has no shelf', statusCode: 400);
    }
    if (quantity < 1 || quantity > maxQuantity) {
      throw const ApiException('Quantity must be between 1 and $maxQuantity',
          statusCode: 400);
    }
    if (quantity > available(product, direction)) {
      throw ApiException(
        direction == MoveDirection.toShelf
            ? 'Not enough stock in backroom'
            : 'Not enough stock on shelf',
        statusCode: 400,
      );
    }
    final cleanNote = note?.trim();
    if (cleanNote != null && cleanNote.length > maxNoteLength) {
      throw const ApiException('Note must be at most $maxNoteLength characters',
          statusCode: 400);
    }
    return _repository.moveStock(
      productId: product.productId,
      direction: direction,
      quantity: quantity,
      note: (cleanNote == null || cleanNote.isEmpty) ? null : cleanNote,
    );
  }
}
