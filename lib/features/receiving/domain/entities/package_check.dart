/// One product line of a package (F2 counting)
class PackageCheckLine {
  final int? productId;
  final String? productCode;
  final String barcode;
  final String? name;
  final int expectedQuantity; // 0 = surplus line (not in the package list)
  final int? actualQuantity; // null = not counted yet
  final int? damagedQuantity;
  final String? note;

  const PackageCheckLine({
    this.productId,
    this.productCode,
    required this.barcode,
    this.name,
    required this.expectedQuantity,
    this.actualQuantity,
    this.damagedQuantity,
    this.note,
  });

  bool get isSurplusLine => expectedQuantity == 0;
}

/// GET/PUT /packages/{code}/items
class PackageCheck {
  final int packageId;
  final String packageCode;
  final String manifestCode;
  final String status;
  final List<PackageCheckLine> lines;

  const PackageCheck({
    required this.packageId,
    required this.packageCode,
    required this.manifestCode,
    required this.status,
    required this.lines,
  });
}

/// What the app sends for one line (PUT body item)
class PackageCountLine {
  final String barcode;
  final int actualQuantity;
  final int damagedQuantity;
  final String? note;

  const PackageCountLine({
    required this.barcode,
    required this.actualQuantity,
    required this.damagedQuantity,
    this.note,
  });
}
