import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../core/widgets/search_paged_list.dart';
import '../../domain/entities/shelf_detail.dart';
import '../providers/shelf_detail_provider.dart';
import '../widgets/shelf_product_tile.dart';

/// One shelf + the products on it. Tap a product → product detail
/// (where staff can move stock / change shelf).
class ShelfDetailScreen extends ConsumerWidget {
  final int shelfId;

  const ShelfDetailScreen({super.key, required this.shelfId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = shelfDetailProvider(shelfId);
    final detailAsync = ref.watch(provider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: Text(detailAsync.value?.name ?? 'Shelf'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => LoadErrorView(
          error: e,
          fallbackMessage: 'Could not load shelf.',
          onRetry: () => ref.invalidate(provider),
        ),
        data: (shelf) => RefreshIndicator(
          onRefresh: () => ref.refresh(provider.future),
          child: shelf.products.isEmpty
              ? ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    _ShelfHeader(shelf: shelf),
                    const SizedBox(height: 20),
                    const SectionHeader('Products on this shelf'),
                    const SizedBox(height: 10),
                    const InfoBanner(
                      message:
                          'No products on this shelf yet. Open a product '
                          '(Product lookup) and use "Assign shelf".',
                    ),
                  ],
                )
              : SearchPagedList(
                  items: shelf.products,
                  padding: const EdgeInsets.all(16),
                  hint: 'Search product, code or barcode…',
                  label: 'products',
                  header: [
                    _ShelfHeader(shelf: shelf),
                    const SizedBox(height: 20),
                    const SectionHeader('Products on this shelf'),
                    const SizedBox(height: 10),
                  ],
                  searchValues: (line) =>
                      [line.name, line.productCode, line.barcode],
                  itemBuilder: (context, line) => ShelfProductTile(
                    line: line,
                    onTap: () => context.push(
                      AppRoutes.storeProductDetail(line.productId),
                    ),
                  ),
                ),
        ),
      ),
    );
  }
}

class _ShelfHeader extends StatelessWidget {
  final ShelfDetail shelf;

  const _ShelfHeader({required this.shelf});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            shelf.name,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: AppColors.textDark,
            ),
          ),
          if (shelf.code != null)
            Text(shelf.code!, style: AppTextStyles.caption),
          if (shelf.description != null && shelf.description!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(shelf.description!, style: AppTextStyles.subtitle),
          ],
          const SizedBox(height: 14),
          Row(
            children: [
              _Number(value: shelf.products.length, label: 'Products'),
              _Number(value: shelf.totalOnShelf, label: 'Pieces'),
              _Number(
                value: shelf.lowStockCount,
                label: 'Low stock',
                color: shelf.lowStockCount > 0
                    ? AppColors.error
                    : AppColors.success,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Number extends StatelessWidget {
  final int value;
  final String label;
  final Color color;

  const _Number({
    required this.value,
    required this.label,
    this.color = AppColors.textDark,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(
            '$value',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
          Text(label, style: AppTextStyles.caption),
        ],
      ),
    );
  }
}
