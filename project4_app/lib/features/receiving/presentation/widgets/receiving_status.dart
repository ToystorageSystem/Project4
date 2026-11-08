import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../domain/entities/receiving_enums.dart';

/// Labels + colors for backend status codes (UI only)
class ReceivingStatusUi {
  static String manifestLabel(String status) => switch (status) {
        ReceivingStatus.notStarted => 'Not started',
        ReceivingStatus.receiving => 'Receiving packages',
        ReceivingStatus.packagesReceived => 'Counting items',
        ReceivingStatus.inspected => 'Waiting for confirmation',
        ReceivingStatus.completed => 'Completed',
        _ => status,
      };

  static Color manifestColor(String status) => switch (status) {
        ReceivingStatus.notStarted => AppColors.textGrey,
        ReceivingStatus.receiving => AppColors.primary,
        ReceivingStatus.packagesReceived => AppColors.primary,
        ReceivingStatus.inspected => const Color(0xFF6B5BD2),
        ReceivingStatus.completed => AppColors.success,
        _ => AppColors.textGrey,
      };

  static String packageLabel(String status) => switch (status) {
        PackageStatus.notReceived => 'Not received',
        PackageStatus.received => 'Received',
        PackageStatus.checked => 'Checked',
        _ => status,
      };

  static Color packageColor(String status) => switch (status) {
        PackageStatus.notReceived => AppColors.textGrey,
        PackageStatus.received => AppColors.primary,
        PackageStatus.checked => AppColors.success,
        _ => AppColors.textGrey,
      };

  static String sealLabel(String? apiValue) => SealStatus.values
      .firstWhere((e) => e.apiValue == apiValue, orElse: () => SealStatus.intact)
      .label;

  static String conditionLabel(String? apiValue) => PackageCondition.values
      .firstWhere((e) => e.apiValue == apiValue,
          orElse: () => PackageCondition.good)
      .label;
}
