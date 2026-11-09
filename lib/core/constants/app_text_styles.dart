import 'package:flutter/material.dart';

import 'app_colors.dart';

class AppTextStyles {
  /// Big screen title: "Sign in", "Two-step verification"
  static const screenTitle = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w800,
    color: AppColors.textDark,
  );

  /// Grey text under a title
  static const subtitle = TextStyle(
    fontSize: 15,
    height: 1.5,
    color: AppColors.textGrey,
  );

  /// Label above an input field
  static const fieldLabel = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w600,
    color: AppColors.textDark,
  );

  /// "SECURITY", "ADMINISTRATION"
  static const sectionHeader = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w700,
    letterSpacing: 1,
    color: AppColors.textGrey,
  );

  /// Bold title inside a card / tile
  static const cardTitle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    color: AppColors.textDark,
  );

  /// Small grey text (version, hints, secondary info)
  static const caption = TextStyle(fontSize: 13, color: AppColors.textGrey);
}
