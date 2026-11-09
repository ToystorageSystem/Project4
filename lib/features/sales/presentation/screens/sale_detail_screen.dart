import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../../../../core/widgets/section_header.dart';
import '../../domain/entities/sale_detail.dart';
import '../providers/sale_detail_provider.dart';
import '../widgets/sale_line_tile.dart';

/// Receipt of one sale. [initialSale] comes from checkout (shown at once,
/// no extra request); from the history it is loaded by id.
class SaleDetailScreen extends ConsumerWidget {
  final int saleId;
  final SaleDetail? initialSale;

  const SaleDetailScreen({super.key, required this.saleId, this.initialSale});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final justSold = initialSale != null;

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Receipt'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: justSold
          ? _Receipt(sale: initialSale!, justSold: true)
          : ref
                .watch(saleDetailProvider(saleId))
                .when(
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => LoadErrorView(
                    error: e,
                    fallbackMessage: 'Could not load the sale.',
                    onRetry: () => ref.invalidate(saleDetailProvider(saleId)),
                  ),
                  data: (sale) => _Receipt(sale: sale, justSold: false),
                ),
    );
  }
}

class _Receipt extends StatelessWidget {
  final SaleDetail sale;
  final bool justSold;

  const _Receipt({required this.sale, required this.justSold});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (justSold) ...[
          const InfoBanner(
            icon: Icons.check_circle_outline_rounded,
            message:
                'Sale completed. Stock was taken from the shelf first, '
                'then from the backroom.',
          ),
          const SizedBox(height: 12),
        ],
        _Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                sale.code,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textDark,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                formatDateTime(sale.createdAt),
                style: AppTextStyles.caption,
              ),
              if (sale.createdByName != null)
                Text(
                  'Sold by ${sale.createdByName}',
                  style: AppTextStyles.caption,
                ),
              if (sale.note != null && sale.note!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text('Note: ${sale.note}', style: AppTextStyles.subtitle),
              ],
            ],
          ),
        ),
        const SizedBox(height: 20),
        const SectionHeader('Items'),
        const SizedBox(height: 10),
        _Card(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: Column(
            children: [
              for (var i = 0; i < sale.items.length; i++)
                SaleLineTile(
                  line: sale.items[i],
                  showDivider: i < sale.items.length - 1,
                ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _Card(
          child: Row(
            children: [
              Expanded(
                child: Text(
                  'Total · ${sale.totalQuantity} pcs',
                  style: AppTextStyles.cardTitle,
                ),
              ),
              Text(
                formatMoney(sale.totalAmount),
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
        ),
        if (justSold) ...[
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => context.pop(), // back to the (now empty) cart
            icon: const Icon(Icons.add_shopping_cart_rounded),
            label: const Text('New sale'),
          ),
        ],
      ],
    );
  }
}

class _Card extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;

  const _Card({required this.child, this.padding = const EdgeInsets.all(16)});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: child,
    );
  }
}
