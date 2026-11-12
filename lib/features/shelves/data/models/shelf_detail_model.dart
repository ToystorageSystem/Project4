import '../../domain/entities/shelf_detail.dart';

class ShelfProductLineModel extends ShelfProductLine {
  const ShelfProductLineModel({
    required super.productId,
    required super.productCode,
    super.barcode,
    required super.name,
    required super.shelfQuantity,
    required super.backroomQuantity,
    required super.minShelfQuantity,
    required super.lowStock,
  });

  factory ShelfProductLineModel.fromJson(Map<String, dynamic> json) =>
      ShelfProductLineModel(
        productId: (json['productId'] as num?)?.toInt() ?? 0,
        productCode: json['productCode'] as String? ?? '',
        barcode: json['barcode'] as String?,
        name: json['name'] as String? ?? '',
        shelfQuantity: (json['shelfQuantity'] as num?)?.toInt() ?? 0,
        backroomQuantity: (json['backroomQuantity'] as num?)?.toInt() ?? 0,
        minShelfQuantity: (json['minShelfQuantity'] as num?)?.toInt() ?? 0,
        lowStock: json['lowStock'] as bool? ?? false,
      );
}

class ShelfDetailModel extends ShelfDetail {
  const ShelfDetailModel({
    required super.id,
    super.code,
    required super.name,
    super.description,
    required super.products,
  });

  factory ShelfDetailModel.fromJson(Map<String, dynamic> json) =>
      ShelfDetailModel(
        id: (json['id'] as num?)?.toInt() ?? 0,
        code: json['code'] as String?,
        name: json['name'] as String? ?? '',
        description: json['description'] as String?,
        products: (json['products'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(ShelfProductLineModel.fromJson)
            .toList(),
      );
}
