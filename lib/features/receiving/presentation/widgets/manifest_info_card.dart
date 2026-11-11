import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/manifest_summary.dart';
import 'receiving_status.dart';

/// Read-only summary of one manifest (no tap)
class ManifestInfoCard extends StatelessWidget {
  final ManifestSummary manifest;
  final int? totalPieces;

  const ManifestInfoCard({super.key, required this.manifest, this.totalPieces});

  static String deliveryLabel(String? status) => switch (status) {
    'IN_TRANSIT' => 'In transit',
    'ARRIVED' => 'Arrived',
    'DELIVERED' => 'Delivered',
    final String s => s.replaceAll('_', ' ').toLowerCase(),
    null => 'Unknown',
  };

  @override
  Widget build(BuildContext context) {
    final m = manifest;
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
            children: [
              const Icon(Icons.description_outlined, color: AppColors.primary),
              const SizedBox(width: 8),
              Expanded(
                child: Text(m.manifestCode, style: AppTextStyles.cardTitle),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            [
              if (m.fromWarehouseName != null) 'From ${m.fromWarehouseName}',
              if (m.shipmentCode != null) 'Shipment ${m.shipmentCode}',
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
                label: 'Delivery: ${deliveryLabel(m.deliveryStatus)}',
                color: m.hasArrived ? AppColors.success : AppColors.textGrey,
              ),
              StatusChip(
                label: ReceivingStatusUi.manifestLabel(m.receivingStatus),
                color: ReceivingStatusUi.manifestColor(m.receivingStatus),
              ),
              StatusChip(
                label: 'Packages ${m.receivedPackages}/${m.totalPackages}',
                color: AppColors.textGrey,
              ),
              if (totalPieces != null)
                StatusChip(
                  label: '$totalPieces pcs',
                  color: AppColors.textGrey,
                ),
            ],
          ),
        ],
      ),
    );
  }
}
