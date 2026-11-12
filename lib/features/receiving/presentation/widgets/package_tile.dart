import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/manifest_detail.dart';
import 'receiving_status.dart';

/// One package in a manifest. [onTap] null = not clickable.
class PackageTile extends StatelessWidget {
  final ReceivingPackage package;
  final VoidCallback? onTap;

  const PackageTile({super.key, required this.package, this.onTap});

  @override
  Widget build(BuildContext context) {
    final p = package;
    final radius = BorderRadius.circular(AppSizes.radiusCard);

    return Material(
      color: Colors.white,
      borderRadius: radius,
      child: InkWell(
        borderRadius: radius,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            borderRadius: radius,
            border: Border.all(
              color: p.isAbnormal
                  ? AppColors.error.withValues(alpha: 0.4)
                  : AppColors.border,
            ),
          ),
          child: Row(
            children: [
              Icon(
                p.isChecked
                    ? Icons.inventory_rounded
                    : p.isReceived
                    ? Icons.inventory_2_rounded
                    : Icons.inventory_2_outlined,
                color: ReceivingStatusUi.packageColor(p.status),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(p.packageCode, style: AppTextStyles.cardTitle),
                    Text(
                      [
                        '${p.lineCount} products · ${p.pieces} pcs',
                        if (p.transferCode != null) p.transferCode!,
                        if (p.sealNumber != null) 'Seal ${p.sealNumber}',
                      ].join(' · '),
                      style: AppTextStyles.caption,
                    ),
                    if (p.isReceived) ...[
                      const SizedBox(height: 6),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          StatusChip(
                            label:
                                'Seal: ${ReceivingStatusUi.sealLabel(p.sealStatus)}',
                            color:
                                p.sealStatus == null || p.sealStatus == 'INTACT'
                                ? AppColors.success
                                : AppColors.error,
                          ),
                          StatusChip(
                            label: ReceivingStatusUi.conditionLabel(
                              p.packageCondition,
                            ),
                            color:
                                p.packageCondition == null ||
                                    p.packageCondition == 'GOOD'
                                ? AppColors.success
                                : AppColors.error,
                          ),
                        ],
                      ),
                    ],
                    if (p.notes != null && p.notes!.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(
                          'Note: ${p.notes}',
                          style: AppTextStyles.caption,
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              StatusChip(
                label: ReceivingStatusUi.packageLabel(p.status),
                color: ReceivingStatusUi.packageColor(p.status),
              ),
              if (onTap != null)
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
