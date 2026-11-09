import 'store_product.dart';

/// Product detail (backend StoreProductDetail = summary + stock details)
class StoreProductDetail extends StoreProduct {
  final int backroomQuantity;
  final int minShelfQuantity;
  final bool lowStock; // shelf != null && shelfQuantity <= minShelfQuantity
  final int? categoryId;
  final int? brandId;
  final String? brandCountry;
  final DateTime? stockUpdatedAt;

  const StoreProductDetail({
    required super.productId,
    required super.productCode,
    super.barcode,
    required super.name,
    super.imageUrl,
    super.sellingPrice,
    super.baseUnit,
    super.categoryName,
    super.brandName,
    super.shelf,
    required super.shelfQuantity,
    required super.totalQuantity,
    required this.backroomQuantity,
    required this.minShelfQuantity,
    required this.lowStock,
    this.categoryId,
    this.brandId,
    this.brandCountry,
    this.stockUpdatedAt,
  });
}
