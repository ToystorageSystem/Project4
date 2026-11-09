import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../providers/shelf_list_provider.dart';
import '../widgets/shelf_card.dart';

/// Shelves of my store
class ShelfListScreen extends ConsumerWidget {
  const ShelfListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final listAsync = ref.watch(shelfListProvider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Shelves'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(
            tooltip: 'Restock shelves',
            onPressed: () => context.push(AppRoutes.lowShelf),
            icon: const Icon(Icons.inventory_outlined),
          ),
        ],
      ),
      body: listAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => LoadErrorView(
          error: e,
          fallbackMessage: 'Could not load shelves.',
          onRetry: () => ref.invalidate(shelfListProvider),
        ),
        data: (shelves) => RefreshIndicator(
          onRefresh: () => ref.refresh(shelfListProvider.future),
          child: shelves.isEmpty
              ? ListView(
                  children: const [
                    SizedBox(height: 80),
                    Icon(Icons.shelves, size: 48, color: AppColors.textGrey),
                    SizedBox(height: 8),
                    Center(
                      child: Text(
                        'This store has no active shelves',
                        style: TextStyle(color: AppColors.textGrey),
                      ),
                    ),
                  ],
                )
              : ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                  itemCount: shelves.length + 1, // +1 header
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: 10),
                  itemBuilder: (context, i) {
                    if (i == 0) {
                      final pieces = shelves.fold<int>(
                        0,
                        (sum, s) => sum + s.shelfQuantity,
                      );
                      return Text(
                        '${shelves.length} shelves · $pieces pcs on shelves',
                        style: AppTextStyles.caption,
                      );
                    }
                    final shelf = shelves[i - 1];
                    return ShelfCard(
                      shelf: shelf,
                      onTap: () =>
                          context.push(AppRoutes.storeShelfDetail(shelf.id)),
                    );
                  },
                ),
        ),
      ),
    );
  }
}
