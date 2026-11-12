import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'receiving_providers.dart';

/// A received package that is not counted yet (suggestion in Package Inspection)
class UncountedPackage {
  final String packageCode;
  final String manifestCode;
  final int lineCount;
  final int pieces;

  const UncountedPackage({
    required this.packageCode,
    required this.manifestCode,
    required this.lineCount,
    required this.pieces,
  });
}

/// Packages waiting to be counted in my store:
/// manifests whose package receiving is completed (PACKAGES_RECEIVED),
/// packages received but not CHECKED yet.
final uncountedPackagesProvider =
    FutureProvider.autoDispose<List<UncountedPackage>>((ref) async {
      final manifests = await ref.read(getManifestsUseCaseProvider)();
      final counting = manifests.where((m) => m.isCounting).toList();
      final details = await Future.wait(
        counting.map(
          (m) => ref.read(getManifestDetailUseCaseProvider)(
            manifestCode: m.manifestCode,
          ),
        ),
      );
      return [
        for (final d in details)
          for (final p in d.packages)
            if (p.isReceived && !p.isChecked)
              UncountedPackage(
                packageCode: p.packageCode,
                manifestCode: d.manifest.manifestCode,
                lineCount: p.lineCount,
                pieces: p.pieces,
              ),
      ];
    }, retry: (retryCount, error) => null);
