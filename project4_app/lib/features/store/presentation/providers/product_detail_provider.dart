import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shelves/presentation/providers/shelf_refresh.dart';
import '../../domain/entities/move_direction.dart';
import '../../domain/entities/store_product_detail.dart';
import 'store_providers.dart';

/// Detail of one product + shelf actions on it. Family: one notifier per id
/// (Riverpod 3: the argument comes through the constructor).
/// Every action returns the updated product → state = that product.
/// Errors (ApiException) go back to the screen.
class ProductDetailNotifier extends AsyncNotifier<StoreProductDetail> {
  ProductDetailNotifier(this.productId);

  final int productId;

  @override
  Future<StoreProductDetail> build() =>
      ref.read(getStoreProductDetailUseCaseProvider)(productId: productId);

  StoreProductDetail get _current => state.requireValue;

  /// shelfId null = remove from its shelf
  Future<void> assignShelf(int? shelfId) async {
    _set(await ref.read(assignShelfUseCaseProvider)(
      product: _current,
      shelfId: shelfId,
    ));
  }

  Future<void> moveStock({
    required MoveDirection direction,
    required int quantity,
    String? note,
  }) async {
    _set(await ref.read(moveStockUseCaseProvider)(
      product: _current,
      direction: direction,
      quantity: quantity,
      note: note,
    ));
  }

  /// Store manager only (UI hides it for staff, backend checks too)
  Future<void> changeMinShelf(int minShelfQuantity) async {
    _set(await ref.read(changeMinShelfUseCaseProvider)(
      product: _current,
      minShelfQuantity: minShelfQuantity,
    ));
  }

  void _set(StoreProductDetail updated) {
    if (!ref.mounted) return;
    state = AsyncData(updated);
    refreshShelfData(ref); // shelf list / shelf detail / low-shelf are now stale
  }
}

final productDetailProvider = AsyncNotifierProvider.autoDispose
    .family<ProductDetailNotifier, StoreProductDetail, int>(
  ProductDetailNotifier.new,
  retry: (retryCount, error) => null,
);
