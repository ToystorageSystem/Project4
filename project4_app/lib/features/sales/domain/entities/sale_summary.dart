/// One row of the sales history (backend SaleSummary)
class SaleSummary {
  final int id;
  final String code; // SL-<storeId>-<12 hex>
  final int totalQuantity;
  final double totalAmount;
  final String? createdByName;
  final DateTime? createdAt;

  const SaleSummary({
    required this.id,
    required this.code,
    required this.totalQuantity,
    required this.totalAmount,
    this.createdByName,
    this.createdAt,
  });
}
