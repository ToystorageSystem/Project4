import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/sale_detail.dart';

/// One line of a receipt: name, qty × price = total, taken from shelf/backroom
class SaleLineTile extends StatelessWidget {
  final SaleItemLine line;
  final bool showDivider;

  const SaleLineTile({super.key, required this.line, this.showDivider = true});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        border: showDivider
            ? const Border(bottom: BorderSide(color: AppColors.border))
            : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(line.name,
                    style: const TextStyle(
                        fontSize: 15, fontWeight: FontWeight.w700, color: AppColors.textDark)),
              ),
              const SizedBox(width: 8),
              Text(formatMoney(line.lineTotal),
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800)),
            ],
          ),
          const SizedBox(height: 2),
          Text('${line.productCode} · ${line.quantity} × ${formatMoney(line.unitPrice)}',
              style: AppTextStyles.caption),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            children: [
              if (line.fromShelf > 0)
                StatusChip(label: 'Shelf ${line.fromShelf}', color: AppColors.success),
              if (line.fromBackroom > 0)
                StatusChip(label: 'Backroom ${line.fromBackroom}', color: AppColors.primary),
            ],
          ),
        ],
      ),
    );
  }
}
