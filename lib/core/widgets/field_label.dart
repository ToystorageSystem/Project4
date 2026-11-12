import 'package:flutter/material.dart';

import '../constants/app_text_styles.dart';

/// Bold label above an input field
class FieldLabel extends StatelessWidget {
  final String text;

  const FieldLabel(this.text, {super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(text, style: AppTextStyles.fieldLabel),
    );
  }
}
