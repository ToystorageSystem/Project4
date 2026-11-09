import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'low_shelf_provider.dart';
import 'shelf_detail_provider.dart';
import 'shelf_list_provider.dart';

/// Call from a Notifier after stock / shelf placement changed
/// (move, assign shelf, min-shelf, sale) → shelf pages reload next time.
/// Invalidating a family (shelfDetailProvider) reloads every shelf id.
void refreshShelfData(Ref ref) {
  ref.invalidate(shelfListProvider);
  ref.invalidate(shelfDetailProvider);
  ref.invalidate(lowShelfProvider);
}
