import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/shelf_detail.dart';
import 'shelf_providers.dart';

/// One shelf + its products, by shelf id
final shelfDetailProvider = FutureProvider.autoDispose.family<ShelfDetail, int>(
  (ref, shelfId) => ref.read(getShelfDetailUseCaseProvider)(shelfId: shelfId),
  retry: (retryCount, error) => null,
);
