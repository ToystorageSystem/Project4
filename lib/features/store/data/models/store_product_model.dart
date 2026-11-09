import '../../domain/entities/store_product.dart';
import 'shelf_info_model.dart';

class StoreProductModel extends StoreProduct {
  const StoreProductModel({
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
  });

  factory StoreProductModel.fromJson(Map<String, dynamic> json) =>
      StoreProductModel(
        productId: (json['productId'] as num).toInt(),
        productCode: json['productCode'] as String? ?? '',
        barcode: json['barcode'] as String?,
        name: json['name'] as String? ?? '',
        imageUrl: json['imageUrl'] as String?,
        sellingPrice: (json['sellingPrice'] as num?)?.toDouble(),
        baseUnit: json['baseUnit'] as String?,
        categoryName: json['categoryName'] as String?,
        brandName: json['brandName'] as String?,
        shelf: ShelfInfoModel.tryFromJson(json['shelf']),
        shelfQuantity: (json['shelfQuantity'] as num?)?.toInt() ?? 0,
        totalQuantity: (json['totalQuantity'] as num?)?.toInt() ?? 0,
      );
}
