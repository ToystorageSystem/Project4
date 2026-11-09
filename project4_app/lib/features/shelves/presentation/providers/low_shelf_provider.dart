import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/low_shelf_item.dart';
import 'shelf_providers.dart';

/// Products to restock (Home badge + "Restock shelves" screen).
/// Invalidate after anything that changes stock (move, sale, receiving).
final lowShelfProvider = FutureProvider.autoDispose<List<LowShelfItem>>(
  (ref) => ref.read(getLowShelfUseCaseProvider)(),
  retry: (retryCount, error) => null,
);
