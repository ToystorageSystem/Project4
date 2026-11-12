/// One product line of a sale. fromShelf + fromBackroom = quantity
/// (backend takes from the shelf first, then the backroom).
class SaleItemLine {
  final int productId;
  final String productCode;
  final String? barcode;
  final String name;
  final int quantity;
  final double unitPrice; // selling price at the moment of the sale
  final double lineTotal;
  final int fromShelf;
  final int fromBackroom;

  const SaleItemLine({
    required this.productId,
    required this.productCode,
    this.barcode,
    required this.name,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
    required this.fromShelf,
    required this.fromBackroom,
  });
}

/// A sale (receipt) – backend SaleDetail
class SaleDetail {
  final int id;
  final String code;
  final int totalQuantity;
  final double totalAmount;
  final String? note;
  final String? createdByName;
  final DateTime? createdAt;
  final List<SaleItemLine> items;

  const SaleDetail({
    required this.id,
    required this.code,
    required this.totalQuantity,
    required this.totalAmount,
    this.note,
    this.createdByName,
    this.createdAt,
    required this.items,
  });
}
