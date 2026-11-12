import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/status_chip.dart';

/// One product being counted in Package Inspection (screen state only)
class CountRow {
  final String barcode;
  final int? productId;
  final String? productCode;
  final String name;
  final int expected; // 0 = extra product (not in the package list)
  int quantity;
  int damaged;
  String? note;

  CountRow({
    required this.barcode,
    this.productId,
    this.productCode,
    required this.name,
    required this.expected,
    this.quantity = 0,
    this.damaged = 0,
    this.note,
  });

  bool get isExtra => expected == 0;
  bool get isOver => quantity > expected;
}

/// What the count popup returns
class CountPopupResult {
  final int quantity;
  final int damaged;
  final String? note;

  const CountPopupResult({
    required this.quantity,
    required this.damaged,
    this.note,
  });
}

/// Popup shown after each scan. The number box is an AMOUNT (default 1):
/// **Add** = counted + amount, **Subtract** = counted − amount.
/// Report issue edits the TOTAL damaged quantity (+ note); **Save issue**
/// keeps the count. Returns null when closed.
Future<CountPopupResult?> showCountPopup(
  BuildContext context, {
  required CountRow row,
}) => showDialog<CountPopupResult>(
  context: context,
  barrierDismissible: false,
  builder: (_) => _CountPopup(row: row),
);

class _CountPopup extends StatefulWidget {
  final CountRow row;

  const _CountPopup({required this.row});

  @override
  State<_CountPopup> createState() => _CountPopupState();
}

class _CountPopupState extends State<_CountPopup> {
  static const _max = 1000;

  late final int _current = widget.row.quantity; // count before this popup
  int _amount = 1;
  late final _amountCtrl = TextEditingController(text: '1')
    ..selection = const TextSelection(baseOffset: 0, extentOffset: 1);
  late int _damaged = widget.row.damaged;
  late final _noteCtrl = TextEditingController(text: widget.row.note ?? '');
  late bool _showIssue =
      widget.row.damaged > 0 || (widget.row.note?.isNotEmpty ?? false);
  String? _error;

  int get _afterAdd => _current + _amount;
  int get _afterSubtract => _current - _amount;
  bool get _canAdd => _amount > 0 && _afterAdd <= _max;
  bool get _canSubtract => _amount > 0 && _afterSubtract >= 0;

