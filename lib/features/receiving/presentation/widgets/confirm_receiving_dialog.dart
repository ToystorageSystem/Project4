import 'package:flutter/material.dart';

/// Store manager confirmation. Returns the note ('' allowed) or null = cancel.
Future<String?> showConfirmReceivingDialog(BuildContext context) {
  return showDialog<String>(
    context: context,
    builder: (_) => const _ConfirmReceivingDialog(),
  );
}

class _ConfirmReceivingDialog extends StatefulWidget {
  const _ConfirmReceivingDialog();

  @override
  State<_ConfirmReceivingDialog> createState() =>
      _ConfirmReceivingDialogState();
}

class _ConfirmReceivingDialogState extends State<_ConfirmReceivingDialog> {
  final _noteCtrl = TextEditingController();

  @override
  void dispose() {
    _noteCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Confirm into stock?'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Good items (actual − damaged) will be added to the backroom stock. '
            'Discrepancy reports are created for shortage, surplus and damage. '
            'This cannot be undone.',
            style: TextStyle(height: 1.45),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _noteCtrl,
            maxLines: 2,
            decoration: const InputDecoration(hintText: 'Note (optional)'),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, _noteCtrl.text.trim()),
          child: const Text('Confirm'),
        ),
      ],
    );
  }
}
