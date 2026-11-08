import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/store_product.dart';

/// One product in the search list
class ProductCard extends StatelessWidget {
  final StoreProduct product;
  final VoidCallback onTap;

  const ProductCard({super.key, required this.product, required this.onTap});

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
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: AppSizes.iconBox,
                height: AppSizes.iconBox,
                decoration: BoxDecoration(
                  color: AppColors.primarySoft,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.toys_outlined, color: AppColors.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(product.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: AppTextStyles.cardTitle),
                    const SizedBox(height: 2),
                    Text(
                      '${product.productCode}'
                      '${product.barcode != null ? ' · ${product.barcode}' : ''}',
                      style: AppTextStyles.caption,
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        StatusChip(
                          label: product.shelf?.name ?? 'Not on shelf',
                          color: product.onShelf
                              ? AppColors.primary
                              : AppColors.textGrey,
                        ),
                        StatusChip(
                          label: 'Shelf ${product.shelfQuantity}',
                          color: product.shelfQuantity > 0
                              ? AppColors.success
                              : AppColors.error,
                        ),
                        StatusChip(
                          label: 'Total ${product.totalQuantity}',
                          color: product.isOutOfStock
                              ? AppColors.error
                              : AppColors.textGrey,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Text(
                formatMoney(product.sellingPrice),
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textDark,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
