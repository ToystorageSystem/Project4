import 'receiving_enums.dart';

/// One incoming manifest (backend ManifestSummaryResponse)
class ManifestSummary {
  final int manifestId;
  final String manifestCode;
  final String? shipmentCode;
  final String? deliveryStatus; // IN_TRANSIT / ARRIVED / DELIVERED...
  final String? fromWarehouseName;
  final DateTime? expectedDeliveryAt;
  final int totalPackages;
  final int receivedPackages;
  final int checkedPackages;
  final String receivingStatus; // see ReceivingStatus
  final bool canReceive;

  const ManifestSummary({
    required this.manifestId,
    required this.manifestCode,
    this.shipmentCode,
    this.deliveryStatus,
    this.fromWarehouseName,
    this.expectedDeliveryAt,
    required this.totalPackages,
    required this.receivedPackages,
    required this.checkedPackages,
    required this.receivingStatus,
    required this.canReceive,
  });

  bool get isScanning =>
      receivingStatus == ReceivingStatus.notStarted ||
      receivingStatus == ReceivingStatus.receiving;
  bool get isCounting => receivingStatus == ReceivingStatus.packagesReceived;
  bool get isInspected => receivingStatus == ReceivingStatus.inspected;
  bool get isCompleted => receivingStatus == ReceivingStatus.completed;
  int get missingPackages => totalPackages - receivedPackages;

  /// The truck is at the store: packages can be received
  bool get hasArrived =>
      deliveryStatus == 'ARRIVED' || deliveryStatus == 'DELIVERED';
}
