import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../../../../core/widgets/search_paged_list.dart';
import '../providers/low_shelf_provider.dart';
import '../widgets/low_shelf_tile.dart';

/// "Restock shelves": products whose shelf quantity is at/below the minimum
/// and that still have stock in the backroom.
class LowShelfScreen extends ConsumerWidget {
  const LowShelfScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final listAsync = ref.watch(lowShelfProvider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Restock shelves'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: listAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => LoadErrorView(
          error: e,
          fallbackMessage: 'Could not load the restock list.',
          onRetry: () => ref.invalidate(lowShelfProvider),
        ),
        data: (items) => RefreshIndicator(
          onRefresh: () => ref.refresh(lowShelfProvider.future),
          child: SearchPagedList(
            items: items,
            padding: const EdgeInsets.all(16),
            hint: 'Search product, barcode or shelf…',
            label: 'products',
            emptyText: items.isEmpty ? '' : 'Nothing matches your search.',
            searchValues: (item) => [
              item.name,
              item.productCode,
              item.barcode,
              item.shelfName,
              item.shelfCode,
            ],
            itemBuilder: (context, item) => LowShelfTile(
              item: item,
              onTap: () =>
                  context.push(AppRoutes.storeProductDetail(item.productId)),
            ),
            header: [
              InfoBanner(
                icon: items.isEmpty
                    ? Icons.check_circle_outline_rounded
                    : Icons.info_outline_rounded,
                message: items.isEmpty
                    ? 'All shelves are above their minimum. Nothing to restock.'
                    : '${items.length} product(s) need restocking. Tap one, then '
                          'use "To shelf" to bring goods out of the backroom.',
              ),
              const SizedBox(height: 12),
            ],
          ),
        ),
      ),
    );
  }
}
