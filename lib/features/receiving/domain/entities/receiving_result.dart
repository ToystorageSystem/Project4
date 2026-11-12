/// One product in the inspection result
class ReceivingResultItem {
  final String? transferCode;
  final int? productId;
  final String? productCode;
  final String name;
  final int expected;
  final int actual;
  final int damaged;
  final int shortage;
  final int surplus;

  const ReceivingResultItem({
    this.transferCode,
    this.productId,
    this.productCode,
    required this.name,
    required this.expected,
    required this.actual,
    required this.damaged,
    required this.shortage,
    required this.surplus,
  });

  bool get hasDifference => shortage > 0 || surplus > 0 || damaged > 0;
}

/// finish-inspection / confirm response
class ReceivingResult {
  final String manifestCode;
  final String receivingStatus;
  final List<ReceivingResultItem> items;
  final int totalExpected;
  final int totalActual;
  final int totalDamaged;
  final int totalShortage;
  final int totalSurplus;
  final List<String> discrepancyReportCodes; // only after confirm

  const ReceivingResult({
    required this.manifestCode,
    required this.receivingStatus,
    required this.items,
    required this.totalExpected,
    required this.totalActual,
    required this.totalDamaged,
    required this.totalShortage,
    required this.totalSurplus,
    this.discrepancyReportCodes = const [],
  });
}
