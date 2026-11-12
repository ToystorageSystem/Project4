import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/receiving_enums.dart';
import '../../domain/entities/receiving_result.dart';
import 'manifest_list_provider.dart';
import 'receiving_providers.dart';

/// One manifest + the actions on it. Family: one notifier per manifestCode
/// (Riverpod 3: the argument comes through the constructor).
class ManifestDetailNotifier extends AsyncNotifier<ManifestDetail> {
  ManifestDetailNotifier(this.manifestCode);

  final String manifestCode;

  @override
  Future<ManifestDetail> build() =>
      ref.read(getManifestDetailUseCaseProvider)(manifestCode: manifestCode);

  /// F1 – receive one package
  Future<void> scanPackage({
    required String packageCode,
    required SealStatus sealStatus,
    required PackageCondition packageCondition,
    String? notes,
  }) async {
    final detail = await ref.read(scanPackageUseCaseProvider)(
      manifestCode: manifestCode,
      packageCode: packageCode,
      sealStatus: sealStatus,
      packageCondition: packageCondition,
      notes: notes,
    );
    _set(detail);
  }

  /// End of F1 (packages not scanned = missing)
  Future<void> completeReceiving() async {
    _set(
      await ref.read(completeReceivingUseCaseProvider)(
        manifestCode: manifestCode,
      ),
    );
  }

  /// End of F2 → result; reload the manifest (status becomes INSPECTED)
  Future<ReceivingResult> finishInspection() async {
    final result = await ref.read(finishInspectionUseCaseProvider)(
      manifestCode: manifestCode,
    );
    _reload();
    return result;
  }

  /// Store manager: add to stock (manifest becomes COMPLETED)
  Future<ReceivingResult> confirm({String? note}) async {
    final result = await ref.read(confirmReceivingUseCaseProvider)(
      manifestCode: manifestCode,
      note: note,
    );
    _reload();
    return result;
  }

  void _set(ManifestDetail detail) {
    if (!ref.mounted) return;
    state = AsyncData(detail);
    ref.invalidate(manifestListProvider); // counts on the list changed
  }

  void _reload() {
    if (!ref.mounted) return;
    ref.invalidateSelf();
    ref.invalidate(manifestListProvider);
  }
}

final manifestDetailProvider = AsyncNotifierProvider.autoDispose
    .family<ManifestDetailNotifier, ManifestDetail, String>(
      ManifestDetailNotifier.new,
      retry: (retryCount, error) => null,
    );
