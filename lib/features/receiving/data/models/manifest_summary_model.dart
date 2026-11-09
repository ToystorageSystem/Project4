import '../../domain/entities/manifest_summary.dart';
import 'json_read.dart';

class ManifestSummaryModel extends ManifestSummary {
  const ManifestSummaryModel({
    required super.manifestId,
    required super.manifestCode,
    super.shipmentCode,
    super.deliveryStatus,
    super.fromWarehouseName,
    super.expectedDeliveryAt,
    required super.totalPackages,
    required super.receivedPackages,
    required super.checkedPackages,
    required super.receivingStatus,
    required super.canReceive,
  });

  factory ManifestSummaryModel.fromJson(Map<String, dynamic> json) =>
      ManifestSummaryModel(
        manifestId: readInt(json['manifestId']),
        manifestCode: json['manifestCode'] as String? ?? '',
        shipmentCode: json['shipmentCode'] as String?,
        deliveryStatus: json['deliveryStatus'] as String?,
        fromWarehouseName: json['fromWarehouseName'] as String?,
        expectedDeliveryAt: readDate(json['expectedDeliveryAt']),
        totalPackages: readInt(json['totalPackages']),
        receivedPackages: readInt(json['receivedPackages']),
        checkedPackages: readInt(json['checkedPackages']),
        receivingStatus: json['receivingStatus'] as String? ?? 'NOT_STARTED',
        canReceive: json['canReceive'] as bool? ?? false,
      );
}
