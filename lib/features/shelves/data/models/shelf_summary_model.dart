import '../../domain/entities/shelf_summary.dart';

class ShelfSummaryModel extends ShelfSummary {
  const ShelfSummaryModel({
    required super.id,
    super.code,
    required super.name,
    super.description,
    required super.productCount,
    required super.shelfQuantity,
  });

  factory ShelfSummaryModel.fromJson(Map<String, dynamic> json) =>
      ShelfSummaryModel(
        id: (json['id'] as num?)?.toInt() ?? 0,
        code: json['code'] as String?,
        name: json['name'] as String? ?? '',
        description: json['description'] as String?,
        productCount: (json['productCount'] as num?)?.toInt() ?? 0,
        shelfQuantity: (json['shelfQuantity'] as num?)?.toInt() ?? 0,
      );
}
