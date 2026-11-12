import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/scan_input.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../domain/entities/package_check.dart';
import '../providers/manifest_detail_notifier.dart';
import '../providers/package_check_provider.dart';
import '../providers/receiving_providers.dart';
import '../widgets/receiving_status.dart';

/// F2: count every product of one received package.
/// Scan a product barcode = +1 to its "actual"; unknown barcode = surplus line.
class PackageCheckScreen extends ConsumerStatefulWidget {
  final String manifestCode;
  final String packageCode;

  const PackageCheckScreen({
    super.key,
    required this.manifestCode,
    required this.packageCode,
  });

  @override
  ConsumerState<PackageCheckScreen> createState() => _PackageCheckScreenState();
}

/// Editable copy of one line (controllers must be disposed)
class _LineEditor {
  final String barcode;
  final String? name;
  final String? productCode;
  final int expected;
  final bool addedHere; // surplus line added on this phone (can be removed)
  final TextEditingController actual;
  final TextEditingController damaged;
  final TextEditingController note;

  _LineEditor({
    required this.barcode,
    this.name,
    this.productCode,
    required this.expected,
    this.addedHere = false,
    int? actualValue,
    int? damagedValue,
    String? noteValue,
  }) : actual = TextEditingController(text: actualValue?.toString() ?? ''),
       damaged = TextEditingController(text: damagedValue?.toString() ?? ''),
       note = TextEditingController(text: noteValue ?? '');

  factory _LineEditor.fromLine(PackageCheckLine l) => _LineEditor(
    barcode: l.barcode,
    name: l.name,
    productCode: l.productCode,
    expected: l.expectedQuantity,
    actualValue: l.actualQuantity,
    damagedValue: l.damagedQuantity,
    noteValue: l.note,
  );

  int? get actualValue => int.tryParse(actual.text.trim());
  int get damagedValue => int.tryParse(damaged.text.trim()) ?? 0;

  void dispose() {
    actual.dispose();
    damaged.dispose();
    note.dispose();
  }
}

class _PackageCheckScreenState extends ConsumerState<PackageCheckScreen> {
  List<_LineEditor>? _editors; // null until data arrives
  bool _dirty = false; // unsaved changes
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    for (final e in _editors ?? const <_LineEditor>[]) {
      e.dispose();
    }
    super.dispose();
  }

  void _setEditors(PackageCheck check) {
    for (final e in _editors ?? const <_LineEditor>[]) {
      e.dispose();
    }
    _editors = check.lines.map(_LineEditor.fromLine).toList();
    _dirty = false;
  }

  void _markDirty() {
    setState(() {
      _dirty = true;
      _error = null;
    });
  }

  // ---------- Actions ----------

  void _onProductScanned(String barcode) {
    final editors = _editors;
    if (editors == null) return;
    final messenger = ScaffoldMessenger.of(context);

    final existing = editors.where((e) => e.barcode == barcode).firstOrNull;
    if (existing != null) {
      final next = (existing.actualValue ?? 0) + 1;
      existing.actual.text = '$next';
      messenger.showSnackBar(
        SnackBar(
          duration: const Duration(milliseconds: 900),
          content: Text('${existing.name ?? barcode}: $next'),
        ),
      );
    } else {
      editors.add(
        _LineEditor(
          barcode: barcode,
          expected: 0,
          addedHere: true,
          actualValue: 1,
        ),
      );
      messenger.showSnackBar(
        SnackBar(content: Text('Unexpected product $barcode added (surplus)')),
      );
    }
    _markDirty();
  }

  void _fillExpected() {
    for (final e in _editors ?? const <_LineEditor>[]) {
      if (e.actual.text.trim().isEmpty) e.actual.text = '${e.expected}';
    }
    _markDirty();
  }

  void _removeLine(_LineEditor editor) {
    setState(() {
      _editors?.remove(editor);
      editor.dispose();
      _dirty = true;
    });
  }

  Future<void> _save() async {
    final editors = _editors;
    if (editors == null || _saving) return;
    FocusScope.of(context).unfocus();

    // All lines must have an actual quantity (backend: all products counted)
    final missing = editors.where((e) => e.actualValue == null).toList();
    if (missing.isNotEmpty) {
      setState(
        () => _error =
            'Enter the actual quantity for every product (${missing.length} left).',
      );
      return;
    }

    final lines = [
      for (final e in editors)
        PackageCountLine(
          barcode: e.barcode,
          actualQuantity: e.actualValue!,
          damagedQuantity: e.damagedValue,
          note: e.note.text.trim().isEmpty ? null : e.note.text.trim(),
        ),
    ];

    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await ref.read(savePackageCheckUseCaseProvider)(
        packageCode: widget.packageCode,
        lines: lines,
      );
      _dirty = false;
      ref.invalidate(manifestDetailProvider(widget.manifestCode));
      messenger.showSnackBar(
        SnackBar(content: Text('Package ${widget.packageCode} checked')),
      );
      navigator.pop();
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _confirmLeave() async {
    final leave = await showConfirmDialog(
      context,
      title: 'Discard changes?',
      message: 'Your counts for this package have not been saved.',
      confirmLabel: 'Discard',
      destructive: true,
    );
    if (leave && mounted) {
      _dirty = false;
      Navigator.of(context).pop();
    }
  }

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final checkAsync = ref.watch(packageCheckProvider(widget.packageCode));
    // Editable only while the manifest is in the counting step
    final manifest = ref
        .watch(manifestDetailProvider(widget.manifestCode))
        .value
        ?.manifest;
    final readOnly = manifest != null && !manifest.isCounting;

    // First data → build the editors once
    if (_editors == null && checkAsync.hasValue) _setEditors(checkAsync.value!);

    return PopScope(
      canPop: !_dirty,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _confirmLeave();
      },
      child: Scaffold(
        backgroundColor: AppColors.fieldFill,
        appBar: AppBar(
          title: Text('Package ${widget.packageCode}'),
          backgroundColor: Colors.white,
          surfaceTintColor: Colors.transparent,
        ),
        body: checkAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ErrorBanner(
                  message: e is ApiException
                      ? e.message
                      : 'Could not load package.',
                ),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: () =>
                      ref.invalidate(packageCheckProvider(widget.packageCode)),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Try again'),
                ),
              ],
            ),
          ),
          data: (check) => _buildBody(check, readOnly),
        ),
        bottomNavigationBar: (!readOnly && _editors != null)
            ? SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                  child: ElevatedButton(
                    onPressed: _saving ? null : _save,
                    child: _saving
                        ? const ButtonSpinner()
                        : const Text('Save counts'),
                  ),
                ),
              )
            : null,
      ),
    );
  }

  Widget _buildBody(PackageCheck check, bool readOnly) {
    final editors = _editors ?? const <_LineEditor>[];
    final counted = editors.where((e) => e.actualValue != null).length;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        Row(
          children: [
            StatusChip(
              label: ReceivingStatusUi.packageLabel(check.status),
              color: ReceivingStatusUi.packageColor(check.status),
            ),
            const SizedBox(width: 8),
            Text(
              'Counted $counted/${editors.length}',
              style: AppTextStyles.caption,
            ),
            const Spacer(),
            if (!readOnly)
              TextButton(
                onPressed: _fillExpected,
                child: const Text('Fill expected'),
              ),
          ],
        ),
        const SizedBox(height: 8),
        if (readOnly)
          const InfoBanner(
            message: 'Inspection is finished – counts are read-only.',
          )
        else
          ScanInput(
            hintText: 'Scan product barcode (+1)',
            scanTitle: 'Scan product',
            enabled: !_saving,
            onSubmitted: _onProductScanned,
          ),
        if (_error != null) ...[
          const SizedBox(height: 12),
          ErrorBanner(message: _error!),
        ],
        const SizedBox(height: 12),
        for (final e in editors) ...[
          _LineCard(
            editor: e,
            readOnly: readOnly,
            onChanged: _markDirty,
            onRemove: e.addedHere ? () => _removeLine(e) : null,
          ),
          const SizedBox(height: 10),
        ],
      ],
    );
  }
}

