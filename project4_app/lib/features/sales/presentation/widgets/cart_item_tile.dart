import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../domain/entities/cart_item.dart';

/// One cart line: name, price, − qty +, line total, remove
class CartItemTile extends StatelessWidget {
  final CartItem item;
  final bool enabled;
  final VoidCallback onIncrease;
  final VoidCallback onDecrease;
  final VoidCallback onEditQuantity;
  final VoidCallback onRemove;

  const CartItemTile({
    super.key,
    required this.item,
    required this.enabled,
    required this.onIncrease,
    required this.onDecrease,
    required this.onEditQuantity,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final atMax = item.quantity >= item.available;

    return Container(
      padding: const EdgeInsets.all(AppSizes.cardPadding),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: AppTextStyles.cardTitle),
                    const SizedBox(height: 2),
                    Text('${item.productCode} · ${formatMoney(item.unitPrice)}',
                        style: AppTextStyles.caption),
                  ],
                ),
              ),
              IconButton(
                tooltip: 'Remove',
                visualDensity: VisualDensity.compact,
                onPressed: enabled ? onRemove : null,
                icon: const Icon(Icons.delete_outline_rounded, color: AppColors.error),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              IconButton.outlined(
                visualDensity: VisualDensity.compact,
                onPressed: enabled ? onDecrease : null,
                icon: const Icon(Icons.remove_rounded, size: 18),
              ),
              InkWell(
                onTap: enabled ? onEditQuantity : null,
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  constraints: const BoxConstraints(minWidth: 52),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                  child: Text('${item.quantity}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
                ),
              ),
              IconButton.outlined(
                visualDensity: VisualDensity.compact,
                onPressed: enabled && !atMax ? onIncrease : null,
                icon: const Icon(Icons.add_rounded, size: 18),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text('In stock ${item.available}',
                    style: TextStyle(
                        fontSize: 12,
                        color: atMax ? AppColors.error : AppColors.textGrey)),
              ),
              Text(formatMoney(item.lineTotal),
                  style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: AppColors.textDark)),
            ],
          ),
        ],
      ),
    );
  }
}
