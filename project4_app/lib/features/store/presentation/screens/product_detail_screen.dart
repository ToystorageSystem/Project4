import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/store_product_detail.dart';
import '../providers/product_detail_provider.dart';

class ProductDetailScreen extends ConsumerWidget {
  final int productId;

  const ProductDetailScreen({super.key, required this.productId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(productDetailProvider(productId));

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Product detail'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ErrorBanner(
                  message: e is ApiException ? e.message : 'Could not load product.'),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () => ref.invalidate(productDetailProvider(productId)),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
        data: (p) => RefreshIndicator(
          onRefresh: () => ref.refresh(productDetailProvider(productId).future),
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              _HeaderCard(product: p),
              const SizedBox(height: 24),
              const SectionHeader('Stock in this store'),
              const SizedBox(height: 10),
              _StockRow(product: p),
              if (p.lowStock) ...[
                const SizedBox(height: 12),
                const _LowStockWarning(),
              ],
              const SizedBox(height: 24),
              const SectionHeader('Details'),
              const SizedBox(height: 10),
              _DetailCard(rows: [
                ('Shelf', p.shelf?.name ?? 'Not on a shelf'),
                ('Min. on shelf', '${p.minShelfQuantity}'),
                ('Category', p.categoryName ?? '—'),
                ('Brand', [p.brandName, p.brandCountry].whereType<String>().join(' · ')),
                ('Unit', p.baseUnit ?? '—'),
                ('Stock updated', formatDateTime(p.stockUpdatedAt)),
              ]),
            ],
          ),
        ),
      ),
    );
  }
}

class _HeaderCard extends StatelessWidget {
  final StoreProductDetail product;

  const _HeaderCard({required this.product});

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
          Text(product.name,
              style: const TextStyle(
                  fontSize: 20, fontWeight: FontWeight.w800, color: AppColors.textDark)),
          const SizedBox(height: 6),
          Text(product.productCode, style: AppTextStyles.caption),
          if (product.barcode != null)
            Text('Barcode ${product.barcode}', style: AppTextStyles.caption),
          const SizedBox(height: 12),
          Text(
            formatMoney(product.sellingPrice),
            style: const TextStyle(
                fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.primary),
          ),
        ],
      ),
    );
  }
}

class _StockRow extends StatelessWidget {
  final StoreProductDetail product;

  const _StockRow({required this.product});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _StockTile(
            label: 'On shelf',
            value: product.shelfQuantity,
            color: product.shelfQuantity > 0 ? AppColors.success : AppColors.error,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _StockTile(
            label: 'Backroom',
            value: product.backroomQuantity,
            color: AppColors.textDark,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _StockTile(
            label: 'Total',
            value: product.totalQuantity,
            color: AppColors.primary,
          ),
        ),
      ],
    );
  }
}

class _StockTile extends StatelessWidget {
  final String label;
  final int value;
  final Color color;

  const _StockTile({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          Text('$value',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, color: color)),
          const SizedBox(height: 2),
          Text(label, style: AppTextStyles.caption),
        ],
      ),
    );
  }
}

class _LowStockWarning extends StatelessWidget {
  const _LowStockWarning();

  @override
  Widget build(BuildContext context) {
    return const Row(
      children: [
        StatusChip(label: 'LOW STOCK', color: AppColors.error),
        SizedBox(width: 8),
        Expanded(
          child: Text('Shelf quantity is at or below the minimum.',
              style: AppTextStyles.caption),
        ),
      ],
    );
  }
}

class _DetailCard extends StatelessWidget {
  final List<(String, String)> rows; // (label, value)

  const _DetailCard({required this.rows});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          for (var i = 0; i < rows.length; i++)
            Container(
              padding: const EdgeInsets.symmetric(vertical: 12),
              decoration: BoxDecoration(
                border: i == rows.length - 1
                    ? null
                    : const Border(bottom: BorderSide(color: AppColors.border)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 120,
                    child: Text(rows[i].$1,
                        style: const TextStyle(fontSize: 14, color: AppColors.textGrey)),
                  ),
                  Expanded(
                    child: Text(
                      rows[i].$2.isEmpty ? '—' : rows[i].$2,
                      textAlign: TextAlign.right,
                      style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textDark),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
