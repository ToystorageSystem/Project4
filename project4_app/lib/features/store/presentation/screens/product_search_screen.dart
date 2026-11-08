import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/barcode_scanner_page.dart';
import '../../../../core/widgets/error_banner.dart';
import '../providers/product_search_notifier.dart';
import '../providers/store_providers.dart';
import '../widgets/product_card.dart';

/// Search products of my store: by name/code, typed barcode, or camera scan
class ProductSearchScreen extends ConsumerStatefulWidget {
  const ProductSearchScreen({super.key});

  @override
  ConsumerState<ProductSearchScreen> createState() => _ProductSearchScreenState();
}

class _ProductSearchScreenState extends ConsumerState<ProductSearchScreen> {
  final _searchCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  Timer? _debounce;
  bool _lookingUp = false; // barcode lookup running

  /// A typed value made only of 8–14 digits is treated as a barcode
  static final _barcodePattern = RegExp(r'^\d{8,14}$');

  @override
  void initState() {
    super.initState();
    _scrollCtrl.addListener(_onScroll);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  // ---------- Actions ----------

  void _onScroll() {
    // Near the bottom → load next page
    if (_scrollCtrl.position.extentAfter < 300) {
      ref.read(productSearchProvider.notifier).loadMore();
    }
  }

  void _onKeywordChanged(String value) {
    setState(() {}); // show/hide clear button
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      ref.read(productSearchProvider.notifier).search(value);
    });
  }

  void _onSubmitted(String value) {
    final text = value.trim();
    if (_barcodePattern.hasMatch(text)) {
      _lookupBarcode(text);
    } else {
      _debounce?.cancel();
      ref.read(productSearchProvider.notifier).search(text);
    }
  }

  void _clearSearch() {
    _searchCtrl.clear();
    _onKeywordChanged('');
  }

  Future<void> _scan() async {
    final code = await scanBarcode(context, title: 'Scan product barcode');
    if (code != null) await _lookupBarcode(code);
  }

  Future<void> _lookupBarcode(String barcode) async {
    if (_lookingUp) return;
    final messenger = ScaffoldMessenger.of(context);
    setState(() => _lookingUp = true);
    try {
      final product =
          await ref.read(getProductByBarcodeUseCaseProvider)(barcode: barcode);
      if (!mounted) return;
      context.push(AppRoutes.storeProductDetail(product.productId));
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(
        content: Text(e.isNotFound ? 'No product with barcode $barcode' : e.message),
      ));
    } finally {
      if (mounted) setState(() => _lookingUp = false);
    }
  }

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final searchAsync = ref.watch(productSearchProvider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Product lookup'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(
            tooltip: 'Scan barcode',
            onPressed: _lookingUp ? null : _scan,
            icon: const Icon(Icons.qr_code_scanner_rounded),
          ),
        ],
        bottom: _lookingUp
            ? const PreferredSize(
                preferredSize: Size.fromHeight(3),
                child: LinearProgressIndicator(minHeight: 3),
              )
            : null,
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
            child: TextField(
              controller: _searchCtrl,
              textInputAction: TextInputAction.search,
              onChanged: _onKeywordChanged,
              onSubmitted: _onSubmitted,
              decoration: InputDecoration(
                hintText: 'Name, product code or barcode',
                prefixIcon: const Icon(Icons.search_rounded),
                suffixIcon: _searchCtrl.text.isEmpty
                    ? null
                    : IconButton(
                        icon: const Icon(Icons.close_rounded),
                        onPressed: _clearSearch,
                      ),
              ),
            ),
          ),
          Expanded(
            child: searchAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => _buildError(e),
              data: _buildList,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError(Object e) {
    final message = e is ApiException ? e.message : 'Could not load products.';
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          ErrorBanner(message: message),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () => ref.read(productSearchProvider.notifier).refresh(),
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Try again'),
          ),
        ],
      ),
    );
  }

  Widget _buildList(ProductSearchState data) {
    return RefreshIndicator(
      onRefresh: () => ref.read(productSearchProvider.notifier).refresh(),
      child: data.items.isEmpty
          ? ListView(
              children: const [
                SizedBox(height: 80),
                Center(
                  child: Text('No products found',
                      style: TextStyle(color: AppColors.textGrey)),
                ),
              ],
            )
          : ListView.separated(
              controller: _scrollCtrl,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              // +1 header (count) +1 footer (loading / error / end)
              itemCount: data.items.length + 2,
              separatorBuilder: (context, index) => const SizedBox(height: 10),
              itemBuilder: (context, i) {
                if (i == 0) {
                  return Text('${data.totalElements} products',
                      style: AppTextStyles.caption);
                }
                if (i == data.items.length + 1) return _buildFooter(data);
                final product = data.items[i - 1];
                return ProductCard(
                  product: product,
                  onTap: () =>
                      context.push(AppRoutes.storeProductDetail(product.productId)),
                );
              },
            ),
    );
  }

  Widget _buildFooter(ProductSearchState data) {
    if (data.loadingMore) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (data.loadMoreError != null) {
      return Column(
        children: [
          Text(data.loadMoreError!, style: const TextStyle(color: AppColors.error)),
          TextButton(
            onPressed: () => ref.read(productSearchProvider.notifier).loadMore(),
            child: const Text('Retry'),
          ),
        ],
      );
    }
    if (!data.hasMore) {
      return const Center(
        child: Text('End of list', style: AppTextStyles.caption),
      );
    }
    return const SizedBox(height: 24);
  }
}
