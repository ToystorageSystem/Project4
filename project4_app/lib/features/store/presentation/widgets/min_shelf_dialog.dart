import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../domain/usecases/change_min_shelf_usecase.dart';

/// Store manager: enter the new minimum shelf quantity. Returns null = cancel.
Future<int?> showMinShelfDialog(BuildContext context, {required int current}) {
  return showDialog<int>(
    context: context,
    builder: (_) => _MinShelfDialog(current: current),
  );
}

class _MinShelfDialog extends StatefulWidget {
  final int current;

  const _MinShelfDialog({required this.current});

  @override
  State<_MinShelfDialog> createState() => _MinShelfDialogState();
}

class _MinShelfDialogState extends State<_MinShelfDialog> {
  late final _ctrl = TextEditingController(text: '${widget.current}');
  String? _error;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _submit() {
    final value = int.tryParse(_ctrl.text.trim());
    if (value == null || value < 0 || value > ChangeMinShelfUseCase.maxValue) {
      setState(() =>
          _error = 'Enter a number from 0 to ${ChangeMinShelfUseCase.maxValue}.');
      return;
    }
    Navigator.of(context).pop(value);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Minimum on shelf'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'When the shelf has this many pieces or fewer, '
            'the product shows as LOW STOCK.',
            style: TextStyle(height: 1.45),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _ctrl,
            autofocus: true,
            keyboardType: TextInputType.number,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            onSubmitted: (_) => _submit(),
            decoration: InputDecoration(errorText: _error),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(onPressed: _submit, child: const Text('Save')),
      ],
    );
  }
}
