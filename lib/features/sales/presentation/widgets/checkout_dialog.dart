import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../domain/usecases/create_sale_usecase.dart';

/// User confirmed the sale (note may be null)
class CheckoutConfirmation {
  final String? note;

  const CheckoutConfirmation(this.note);
}

/// "Confirm sale" with the total + optional note. Returns null = cancel.
Future<CheckoutConfirmation?> showCheckoutDialog(
  BuildContext context, {
  required int lineCount,
  required int totalQuantity,
  required double totalAmount,
}) {
  return showDialog<CheckoutConfirmation>(
    context: context,
    builder: (_) => _CheckoutDialog(
      lineCount: lineCount,
      totalQuantity: totalQuantity,
      totalAmount: totalAmount,
    ),
  );
}

class _CheckoutDialog extends StatefulWidget {
  final int lineCount;
  final int totalQuantity;
  final double totalAmount;

  const _CheckoutDialog({
    required this.lineCount,
    required this.totalQuantity,
    required this.totalAmount,
  });

  @override
  State<_CheckoutDialog> createState() => _CheckoutDialogState();
}

class _CheckoutDialogState extends State<_CheckoutDialog> {
  final _noteCtrl = TextEditingController();

  @override
  void dispose() {
    _noteCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Confirm sale'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('${widget.lineCount} products · ${widget.totalQuantity} pcs'),
          const SizedBox(height: 6),
          Text(
            formatMoney(widget.totalAmount),
            style: const TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.w800,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            'Stock is taken from the shelf first, then the backroom.',
            style: TextStyle(fontSize: 13, color: AppColors.textGrey),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _noteCtrl,
            maxLength: CreateSaleUseCase.maxNoteLength,
            decoration: const InputDecoration(hintText: 'Note (optional)'),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            final note = _noteCtrl.text.trim();
            Navigator.of(context)
                .pop(CheckoutConfirmation(note.isEmpty ? null : note));
          },
          child: const Text('Confirm'),
        ),
      ],
    );
  }
}
