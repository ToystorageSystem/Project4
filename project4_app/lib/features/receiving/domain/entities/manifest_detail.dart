import 'manifest_summary.dart';
import 'receiving_enums.dart';

/// One package of a manifest
class ReceivingPackage {
  final int packageId;
  final String packageCode;
  final String? sealNumber;
  final String? transferCode;
  final String status; // see PackageStatus
  final String? sealStatus; // INTACT / BROKEN / MISSING
  final String? packageCondition; // GOOD / DENTED / ...
  final String? notes;
  final int lineCount; // number of different products
  final int pieces; // total quantity of items

  const ReceivingPackage({
    required this.packageId,
    required this.packageCode,
    this.sealNumber,
    this.transferCode,
    required this.status,
    this.sealStatus,
    this.packageCondition,
    this.notes,
    required this.lineCount,
    required this.pieces,
  });

  bool get isReceived => status != PackageStatus.notReceived;
  bool get isChecked => status == PackageStatus.checked;
  bool get isAbnormal =>
      (sealStatus != null && sealStatus != SealStatus.intact.apiValue) ||
      (packageCondition != null && packageCondition != PackageCondition.good.apiValue);
}

/// Store receipt created on the first scan (one per stock transfer)
class StoreReceiptInfo {
  final int receiptId;
  final String receiptCode;
  final String? transferCode;
  final String? status;
  final DateTime? receivedAt;

  const StoreReceiptInfo({
    required this.receiptId,
    required this.receiptCode,
    this.transferCode,
    this.status,
    this.receivedAt,
  });
}

/// GET /manifests/{code} → { manifest, packages, receipts }
class ManifestDetail {
  final ManifestSummary manifest;
  final List<ReceivingPackage> packages;
  final List<StoreReceiptInfo> receipts;

  const ManifestDetail({
    required this.manifest,
    required this.packages,
    required this.receipts,
  });

  /// F2 can be finished when every received package is CHECKED
  bool get allReceivedPackagesChecked =>
      packages.where((p) => p.isReceived).every((p) => p.isChecked);

  int get uncheckedReceivedCount =>
      packages.where((p) => p.isReceived && !p.isChecked).length;
}
