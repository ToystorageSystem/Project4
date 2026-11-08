import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';

/// Explains the reset process when a user changed or lost their phone.
/// Matches backend flow: admin reset-2fa (+ reset-password) → old phone 401
/// → sign in → change password → set up 2FA on new phone.
Future<void> showLostAuthenticatorDialog(
  BuildContext context, {
  String? email,
}) {
  return showDialog<void>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Lost access to your authenticator?'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'If you changed or lost your phone, you can still sign in.',
              style: TextStyle(fontSize: 15, height: 1.45),
            ),
            const SizedBox(height: 16),
            const _Step(
              number: 1,
              text: 'Use one of your saved recovery codes: tap '
                  '"Use a recovery code" on the code screen.',
            ),
            const _Step(
              number: 2,
              text: 'No recovery code? Contact ${AppStrings.supportContact} '
                  'and verify your identity (employee code or ID card).',
            ),
            const _Step(
              number: 3,
              text: 'After the reset, sign in again and set up '
                  'Google Authenticator on your new phone.',
            ),
            if (email != null && email.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.fieldFill,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.border),
                ),
                child: Text.rich(
                  TextSpan(
                    text: 'Your account: ',
                    style: const TextStyle(
                        fontSize: 14, color: AppColors.textGrey),
                    children: [
                      TextSpan(
                        text: email,
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          color: AppColors.textDark,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.shield_outlined, size: 18, color: AppColors.textGrey),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'For your security, the old phone will be signed out '
                    'automatically after the reset.',
                    style: TextStyle(
                        fontSize: 13, height: 1.4, color: AppColors.textGrey),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx),
          child: const Text('OK'),
        ),
      ],
    ),
  );
}

class _Step extends StatelessWidget {
  final int number;
  final String text;

  const _Step({required this.number, required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CircleAvatar(
            radius: 11,
            backgroundColor: AppColors.primarySoft,
            child: Text(
              '$number',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: AppColors.primary,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(text, style: const TextStyle(fontSize: 14, height: 1.4)),
          ),
        ],
      ),
    );
  }
}
