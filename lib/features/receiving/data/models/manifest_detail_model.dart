import '../../domain/entities/manifest_detail.dart';
import 'json_read.dart';
import 'manifest_summary_model.dart';

class ReceivingPackageModel extends ReceivingPackage {
  const ReceivingPackageModel({
    required super.packageId,
    required super.packageCode,
    super.sealNumber,
    super.transferCode,
    required super.status,
    super.sealStatus,
    super.packageCondition,
    super.notes,
    required super.lineCount,
    required super.pieces,
  });

  factory ReceivingPackageModel.fromJson(Map<String, dynamic> json) =>
      ReceivingPackageModel(
        packageId: readInt(json['packageId']),
        packageCode: json['packageCode'] as String? ?? '',
        sealNumber: json['sealNumber'] as String?,
        transferCode: json['transferCode'] as String?,
        status: json['status'] as String? ?? 'NOT_RECEIVED',
        sealStatus: json['sealStatus'] as String?,
        packageCondition: json['packageCondition'] as String?,
        notes: json['notes'] as String?,
        lineCount: readInt(json['lineCount']),
        pieces: readInt(json['pieces']),
      );
}

class StoreReceiptInfoModel extends StoreReceiptInfo {
  const StoreReceiptInfoModel({
    required super.receiptId,
    required super.receiptCode,
    super.transferCode,
    super.status,
    super.receivedAt,
  });

  factory StoreReceiptInfoModel.fromJson(Map<String, dynamic> json) =>
      StoreReceiptInfoModel(
        receiptId: readInt(json['receiptId']),
        receiptCode: json['receiptCode'] as String? ?? '',
        transferCode: json['transferCode'] as String?,
        status: json['status'] as String?,
        receivedAt: readDate(json['receivedAt']),
      );
}

/// { manifest: {...}, packages: [...], receipts: [...] }
class ManifestDetailModel extends ManifestDetail {
  const ManifestDetailModel({
    required super.manifest,
    required super.packages,
    required super.receipts,
  });

  factory ManifestDetailModel.fromJson(Map<String, dynamic> json) =>
      ManifestDetailModel(
        manifest: ManifestSummaryModel.fromJson(
          json['manifest'] as Map<String, dynamic>? ?? const {},
        ),
        packages: readList(json['packages'])
            .map(ReceivingPackageModel.fromJson)
            .toList(growable: false),
        receipts: readList(json['receipts'])
            .map(StoreReceiptInfoModel.fromJson)
            .toList(growable: false),
      );
}
