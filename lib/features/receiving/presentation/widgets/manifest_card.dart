import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/manifest_summary.dart';
import 'receiving_status.dart';

/// One incoming manifest in the list
class ManifestCard extends StatelessWidget {
  final ManifestSummary manifest;
  final VoidCallback onTap;

  const ManifestCard({super.key, required this.manifest, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final m = manifest;
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(
                    Icons.local_shipping_outlined,
                    color: AppColors.primary,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(m.manifestCode, style: AppTextStyles.cardTitle),
                  ),
                  const Icon(
                    Icons.chevron_right_rounded,
                    color: AppColors.textGrey,
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                [
                  if (m.fromWarehouseName != null)
                    'From ${m.fromWarehouseName}',
                  if (m.shipmentCode != null) m.shipmentCode!,
                ].join(' · '),
                style: AppTextStyles.caption,
              ),
              if (m.expectedDeliveryAt != null)
                Text(
                  'Expected ${formatDateTime(m.expectedDeliveryAt)}',
                  style: AppTextStyles.caption,
                ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  StatusChip(
                    label: ReceivingStatusUi.manifestLabel(m.receivingStatus),
                    color: ReceivingStatusUi.manifestColor(m.receivingStatus),
                  ),
                  StatusChip(
                    label: 'Packages ${m.receivedPackages}/${m.totalPackages}',
                    color: AppColors.textGrey,
                  ),
                  if (m.checkedPackages > 0)
                    StatusChip(
                      label: 'Checked ${m.checkedPackages}',
                      color: AppColors.success,
                    ),
                  if (m.isScanning)
                    StatusChip(
                      label: m.canReceive
                          ? 'Ready to receive'
                          : 'Not arrived yet',
                      color: m.canReceive
                          ? AppColors.success
                          : AppColors.textGrey,
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
