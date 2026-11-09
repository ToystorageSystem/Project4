import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/shelf_detail.dart';
import 'stock_numbers.dart';

/// One product on a shelf (shelf detail screen)
class ShelfProductTile extends StatelessWidget {
  final ShelfProductLine line;
  final VoidCallback onTap;

  const ShelfProductTile({super.key, required this.line, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(AppSizes.radiusCard);

    return Material(
      color: Colors.white,
      borderRadius: radius,
      child: InkWell(
        borderRadius: radius,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(AppSizes.cardPadding),
          decoration: BoxDecoration(
            borderRadius: radius,
            border: Border.all(
              color: line.lowStock
                  ? AppColors.error.withValues(alpha: 0.4)
                  : AppColors.border,
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(line.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: AppTextStyles.cardTitle),
                    const SizedBox(height: 2),
                    Text(
                      '${line.productCode}'
                      '${line.barcode != null ? ' · ${line.barcode}' : ''}',
                      style: AppTextStyles.caption,
                    ),
                    const SizedBox(height: 6),
                    StockNumbers(
                      shelfQuantity: line.shelfQuantity,
                      minShelfQuantity: line.minShelfQuantity,
                      backroomQuantity: line.backroomQuantity,
                      low: line.lowStock,
                    ),
                  ],
                ),
              ),
              if (line.lowStock) ...[
                const SizedBox(width: 8),
                StatusChip(
                  label: line.backroomQuantity > 0 ? 'LOW' : 'LOW · NO STOCK',
                  color: AppColors.error,
                ),
              ],
              const SizedBox(width: 4),
              const Icon(Icons.chevron_right_rounded, color: AppColors.textGrey),
            ],
          ),
        ),
      ),
    );
  }
}
