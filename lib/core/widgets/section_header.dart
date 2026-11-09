import 'package:flutter/material.dart';

import '../constants/app_text_styles.dart';

/// "SECURITY", "ADMINISTRATION" – grey uppercase group title
class SectionHeader extends StatelessWidget {
  final String title;

  const SectionHeader(this.title, {super.key});

  @override
  Widget build(BuildContext context) {
    return Text(title.toUpperCase(), style: AppTextStyles.sectionHeader);
  }
}
