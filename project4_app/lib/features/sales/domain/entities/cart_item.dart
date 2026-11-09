/// One product in the cart (app only – not saved on the server until checkout)
class CartItem {
  final int productId;
  final String productCode;
  final String? barcode;
  final String name;
  final double? unitPrice; // current selling price (backend uses its own at sale time)
  final int available; // shelf + backroom when the product was added
  final int quantity;

  const CartItem({
    required this.productId,
    required this.productCode,
    this.barcode,
    required this.name,
    this.unitPrice,
    required this.available,
    required this.quantity,
  });

  double get lineTotal => (unitPrice ?? 0) * quantity;

  CartItem copyWith({int? quantity}) => CartItem(
        productId: productId,
        productCode: productCode,
        barcode: barcode,
        name: name,
        unitPrice: unitPrice,
        available: available,
        quantity: quantity ?? this.quantity,
      );
}

/// What is sent to POST /api/store/sales for one product
class SaleLine {
  final int productId;
  final int quantity;

  const SaleLine({required this.productId, required this.quantity});
}
