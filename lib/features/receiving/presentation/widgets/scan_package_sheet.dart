import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/field_label.dart';
import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/receiving_enums.dart';
import '../../domain/usecases/scan_package_usecase.dart';

/// What the user chose for a scanned package
class ScanPackageInput {
  final SealStatus sealStatus;
  final PackageCondition packageCondition;
  final String? notes;

  const ScanPackageInput({
    required this.sealStatus,
    required this.packageCondition,
    this.notes,
  });
}

/// F1 bottom sheet: seal + condition (+ notes if abnormal).
/// Returns null when cancelled.
Future<ScanPackageInput?> showScanPackageSheet(
  BuildContext context, {
  required String packageCode,
  ReceivingPackage? package,
}) {
  return showModalBottomSheet<ScanPackageInput>(
    context: context,
    isScrollControlled: true, // grows above the keyboard
    useSafeArea: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
    ),
    builder: (_) =>
        _ScanPackageSheet(packageCode: packageCode, package: package),
  );
}

class _ScanPackageSheet extends StatefulWidget {
  final String packageCode;
  final ReceivingPackage? package;

  const _ScanPackageSheet({required this.packageCode, this.package});

  @override
  State<_ScanPackageSheet> createState() => _ScanPackageSheetState();
}

class _ScanPackageSheetState extends State<_ScanPackageSheet> {
  SealStatus _seal = SealStatus.intact;
  PackageCondition _condition = PackageCondition.good;
  final _notesCtrl = TextEditingController();
  String? _error;

  bool get _abnormal => ScanPackageUseCase.isAbnormal(_seal, _condition);

  @override
  void dispose() {
    _notesCtrl.dispose();
    super.dispose();
  }

  void _submit() {
    final notes = _notesCtrl.text.trim();
    if (_abnormal && notes.isEmpty) {
      setState(() => _error = 'Please describe the problem (required).');
      return;
    }
    Navigator.of(context).pop(
      ScanPackageInput(
        sealStatus: _seal,
        packageCondition: _condition,
        notes: notes.isEmpty ? null : notes,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.package;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(24, 12, 24, 24 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Receive package ${widget.packageCode}',
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: AppColors.textDark,
              ),
            ),
            if (p != null)
              Text(
                [
                  '${p.lineCount} products · ${p.pieces} pcs',
                  if (p.sealNumber != null) 'Seal no. ${p.sealNumber}',
                ].join(' · '),
                style: AppTextStyles.caption,
              ),
            const SizedBox(height: 20),
            const FieldLabel('Seal'),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final s in SealStatus.values)
                  ChoiceChip(
                    label: Text(s.label),
                    selected: _seal == s,
                    onSelected: (_) => setState(() {
                      _seal = s;
                      _error = null;
                    }),
                  ),
              ],
            ),
            const SizedBox(height: 16),
            const FieldLabel('Package condition'),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final c in PackageCondition.values)
                  ChoiceChip(
                    label: Text(c.label),
                    selected: _condition == c,
                    onSelected: (_) => setState(() {
                      _condition = c;
                      _error = null;
                    }),
                  ),
              ],
            ),
            const SizedBox(height: 16),
            FieldLabel(_abnormal ? 'Notes (required)' : 'Notes (optional)'),
            TextField(
              controller: _notesCtrl,
              maxLines: 2,
              onChanged: (_) {
                if (_error != null) setState(() => _error = null);
              },
              decoration: InputDecoration(
                hintText: _abnormal
                    ? 'What is wrong with the package?'
                    : 'Anything to note',
                errorText: _error,
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                    ),
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _submit,
                    child: const Text('Receive package'),
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
