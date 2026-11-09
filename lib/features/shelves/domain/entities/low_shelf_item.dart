/// Product that needs restocking: on a shelf, shelf <= min, backroom > 0
/// (GET /api/store/products/low-shelf)
class LowShelfItem {
  final int productId;
  final String productCode;
  final String? barcode;
  final String name;
  final int? shelfId;
  final String? shelfCode;
  final String shelfName;
  final int shelfQuantity;
  final int backroomQuantity;
  final int minShelfQuantity;

  const LowShelfItem({
    required this.productId,
    required this.productCode,
    this.barcode,
    required this.name,
    this.shelfId,
    this.shelfCode,
    required this.shelfName,
    required this.shelfQuantity,
    required this.backroomQuantity,
    required this.minShelfQuantity,
  });

  /// Pieces to bring out so the shelf is above the minimum again
  /// (limited by what the backroom has)
  int get suggestedRefill {
    final need = minShelfQuantity - shelfQuantity + 1;
    if (need <= 0) return 0;
    return need > backroomQuantity ? backroomQuantity : need;
  }
}
