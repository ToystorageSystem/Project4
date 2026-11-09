import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../shelves/presentation/providers/shelf_list_provider.dart';
import '../../domain/entities/store_product_detail.dart';
import 'sheet_handle.dart';

/// Result of the sheet. shelfId == null → "remove from shelf".
/// (The sheet itself returns null when cancelled.)
class ShelfChoice {
  final int? shelfId;
  final String? shelfName;

  const ShelfChoice(this.shelfId, this.shelfName);
}

Future<ShelfChoice?> showAssignShelfSheet(
  BuildContext context, {
  required StoreProductDetail product,
}) {
  return showAppBottomSheet<ShelfChoice>(
    context,
    (_) => _AssignShelfSheet(product: product),
  );
}

class _AssignShelfSheet extends ConsumerWidget {
  final StoreProductDetail product;

  const _AssignShelfSheet({required this.product});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final shelvesAsync = ref.watch(shelfListProvider);
    final current = product.shelf;
    final canRemove = current != null && product.shelfQuantity == 0;

    return ConstrainedBox(
      constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(context).height * 0.8),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SheetHandle(),
            const SizedBox(height: 16),
            Text(current == null ? 'Assign shelf' : 'Change shelf',
                style: const TextStyle(
                    fontSize: 20, fontWeight: FontWeight.w800, color: AppColors.textDark)),
            Text(product.name, style: AppTextStyles.caption),
            if (current != null && product.shelfQuantity > 0) ...[
              const SizedBox(height: 12),
              InfoBanner(
                message: 'The ${product.shelfQuantity} pcs on ${current.name} '
                    'move together with the product.',
              ),
            ],
            const SizedBox(height: 12),
            Flexible(
              child: shelvesAsync.when(
                loading: () => const Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (e, _) => ErrorBanner(message: '$e'),
                data: (shelves) => shelves.isEmpty
                    ? const InfoBanner(message: 'This store has no active shelves.')
                    : ListView(
                        shrinkWrap: true,
                        children: [
                          for (final s in shelves)
                            ListTile(
                              contentPadding: EdgeInsets.zero,
                              leading: Icon(
                                s.id == current?.id
                                    ? Icons.radio_button_checked_rounded
                                    : Icons.radio_button_off_rounded,
                                color: s.id == current?.id
                                    ? AppColors.primary
                                    : AppColors.textGrey,
                              ),
                              title: Text(s.name),
                              subtitle: Text(
                                  '${s.productCount} products · ${s.shelfQuantity} pcs'),
                              trailing: s.id == current?.id
                                  ? const Text('Current', style: AppTextStyles.caption)
                                  : null,
                              onTap: s.id == current?.id
                                  ? null
                                  : () => Navigator.of(context)
                                      .pop(ShelfChoice(s.id, s.name)),
                            ),
                        ],
                      ),
              ),
            ),
            if (current != null) ...[
              const Divider(height: 24),
              TextButton.icon(
                style: TextButton.styleFrom(foregroundColor: AppColors.error),
                onPressed: canRemove
                    ? () => Navigator.of(context).pop(const ShelfChoice(null, null))
                    : null,
                icon: const Icon(Icons.remove_circle_outline_rounded),
                label: const Text('Remove from shelf'),
              ),
              if (!canRemove)
                const Text(
                  'To remove, first move all shelf stock to the backroom.',
                  textAlign: TextAlign.center,
                  style: AppTextStyles.caption,
                ),
            ],
          ],
        ),
      ),
    );
  }
}
