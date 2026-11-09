/// One ACTIVE shelf of my store (GET /api/store/shelves)
class ShelfSummary {
  final int id;
  final String? code;
  final String name;
  final String? description;
  final int productCount; // how many products are placed on it
  final int shelfQuantity; // total pieces on it

  const ShelfSummary({
    required this.id,
    this.code,
    required this.name,
    this.description,
    required this.productCount,
    required this.shelfQuantity,
  });

  bool get isEmpty => productCount == 0;
}
