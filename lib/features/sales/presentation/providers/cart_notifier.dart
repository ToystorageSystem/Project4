import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shelves/presentation/providers/shelf_refresh.dart';
import '../../../store/domain/entities/store_product.dart';
import '../../../store/presentation/providers/product_detail_provider.dart';
import '../../domain/entities/cart_item.dart';
import '../../domain/entities/sale_detail.dart';
import '../../domain/usecases/create_sale_usecase.dart';
import 'sale_history_notifier.dart';
import 'sale_providers.dart';

/// The cart of the "New sale" screen
class CartState {
  final List<CartItem> items;

  const CartState({this.items = const []});

  bool get isEmpty => items.isEmpty;
  int get totalQuantity => items.fold(0, (sum, i) => sum + i.quantity);
  double get totalAmount => items.fold(0, (sum, i) => sum + i.lineTotal);

  int quantityOf(int productId) {
    for (final i in items) {
      if (i.productId == productId) return i.quantity;
    }
    return 0;
  }
}

/// Add / change / remove products, then checkout.
/// The same product is always ONE line (backend rejects duplicates).
class CartNotifier extends Notifier<CartState> {
  @override
  CartState build() => const CartState();

  /// Max pieces of one line: stock when added, and the backend limit 1000
  static int _maxFor(CartItem item) =>
      item.available < CreateSaleUseCase.maxQuantity
      ? item.available
      : CreateSaleUseCase.maxQuantity;

  /// Add 1 piece. Returns the message to show (added / out of stock / limit).
  String add(StoreProduct p) {
    final items = [...state.items];
    final index = items.indexWhere((i) => i.productId == p.productId);

    if (index >= 0) {
      final item = items[index];
      if (item.quantity >= _maxFor(item)) {
        return 'Only ${_maxFor(item)} pcs of ${p.name} can be sold';
      }
      items[index] = item.copyWith(quantity: item.quantity + 1);
      state = CartState(items: items);
      return '${p.name} × ${item.quantity + 1}';
    }

    if (p.totalQuantity <= 0) return '${p.name} is out of stock';
    if (items.length >= CreateSaleUseCase.maxLines) {
      return 'A sale can have at most ${CreateSaleUseCase.maxLines} products';
    }
    items.add(
      CartItem(
        productId: p.productId,
        productCode: p.productCode,
        barcode: p.barcode,
        name: p.name,
        unitPrice: p.sellingPrice,
        available: p.totalQuantity,
        quantity: 1,
      ),
    );
    state = CartState(items: items);
    return 'Added ${p.name}';
  }

  /// Set quantity (limited to 1..max). 0 or less = remove the line.
  void setQuantity(int productId, int quantity) {
    if (quantity <= 0) {
      remove(productId);
      return;
    }
    state = CartState(
      items: [
        for (final i in state.items)
          if (i.productId == productId)
            i.copyWith(quantity: quantity > _maxFor(i) ? _maxFor(i) : quantity)
          else
            i,
      ],
    );
  }

  void remove(int productId) {
    state = CartState(
      items: state.items.where((i) => i.productId != productId).toList(),
    );
  }

  void clear() => state = const CartState();

  /// POST /api/store/sales. Success → empty cart, stock pages reload.
  /// Error (ApiException) → the cart is kept so the user can fix it.
  Future<SaleDetail> checkout({String? note}) async {
    final sale = await ref.read(createSaleUseCaseProvider)(
      lines: [
        for (final i in state.items)
          SaleLine(productId: i.productId, quantity: i.quantity),
      ],
      note: note,
    );
    if (!ref.mounted) return sale;
    state = const CartState();
    refreshShelfData(ref); // shelf quantities changed
    ref.invalidate(productDetailProvider); // every product detail is stale
    ref.invalidate(saleHistoryProvider);
    return sale;
  }
}

/// autoDispose: leaving the "New sale" screen empties the cart
final cartProvider = NotifierProvider.autoDispose<CartNotifier, CartState>(
  CartNotifier.new,
);