class _LineCard extends StatelessWidget {
  final _LineEditor editor;
  final bool readOnly;
  final VoidCallback onChanged;
  final VoidCallback? onRemove;

  const _LineCard({
    required this.editor,
    required this.readOnly,
    required this.onChanged,
    this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final e = editor;
    final isSurplus = e.expected == 0;
    final actual = e.actualValue;
    final diff = actual == null ? null : actual - e.expected;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(
          color: isSurplus
              ? AppColors.primary.withValues(alpha: 0.5)
              : AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      e.name ?? 'Unknown product',
                      style: AppTextStyles.cardTitle,
                    ),
                    Text(
                      [
                        if (e.productCode != null) e.productCode!,
                        e.barcode,
                      ].join(' · '),
                      style: AppTextStyles.caption,
                    ),
                  ],
                ),
              ),
              StatusChip(
                label: isSurplus ? 'Not expected' : 'Expected ${e.expected}',
                color: isSurplus ? AppColors.primary : AppColors.textGrey,
              ),
              if (onRemove != null)
                IconButton(
                  tooltip: 'Remove',
                  onPressed: onRemove,
                  icon: const Icon(Icons.close_rounded, color: AppColors.error),
                ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _NumberField(
                  controller: e.actual,
                  label: 'Actual',
                  readOnly: readOnly,
                  onChanged: onChanged,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _NumberField(
                  controller: e.damaged,
                  label: 'Damaged',
                  readOnly: readOnly,
                  onChanged: onChanged,
                ),
              ),
            ],
          ),
          if (diff != null && diff != 0) ...[
            const SizedBox(height: 6),
            Text(
              diff < 0 ? 'Short ${-diff}' : 'Over $diff',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: diff < 0 ? AppColors.error : AppColors.primary,
              ),
            ),
          ],
          const SizedBox(height: 8),
          TextField(
            controller: e.note,
            readOnly: readOnly,
            onChanged: (_) => onChanged(),
            decoration: const InputDecoration(
              hintText: 'Note (optional)',
              isDense: true,
            ),
          ),
        ],
      ),
    );
  }
}

class _NumberField extends StatelessWidget {
  final TextEditingController controller;
  final String label;
  final bool readOnly;
  final VoidCallback onChanged;

  const _NumberField({
    required this.controller,
    required this.label,
    required this.readOnly,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      readOnly: readOnly,
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      onChanged: (_) => onChanged(),
      decoration: InputDecoration(labelText: label, isDense: true),
    );
  }
}
