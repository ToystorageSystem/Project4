import 'shelf_info.dart';

/// One row of the product list (backend StoreProductSummary)
class StoreProduct {
  final int productId;
  final String productCode;
  final String? barcode;
  final String name;
  final String? imageUrl; // always null for now (backend does not map it yet)
  final double? sellingPrice;
  final String? baseUnit;
  final String? categoryName;
  final String? brandName;
  final ShelfInfo? shelf; // null = not placed on a shelf
  final int shelfQuantity;
  final int totalQuantity; // shelf + backroom in this store

  const StoreProduct({
    required this.productId,
    required this.productCode,
    this.barcode,
    required this.name,
    this.imageUrl,
    this.sellingPrice,
    this.baseUnit,
    this.categoryName,
    this.brandName,
    this.shelf,
    required this.shelfQuantity,
    required this.totalQuantity,
  });

  bool get isOutOfStock => totalQuantity <= 0;
  bool get onShelf => shelf != null;
}
