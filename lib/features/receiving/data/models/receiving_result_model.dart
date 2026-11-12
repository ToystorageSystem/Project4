import '../../domain/entities/receiving_result.dart';
import 'json_read.dart';

class ReceivingResultItemModel extends ReceivingResultItem {
  const ReceivingResultItemModel({
    super.transferCode,
    super.productId,
    super.productCode,
    required super.name,
    required super.expected,
    required super.actual,
    required super.damaged,
    required super.shortage,
    required super.surplus,
  });

  factory ReceivingResultItemModel.fromJson(Map<String, dynamic> json) =>
      ReceivingResultItemModel(
        transferCode: json['transferCode'] as String?,
        productId: readIntOrNull(json['productId']),
        productCode: json['productCode'] as String?,
        name: json['name'] as String? ?? '',
        expected: readInt(json['expected']),
        actual: readInt(json['actual']),
        damaged: readInt(json['damaged']),
        shortage: readInt(json['shortage']),
        surplus: readInt(json['surplus']),
      );
}

class ReceivingResultModel extends ReceivingResult {
  const ReceivingResultModel({
    required super.manifestCode,
    required super.receivingStatus,
    required super.items,
    required super.totalExpected,
    required super.totalActual,
    required super.totalDamaged,
    required super.totalShortage,
    required super.totalSurplus,
    super.discrepancyReportCodes,
  });

  factory ReceivingResultModel.fromJson(Map<String, dynamic> json) =>
      ReceivingResultModel(
        manifestCode: json['manifestCode'] as String? ?? '',
        receivingStatus: json['receivingStatus'] as String? ?? '',
        items: readList(json['items'])
            .map(ReceivingResultItemModel.fromJson)
            .toList(growable: false),
        totalExpected: readInt(json['totalExpected']),
        totalActual: readInt(json['totalActual']),
        totalDamaged: readInt(json['totalDamaged']),
        totalShortage: readInt(json['totalShortage']),
        totalSurplus: readInt(json['totalSurplus']),
        discrepancyReportCodes:
            (json['discrepancyReportCodes'] as List<dynamic>? ?? const [])
                .whereType<String>()
                .toList(growable: false),
      );
}
