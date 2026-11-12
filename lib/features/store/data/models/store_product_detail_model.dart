import '../../domain/entities/store_product_detail.dart';
import 'shelf_info_model.dart';

class StoreProductDetailModel extends StoreProductDetail {
  const StoreProductDetailModel({
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
    required super.backroomQuantity,
    required super.minShelfQuantity,
    required super.lowStock,
    super.categoryId,
    super.brandId,
    super.brandCountry,
    super.stockUpdatedAt,
  });

  factory StoreProductDetailModel.fromJson(Map<String, dynamic> json) =>
      StoreProductDetailModel(
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
        backroomQuantity: (json['backroomQuantity'] as num?)?.toInt() ?? 0,
        minShelfQuantity: (json['minShelfQuantity'] as num?)?.toInt() ?? 0,
        lowStock: json['lowStock'] as bool? ?? false,
        categoryId: (json['categoryId'] as num?)?.toInt(),
        brandId: (json['brandId'] as num?)?.toInt(),
        brandCountry: json['brandCountry'] as String?,
        stockUpdatedAt: json['stockUpdatedAt'] is String
            ? DateTime.tryParse(json['stockUpdatedAt'] as String)
            : null,
      );
}
