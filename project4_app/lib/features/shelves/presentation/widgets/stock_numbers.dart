import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';

/// "Shelf 2 / min 3 · Backroom 15" – compact numbers used in shelf lists
class StockNumbers extends StatelessWidget {
  final int shelfQuantity;
  final int minShelfQuantity;
  final int backroomQuantity;
  final bool low;

  const StockNumbers({
    super.key,
    required this.shelfQuantity,
    required this.minShelfQuantity,
    required this.backroomQuantity,
    required this.low,
  });

  @override
  Widget build(BuildContext context) {
    return Text.rich(
      TextSpan(
        style: AppTextStyles.caption,
        children: [
          const TextSpan(text: 'Shelf '),
          TextSpan(
            text: '$shelfQuantity',
            style: TextStyle(
              fontWeight: FontWeight.w800,
              color: low ? AppColors.error : AppColors.success,
            ),
          ),
          TextSpan(text: ' / min $minShelfQuantity  ·  Backroom '),
          TextSpan(
            text: '$backroomQuantity',
            style: const TextStyle(
                fontWeight: FontWeight.w800, color: AppColors.textDark),
          ),
        ],
      ),
    );
  }
}
