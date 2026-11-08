import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/manifest_summary.dart';
import 'receiving_providers.dart';

/// Incoming manifests of my store (pull to refresh = ref.refresh)
final manifestListProvider = FutureProvider.autoDispose<List<ManifestSummary>>(
  (ref) => ref.read(getManifestsUseCaseProvider)(),
  retry: (retryCount, error) => null,
);
