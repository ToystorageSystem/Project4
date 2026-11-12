import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/shelf_summary.dart';

/// One shelf in the shelf list
class ShelfCard extends StatelessWidget {
  final ShelfSummary shelf;
  final VoidCallback onTap;

  const ShelfCard({super.key, required this.shelf, required this.onTap});

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
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              Container(
                width: AppSizes.iconBox,
                height: AppSizes.iconBox,
                decoration: BoxDecoration(
                  color: AppColors.primarySoft,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.shelves, color: AppColors.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(shelf.name, style: AppTextStyles.cardTitle),
                    if (shelf.code != null || shelf.description != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        [shelf.code, shelf.description]
                            .whereType<String>()
                            .where((s) => s.isNotEmpty)
                            .join(' · '),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTextStyles.caption,
                      ),
                    ],
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        StatusChip(
                          label: shelf.isEmpty
                              ? 'Empty'
                              : '${shelf.productCount} products',
                          color: shelf.isEmpty
                              ? AppColors.textGrey
                              : AppColors.primary,
                        ),
                        StatusChip(
                          label: '${shelf.shelfQuantity} pcs',
                          color: shelf.shelfQuantity > 0
                              ? AppColors.success
                              : AppColors.textGrey,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.textGrey,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
