import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../domain/entities/sale_summary.dart';
import 'sale_providers.dart';

/// What the sales history screen shows
class SaleHistoryState {
  final List<SaleSummary> items;
  final int page; // last loaded page (0-based)
  final int totalElements;
  final bool hasMore;
  final bool loadingMore;
  final String? loadMoreError;

  const SaleHistoryState({
    required this.items,
    required this.page,
    required this.totalElements,
    required this.hasMore,
    this.loadingMore = false,
    this.loadMoreError,
  });

  SaleHistoryState copyWith({
    List<SaleSummary>? items,
    int? page,
    bool? hasMore,
    bool? loadingMore,
    String? loadMoreError,
    bool clearLoadMoreError = false,
  }) {
    return SaleHistoryState(
      items: items ?? this.items,
      page: page ?? this.page,
      totalElements: totalElements,
      hasMore: hasMore ?? this.hasMore,
      loadingMore: loadingMore ?? this.loadingMore,
      loadMoreError: clearLoadMoreError
          ? null
          : (loadMoreError ?? this.loadMoreError),
    );
  }
}

/// Sales history with infinite scroll
class SaleHistoryNotifier extends AsyncNotifier<SaleHistoryState> {
  @override
  Future<SaleHistoryState> build() async {
    final result = await ref.read(getSalesUseCaseProvider)();
    return SaleHistoryState(
      items: result.content,
      page: result.page,
      totalElements: result.totalElements,
      hasMore: result.hasNext,
    );
  }

  /// Next page (called when the list is scrolled near the end)
  Future<void> loadMore() async {
    final current = state.value;
    if (current == null || !current.hasMore || current.loadingMore) return;

    state = AsyncData(
      current.copyWith(loadingMore: true, clearLoadMoreError: true),
    );
    try {
      final next = await ref.read(getSalesUseCaseProvider)(
        page: current.page + 1,
      );
      if (!ref.mounted) return;
      state = AsyncData(
        current.copyWith(
          items: [...current.items, ...next.content],
          page: next.page,
          hasMore: next.hasNext,
          loadingMore: false,
        ),
      );
    } on ApiException catch (e) {
      if (!ref.mounted) return;
      state = AsyncData(
        current.copyWith(loadingMore: false, loadMoreError: e.message),
      );
    }
  }
}

final saleHistoryProvider =
    AsyncNotifierProvider.autoDispose<SaleHistoryNotifier, SaleHistoryState>(
      SaleHistoryNotifier.new,
      retry: (retryCount, error) => null,
    );
