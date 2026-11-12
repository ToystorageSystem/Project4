import '../../domain/entities/package_check.dart';
import 'json_read.dart';

class PackageCheckLineModel extends PackageCheckLine {
  const PackageCheckLineModel({
    super.productId,
    super.productCode,
    required super.barcode,
    super.name,
    required super.expectedQuantity,
    super.actualQuantity,
    super.damagedQuantity,
    super.note,
  });

  factory PackageCheckLineModel.fromJson(Map<String, dynamic> json) =>
      PackageCheckLineModel(
        productId: readIntOrNull(json['productId']),
        productCode: json['productCode'] as String?,
        barcode: json['barcode'] as String? ?? '',
        name: json['name'] as String?,
        expectedQuantity: readInt(json['expectedQuantity']),
        actualQuantity: readIntOrNull(json['actualQuantity']),
        damagedQuantity: readIntOrNull(json['damagedQuantity']),
        note: json['note'] as String?,
      );
}

class PackageCheckModel extends PackageCheck {
  const PackageCheckModel({
    required super.packageId,
    required super.packageCode,
    required super.manifestCode,
    required super.status,
    required super.lines,
  });

  factory PackageCheckModel.fromJson(Map<String, dynamic> json) =>
      PackageCheckModel(
        packageId: readInt(json['packageId']),
        packageCode: json['packageCode'] as String? ?? '',
        manifestCode: json['manifestCode'] as String? ?? '',
        status: json['status'] as String? ?? '',
        lines: readList(json['lines'])
            .map(PackageCheckLineModel.fromJson)
            .toList(growable: false),
      );

  /// PUT body: { "lines": [ {barcode, actualQuantity, damagedQuantity, note} ] }
  static Map<String, dynamic> countBody(List<PackageCountLine> lines) => {
    'lines': [
      for (final l in lines)
        {
          'barcode': l.barcode,
          'actualQuantity': l.actualQuantity,
          'damagedQuantity': l.damagedQuantity,
          'note': l.note,
        },
    ],
  };
}
