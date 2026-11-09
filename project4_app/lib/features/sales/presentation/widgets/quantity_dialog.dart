import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Type a quantity (1..max). Returns null when cancelled.
Future<int?> showQuantityDialog(
  BuildContext context, {
  required String title,
  required int current,
  required int max,
}) {
  return showDialog<int>(
    context: context,
    builder: (_) => _QuantityDialog(title: title, current: current, max: max),
  );
}

class _QuantityDialog extends StatefulWidget {
  final String title;
  final int current;
  final int max;

  const _QuantityDialog({required this.title, required this.current, required this.max});

  @override
  State<_QuantityDialog> createState() => _QuantityDialogState();
}

class _QuantityDialogState extends State<_QuantityDialog> {
  late final _ctrl = TextEditingController(text: '${widget.current}');
  String? _error;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _submit() {
    final value = int.tryParse(_ctrl.text.trim());
    if (value == null || value < 1 || value > widget.max) {
      setState(() => _error = 'Enter a number from 1 to ${widget.max}.');
      return;
    }
    Navigator.of(context).pop(value);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.title),
      content: TextField(
        controller: _ctrl,
        autofocus: true,
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        onSubmitted: (_) => _submit(),
        decoration: InputDecoration(
          helperText: 'Max ${widget.max}',
          errorText: _error,
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(onPressed: _submit, child: const Text('OK')),
      ],
    );
  }
}
