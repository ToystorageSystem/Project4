import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../domain/entities/store_product.dart';
import 'store_providers.dart';

/// What the product list screen shows
class ProductSearchState {
  final String keyword;
  final List<StoreProduct> items;
  final int page; // last loaded page (0-based)
  final int totalElements;
  final bool hasMore;
  final bool loadingMore;
  final String? loadMoreError;

  const ProductSearchState({
    required this.keyword,
    required this.items,
    required this.page,
    required this.totalElements,
    required this.hasMore,
    this.loadingMore = false,
    this.loadMoreError,
  });

  ProductSearchState copyWith({
    List<StoreProduct>? items,
    int? page,
    bool? hasMore,
    bool? loadingMore,
    String? loadMoreError,
    bool clearLoadMoreError = false,
  }) {
    return ProductSearchState(
      keyword: keyword,
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

/// Product list with search + infinite scroll ("load more" when near bottom)
class ProductSearchNotifier extends AsyncNotifier<ProductSearchState> {
  @override
  Future<ProductSearchState> build() => _firstPage('');

  Future<ProductSearchState> _firstPage(String keyword) async {
    final result = await ref.read(searchStoreProductsUseCaseProvider)(
      keyword: keyword,
    );
    return ProductSearchState(
      keyword: keyword,
      items: result.content,
      page: result.page,
      totalElements: result.totalElements,
      hasMore: result.hasNext,
    );
  }

  /// New keyword → reload from page 0
  Future<void> search(String keyword) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => _firstPage(keyword.trim()));
  }

  /// Pull to refresh with the same keyword
  Future<void> refresh() => search(state.value?.keyword ?? '');

  /// Next page (called when the list is scrolled near the end)
  Future<void> loadMore() async {
    final current = state.value;
    if (current == null || !current.hasMore || current.loadingMore) return;

    state = AsyncData(
      current.copyWith(loadingMore: true, clearLoadMoreError: true),
    );
    try {
      final next = await ref.read(searchStoreProductsUseCaseProvider)(
        keyword: current.keyword,
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

final productSearchProvider =
    AsyncNotifierProvider.autoDispose<
      ProductSearchNotifier,
      ProductSearchState
    >(ProductSearchNotifier.new, retry: (retryCount, error) => null);
