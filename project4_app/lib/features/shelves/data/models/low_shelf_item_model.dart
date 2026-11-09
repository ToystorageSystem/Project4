import '../../domain/entities/low_shelf_item.dart';

class LowShelfItemModel extends LowShelfItem {
  const LowShelfItemModel({
    required super.productId,
    required super.productCode,
    super.barcode,
    required super.name,
    super.shelfId,
    super.shelfCode,
    required super.shelfName,
    required super.shelfQuantity,
    required super.backroomQuantity,
    required super.minShelfQuantity,
  });

  factory LowShelfItemModel.fromJson(Map<String, dynamic> json) {
    final shelf = json['shelf'] is Map<String, dynamic>
        ? json['shelf'] as Map<String, dynamic>
        : const <String, dynamic>{};
    return LowShelfItemModel(
      productId: (json['productId'] as num?)?.toInt() ?? 0,
      productCode: json['productCode'] as String? ?? '',
      barcode: json['barcode'] as String?,
      name: json['name'] as String? ?? '',
      shelfId: (shelf['id'] as num?)?.toInt(),
      shelfCode: shelf['code'] as String?,
      shelfName: shelf['name'] as String? ?? '—',
      shelfQuantity: (json['shelfQuantity'] as num?)?.toInt() ?? 0,
      backroomQuantity: (json['backroomQuantity'] as num?)?.toInt() ?? 0,
      minShelfQuantity: (json['minShelfQuantity'] as num?)?.toInt() ?? 0,
    );
  }
}
