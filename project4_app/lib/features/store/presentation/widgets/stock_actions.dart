import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../domain/entities/store_product_detail.dart';

/// Buttons on the product detail: assign/change shelf, move stock, (manager) min
class StockActions extends StatelessWidget {
  final StoreProductDetail product;
  final bool isManager;
  final bool busy;
  final VoidCallback onAssignShelf;
  final VoidCallback onToShelf;
  final VoidCallback onToBackroom;
  final VoidCallback onChangeMin;

  const StockActions({
    super.key,
    required this.product,
    required this.isManager,
    required this.busy,
    required this.onAssignShelf,
    required this.onToShelf,
    required this.onToBackroom,
    required this.onChangeMin,
  });

  @override
  Widget build(BuildContext context) {
    final p = product;
    final hasShelf = p.shelf != null;
    final canToShelf = !busy && hasShelf && p.backroomQuantity > 0;
    final canToBackroom = !busy && p.shelfQuantity > 0;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: canToShelf ? onToShelf : null,
                  icon: const Icon(Icons.upload_rounded),
                  label: const Text('To shelf'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(AppSizes.buttonHeight)),
                  onPressed: canToBackroom ? onToBackroom : null,
                  icon: const Icon(Icons.download_rounded),
                  label: const Text('To backroom'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(48)),
            onPressed: busy ? null : onAssignShelf,
            icon: const Icon(Icons.shelves),
            label: Text(hasShelf ? 'Change shelf (${p.shelf!.name})' : 'Assign shelf'),
          ),
          if (isManager) ...[
            const SizedBox(height: 4),
            TextButton.icon(
              onPressed: busy ? null : onChangeMin,
              icon: const Icon(Icons.tune_rounded),
              label: Text('Minimum on shelf: ${p.minShelfQuantity}'),
            ),
          ],
          if (!hasShelf) ...[
            const SizedBox(height: 6),
            const Text('Assign a shelf first to put goods on display.',
                textAlign: TextAlign.center, style: AppTextStyles.caption),
          ] else if (p.backroomQuantity == 0 && p.shelfQuantity == 0) ...[
            const SizedBox(height: 6),
            const Text('No stock in this store.',
                textAlign: TextAlign.center, style: AppTextStyles.caption),
          ],
        ],
      ),
    );
  }
}
