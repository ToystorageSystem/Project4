/// Seal of a package when it arrives (F1)
enum SealStatus {
  intact('INTACT', 'Intact'),
  broken('BROKEN', 'Broken'),
  missing('MISSING', 'Missing');

  const SealStatus(this.apiValue, this.label);
  final String apiValue;
  final String label;
}

/// Outside condition of a package (F1)
enum PackageCondition {
  good('GOOD', 'Good'),
  dented('DENTED', 'Dented'),
  torn('TORN', 'Torn'),
  wet('WET', 'Wet'),
  damaged('DAMAGED', 'Damaged');

  const PackageCondition(this.apiValue, this.label);
  final String apiValue;
  final String label;
}

/// Manifest receiving status (computed by backend)
class ReceivingStatus {
  static const notStarted = 'NOT_STARTED';
  static const receiving = 'RECEIVING';
  static const packagesReceived = 'PACKAGES_RECEIVED';
  static const inspected = 'INSPECTED';
  static const completed = 'COMPLETED';
}

/// Package status inside a manifest
class PackageStatus {
  static const notReceived = 'NOT_RECEIVED';
  static const received = 'RECEIVED'; // seal checked (F1)
  static const checked = 'CHECKED'; // every product counted (F2)
}
