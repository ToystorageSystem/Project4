import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_colors.dart';

/// Shown ONCE after enabling 2FA. Codes let the user sign in without the phone.
Future<void> showRecoveryCodesDialog(BuildContext context, List<String> codes) {
  return showDialog<void>(
    context: context,
    barrierDismissible: false, // must press the button
    builder: (ctx) {
      var copied = false;
      return StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('Save your recovery codes'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'If you lose your phone, each code can be used ONCE to sign in. '
                  'Keep them somewhere safe (not on this phone).',
                  style: TextStyle(height: 1.45),
                ),
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.fieldFill,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: SelectableText(
                    codes.join('\n'),
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 16,
                      height: 1.6,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textDark,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                TextButton.icon(
                  onPressed: () async {
                    await Clipboard.setData(
                      ClipboardData(text: codes.join('\n')),
                    );
                    setDialogState(() => copied = true);
                  },
                  icon: Icon(copied ? Icons.check_rounded : Icons.copy_rounded),
                  label: Text(copied ? 'Copied' : 'Copy all'),
                ),
                const SizedBox(height: 4),
                const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.warning_amber_rounded,
                      size: 18,
                      color: AppColors.error,
                    ),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        "They are shown only once and can't be viewed again.",
                        style: TextStyle(fontSize: 13, color: AppColors.error),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          actions: [
            FilledButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('I have saved them'),
            ),
          ],
        ),
      );
    },
  );
}