  @override
  void dispose() {
    _amountCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  void _setAmount(int value) {
    final v = value.clamp(0, _max);
    setState(() {
      _amount = v;
      _error = null;
      _amountCtrl.text = '$v';
      _amountCtrl.selection = TextSelection.collapsed(
        offset: _amountCtrl.text.length,
      );
    });
  }

  /// Validate damaged/note against the new quantity, then close
  void _finish(int newQuantity) {
    final note = _noteCtrl.text.trim();
    if (_damaged > newQuantity) {
      setState(() {
        _showIssue = true;
        _error =
            'Damaged ($_damaged) cannot be more than the count '
            '($newQuantity). Lower the damaged quantity first.';
      });
      return;
    }
    if (_damaged > 0 && note.isEmpty) {
      setState(() {
        _showIssue = true;
        _error = 'Describe the damage (required).';
      });
      return;
    }
    Navigator.pop(
      context,
      CountPopupResult(
        quantity: newQuantity,
        damaged: _damaged,
        note: note.isEmpty ? null : note,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final r = widget.row;
    final overAfterAdd = !r.isExtra && _amount > 0 && _afterAdd > r.expected;
    return AlertDialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      titlePadding: const EdgeInsets.fromLTRB(20, 12, 8, 0),
      contentPadding: const EdgeInsets.fromLTRB(20, 4, 20, 0),
      title: Row(
        children: [
          Expanded(
            child: Text(
              r.name,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
            ),
          ),
          IconButton(
            tooltip: 'Close',
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              [r.barcode, if (r.productCode != null) r.productCode!].join(' · '),
              style: AppTextStyles.caption,
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                StatusChip(
                  label: r.isExtra
                      ? 'Counted now: $_current (extra item)'
                      : 'Counted now: $_current / Expected ${r.expected}',
                  color: r.isExtra || _current > r.expected
                      ? Colors.orange.shade800
                      : _current == r.expected
                      ? AppColors.success
                      : AppColors.textGrey,
                ),
                if (r.damaged > 0)
                  StatusChip(
                    label: '${r.damaged} damaged',
                    color: AppColors.error,
                  ),
              ],
            ),
            const SizedBox(height: 16),
            const Text('Quantity', style: AppTextStyles.fieldLabel),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton.filledTonal(
                  iconSize: 26,
                  onPressed: _amount > 1 ? () => _setAmount(_amount - 1) : null,
                  icon: const Icon(Icons.remove_rounded),
                ),
                const SizedBox(width: 12),
                SizedBox(
                  width: 96,
                  child: TextField(
                    controller: _amountCtrl,
                    autofocus: true,
                    textAlign: TextAlign.center,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                    ),
                    decoration: const InputDecoration(
                      isDense: true,
                      contentPadding: EdgeInsets.symmetric(vertical: 10),
                    ),
                    onChanged: (t) => setState(() {
                      _amount = (int.tryParse(t) ?? 0).clamp(0, _max);
                      _error = null;
                    }),
                  ),
                ),
                const SizedBox(width: 12),
                IconButton.filledTonal(
                  iconSize: 26,
                  onPressed: _amount < _max
                      ? () => _setAmount(_amount + 1)
                      : null,
                  icon: const Icon(Icons.add_rounded),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              _amount == 0
                  ? 'Enter how many to add or subtract.'
                  : 'After Add: $_afterAdd'
                        '${_canSubtract ? '  ·  After Subtract: $_afterSubtract' : ''}',
              textAlign: TextAlign.center,
              style: AppTextStyles.caption,
            ),
            if (overAfterAdd) ...[
              const SizedBox(height: 10),
              _Warning(
                'More than expected (${r.expected}). After Add, '
                '${_afterAdd - r.expected} will be recorded as extra.',
              ),
            ],
            if (r.isExtra) ...[
              const SizedBox(height: 10),
              const _Warning(
                'Not in this package. It will be recorded as extra.',
              ),
            ],
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                      foregroundColor: AppColors.error,
                    ),
                    onPressed: _canSubtract
                        ? () => _finish(_afterSubtract)
                        : null,
                    icon: const Icon(Icons.remove_circle_outline_rounded),
                    label: const Text('Subtract'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                    ),
                    onPressed: _canAdd ? () => _finish(_afterAdd) : null,
                    icon: const Icon(Icons.add_circle_outline_rounded),
                    label: const Text('Add'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (!_showIssue)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  style: TextButton.styleFrom(
                    foregroundColor: AppColors.error,
                  ),
                  onPressed: () => setState(() => _showIssue = true),
                  icon: const Icon(Icons.report_problem_outlined, size: 18),
                  label: const Text('Report issue'),
                ),
              )
            else ...[
              const Divider(),
              const Text(
                'Damaged quantity (total for this product)',
                style: AppTextStyles.fieldLabel,
              ),
              const Text(
                'Damaged items are not added to stock.',
                style: AppTextStyles.caption,
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton.filledTonal(
                    onPressed: _damaged > 0
                        ? () => setState(() {
                            _damaged--;
                            _error = null;
                          })
                        : null,
                    icon: const Icon(Icons.remove_rounded),
                  ),
                  SizedBox(
                    width: 72,
                    child: Text(
                      '$_damaged',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        color: _damaged > 0
                            ? AppColors.error
                            : AppColors.textDark,
                      ),
                    ),
                  ),
                  IconButton.filledTonal(
                    // can't be more than the biggest possible count
                    onPressed: _damaged < _current + _amount
                        ? () => setState(() {
                            _damaged++;
                            _error = null;
                          })
                        : null,
                    icon: const Icon(Icons.add_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _noteCtrl,
                maxLines: 2,
                maxLength: 255,
                onChanged: (_) {
                  if (_error != null) setState(() => _error = null);
                },
                decoration: InputDecoration(
                  labelText: _damaged > 0 ? 'Note (required)' : 'Note',
                  hintText: 'e.g. 2 pcs broken, box crushed',
                ),
              ),
              if (_current > 0)
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: () => _finish(_current),
                    icon: const Icon(Icons.save_outlined, size: 18),
                    label: const Text('Save issue (keep count)'),
                  ),
                ),
            ],
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 4, bottom: 8),
                child: Text(
                  _error!,
                  style: const TextStyle(color: AppColors.error, fontSize: 13),
                ),
              ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }
}

class _Warning extends StatelessWidget {
  final String message;

  const _Warning(this.message);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF4E5),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.orange.shade300),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.warning_amber_rounded,
            size: 18,
            color: Colors.orange.shade800,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(fontSize: 13, color: Colors.orange.shade900),
            ),
          ),
        ],
      ),
    );
  }
}
