import '../../domain/entities/sale_summary.dart';
import 'sale_json.dart';

class SaleSummaryModel extends SaleSummary {
  const SaleSummaryModel({
    required super.id,
    required super.code,
    required super.totalQuantity,
    required super.totalAmount,
    super.createdByName,
    super.createdAt,
  });

  factory SaleSummaryModel.fromJson(Map<String, dynamic> json) => SaleSummaryModel(
        id: readInt(json['id']),
        code: json['code'] as String? ?? '',
        totalQuantity: readInt(json['totalQuantity']),
        totalAmount: readMoney(json['totalAmount']),
        createdByName: json['createdByName'] as String?,
        createdAt: readDate(json['createdAt']),
      );
}
