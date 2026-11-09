/// One product placed on a shelf
class ShelfProductLine {
  final int productId;
  final String productCode;
  final String? barcode;
  final String name;
  final int shelfQuantity;
  final int backroomQuantity;
  final int minShelfQuantity;
  final bool lowStock; // shelfQuantity <= minShelfQuantity

  const ShelfProductLine({
    required this.productId,
    required this.productCode,
    this.barcode,
    required this.name,
    required this.shelfQuantity,
    required this.backroomQuantity,
    required this.minShelfQuantity,
    required this.lowStock,
  });

  /// Low on shelf AND there is something in the backroom to bring out
  bool get canRestock => lowStock && backroomQuantity > 0;
}

/// A shelf with its products (GET /api/store/shelves/{id})
class ShelfDetail {
  final int id;
  final String? code;
  final String name;
  final String? description;
  final List<ShelfProductLine> products;

  const ShelfDetail({
    required this.id,
    this.code,
    required this.name,
    this.description,
    required this.products,
  });

  int get totalOnShelf => products.fold(0, (sum, p) => sum + p.shelfQuantity);
  int get lowStockCount => products.where((p) => p.lowStock).length;
}
