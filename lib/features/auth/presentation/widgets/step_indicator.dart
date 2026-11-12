import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';

/// "▬ ▬  STEP 2 OF 2"
class StepIndicator extends StatelessWidget {
  final int current;
  final int total;
  final MainAxisAlignment alignment;

  const StepIndicator({
    super.key,
    required this.current,
    required this.total,
    this.alignment = MainAxisAlignment.start,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: alignment,
      children: [
        for (var i = 1; i <= total; i++)
          Container(
            width: 28,
            height: 5,
            margin: const EdgeInsets.only(right: 6),
            decoration: BoxDecoration(
              color: i <= current ? AppColors.primary : AppColors.border,
              borderRadius: BorderRadius.circular(3),
            ),
          ),
        const SizedBox(width: 6),
        Text(
          'STEP $current OF $total',
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            letterSpacing: 1,
            color: AppColors.textGrey,
          ),
        ),
      ],
    );
  }
}
