import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/field_label.dart';
import '../../domain/entities/move_direction.dart';
import '../../domain/entities/store_product_detail.dart';
import '../../domain/usecases/move_stock_usecase.dart';
import 'sheet_handle.dart';

/// What the user entered in the move sheet
class MoveStockInput {
  final int quantity;
  final String? note;

  const MoveStockInput(this.quantity, this.note);
}

/// Bottom sheet: how many pieces to move (≤ available) + optional note.
/// Returns null when cancelled.
Future<MoveStockInput?> showMoveStockSheet(
  BuildContext context, {
  required StoreProductDetail product,
  required MoveDirection direction,
}) {
  return showAppBottomSheet<MoveStockInput>(
    context,
    (_) => _MoveStockSheet(product: product, direction: direction),
  );
}

class _MoveStockSheet extends StatefulWidget {
  final StoreProductDetail product;
  final MoveDirection direction;

  const _MoveStockSheet({required this.product, required this.direction});

  @override
  State<_MoveStockSheet> createState() => _MoveStockSheetState();
}

class _MoveStockSheetState extends State<_MoveStockSheet> {
  late final TextEditingController _qtyCtrl;
  final _noteCtrl = TextEditingController();
  String? _error;

  int get _available => MoveStockUseCase.available(widget.product, widget.direction);
  bool get _toShelf => widget.direction == MoveDirection.toShelf;

  /// To shelf: suggest enough to be above the minimum again
  int get _suggested {
    final p = widget.product;
    if (!_toShelf) return _available;
    final need = p.minShelfQuantity - p.shelfQuantity + 1;
    if (need <= 0) return _available < 1 ? 0 : 1;
    return need > _available ? _available : need;
  }

  @override
  void initState() {
    super.initState();
    _qtyCtrl = TextEditingController(text: '${_suggested < 1 ? 1 : _suggested}');
  }

  @override
  void dispose() {
    _qtyCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  int get _qty => int.tryParse(_qtyCtrl.text.trim()) ?? 0;

  void _setQty(int value) {
    final v = value.clamp(1, _available < 1 ? 1 : _available);
    _qtyCtrl.text = '$v';
    setState(() => _error = null);
  }

  void _submit() {
    final qty = _qty;
    if (qty < 1) {
      setState(() => _error = 'Enter a quantity of at least 1.');
      return;
    }
    if (qty > _available) {
      setState(() => _error = 'Only $_available pcs available.');
      return;
    }
    final note = _noteCtrl.text.trim();
    Navigator.of(context).pop(MoveStockInput(qty, note.isEmpty ? null : note));
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.product;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    final from = _toShelf ? 'Backroom' : (p.shelf?.name ?? 'Shelf');
    final to = _toShelf ? (p.shelf?.name ?? 'Shelf') : 'Backroom';

    return Padding(
      padding: EdgeInsets.fromLTRB(24, 12, 24, 24 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SheetHandle(),
            const SizedBox(height: 16),
            Text(widget.direction.label,
                style: const TextStyle(
                    fontSize: 20, fontWeight: FontWeight.w800, color: AppColors.textDark)),
            Text(p.name, style: AppTextStyles.caption),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: _Place(label: 'From', name: from)),
                const Icon(Icons.arrow_forward_rounded, color: AppColors.primary),
                Expanded(child: _Place(label: 'To', name: to)),
              ],
            ),
            const SizedBox(height: 8),
            Text('Available: $_available pcs',
                textAlign: TextAlign.center, style: AppTextStyles.caption),
            const SizedBox(height: 16),
            const FieldLabel('Quantity'),
            Row(
              children: [
                IconButton.outlined(
                  onPressed: () => _setQty(_qty - 1),
                  icon: const Icon(Icons.remove_rounded),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _qtyCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    textAlign: TextAlign.center,
                    onChanged: (_) {
                      if (_error != null) setState(() => _error = null);
                    },
                    decoration: InputDecoration(errorText: _error),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.outlined(
                  onPressed: () => _setQty(_qty + 1),
                  icon: const Icon(Icons.add_rounded),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                if (_toShelf && _suggested > 0)
                  ActionChip(
                    label: Text('Suggested $_suggested'),
                    onPressed: () => _setQty(_suggested),
                  ),
                ActionChip(
                  label: Text('All $_available'),
                  onPressed: () => _setQty(_available),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const FieldLabel('Note (optional)'),
            TextField(
              controller: _noteCtrl,
              maxLength: MoveStockUseCase.maxNoteLength,
              decoration: const InputDecoration(hintText: 'e.g. Restock for weekend'),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                        minimumSize: const Size.fromHeight(52)),
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _available > 0 ? _submit : null,
                    child: Text(_toShelf ? 'Move to shelf' : 'Move to backroom'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Place extends StatelessWidget {
  final String label;
  final String name;

  const _Place({required this.label, required this.name});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(label, style: AppTextStyles.caption),
        Text(name,
            textAlign: TextAlign.center,
            style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
      ],
    );
  }
}
