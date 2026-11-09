import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/shelf_summary.dart';
import 'shelf_providers.dart';

/// All ACTIVE shelves of my store (also used by the "choose shelf" sheet)
final shelfListProvider = FutureProvider.autoDispose<List<ShelfSummary>>(
  (ref) => ref.read(getShelvesUseCaseProvider)(),
  retry: (retryCount, error) => null,
);
