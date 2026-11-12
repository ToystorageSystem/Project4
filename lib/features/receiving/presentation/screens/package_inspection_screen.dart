import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../../store/presentation/providers/store_providers.dart';
import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/package_check.dart';
import '../../domain/entities/receiving_enums.dart';
import '../providers/inspection_providers.dart';
import '../providers/manifest_list_provider.dart';
import '../providers/receiving_providers.dart';
import '../widgets/code_suggest_field.dart';
import '../widgets/count_line_tile.dart';
import '../widgets/receiving_steps.dart';
import '../widgets/receiving_status.dart';

/// Package Inspection (F2) in 3 steps:
/// 1. Find package (only when its manifest finished package receiving)
/// 2. Review products (read only, with expected quantities)
/// 3. Count products: scan → popup (quantity, Report issue) → Save count.
///    Step 2 shows Counted / Expected while counting. When every received
///    package of the
///    manifest is counted → Finish inspection → result screen.
class PackageInspectionScreen extends ConsumerStatefulWidget {
  const PackageInspectionScreen({super.key});

  @override
  ConsumerState<PackageInspectionScreen> createState() =>
      _PackageInspectionScreenState();
}

class _PackageInspectionScreenState
    extends ConsumerState<PackageInspectionScreen> {
  static const _steps = ['Find package', 'Review products', 'Count products'];

  int _step = 1;
  bool _busy = false;

  // Step 1
  bool _finding = false;
  String? _findError; // red: not found, not received
  String? _findBlock; // orange: cannot be inspected now
  PackageCheck? _check; // package + expected lines (+ previous count)
  ManifestDetail? _manifest; // manifest of the package

  // Step 3
  List<CountRow> _rows = [];
  String? _lastBarcode;
  String? _countError;
  bool _dirty = false;

  // After Save count
  bool _saved = false;

  bool get _wasCounted => _check?.status == PackageStatus.checked;

  // ---------------- Navigation ----------------

  Future<void> _back() async {
    if (_busy || _finding) return;
    if (_step == 1 || _saved) {
      context.pop();
      return;
    }
    // Step 3 → 2: keep the counts (step 2 shows Counted / Expected)
    if (_step == 2 && _dirty) {
      final discard = await showConfirmDialog(
        context,
        title: 'Discard this count?',
        message: 'The quantities you entered for this package are not saved.',
        confirmLabel: 'Discard',
        destructive: true,
      );
      if (!discard || !mounted) return;
    }
    setState(() {
      if (_step == 2) {
        _rows = _prefill(_check!); // back to the saved state
        _dirty = false;
      }
      _step--;
      _countError = null;
    });
  }

  void _restart() {
    ref.invalidate(uncountedPackagesProvider);
    setState(() {
      _step = 1;
      _check = null;
      _manifest = null;
      _findError = null;
      _findBlock = null;
      _rows = [];
      _lastBarcode = null;
      _countError = null;
      _dirty = false;
      _saved = false;
    });
  }

  // ---------------- Step 1: find package ----------------

  Future<void> _find(String code) async {
    setState(() {
      _finding = true;
      _findError = null;
      _findBlock = null;
      _check = null;
      _manifest = null;
    });
    try {
      final check = await ref.read(getPackageCheckUseCaseProvider)(
        packageCode: code,
      );
      final manifest = await ref.read(getManifestDetailUseCaseProvider)(
        manifestCode: check.manifestCode,
      );
      if (!mounted) return;
      final block = _whyNotInspectable(manifest);
      setState(() {
        _finding = false;
        _check = check;
        _manifest = manifest;
        _findBlock = block;
        _rows = _prefill(check);
        _dirty = false;
        _lastBarcode = null;
      });
      if (block == null) setState(() => _step = 2);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _finding = false;
        _findError = e.isNotFound
            ? 'Package "$code" not found. Check the code – only packages '
                  'sent to your store can be inspected.'
            : e.message.contains('not been received')
            ? 'Package "$code" has not been received. Receive it in '
                  'Receive by Manifest first – or it was missing from the '
                  'delivery.'
            : e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _finding = false;
        _findError = 'Could not look up the package. Please try again.';
      });
    }
  }

  /// null = the package can be counted now
  String? _whyNotInspectable(ManifestDetail d) {
    final m = d.manifest;
    return switch (m.receivingStatus) {
      ReceivingStatus.notStarted || ReceivingStatus.receiving =>
        'Package receiving for manifest ${m.manifestCode} is not completed '
            'yet. Complete it in Receive by Manifest first.',
      ReceivingStatus.inspected =>
        'Inspection for manifest ${m.manifestCode} is already finished – '
            'waiting for the store manager to confirm.',
      ReceivingStatus.completed =>
        'Manifest ${m.manifestCode} has already been received and confirmed.',
      _ => null,
    };
  }

  // ---------------- Step 3: count ----------------

  /// Recount: start from the previous count; new package: empty
  static List<CountRow> _prefill(PackageCheck check) => [
    for (final l in check.lines)
      if ((l.actualQuantity ?? 0) > 0 || (l.damagedQuantity ?? 0) > 0)
        CountRow(
          barcode: l.barcode,
          productId: l.productId,
          productCode: l.productCode,
          name: l.name ?? l.barcode,
          expected: l.expectedQuantity,
          quantity: l.actualQuantity ?? 0,
          damaged: l.damagedQuantity ?? 0,
          note: l.note,
        ),
  ];

  CountRow? _rowOf(String barcode) {
    for (final r in _rows) {
      if (r.barcode == barcode) return r;
    }
    return null;
  }

  void _startCounting() => setState(() {
    _step = 3;
    _countError = null;
  });

  /// Open the popup for [row] (Add / Subtract an amount, Report issue);
  /// apply the result (or add the new row).
  Future<void> _openPopup(CountRow row) async {
    final result = await showCountPopup(context, row: row);
    if (result == null || !mounted) return;
    setState(() {
      final isNew = !_rows.contains(row);
      if (result.quantity == 0 && result.damaged == 0 && (row.isExtra || isNew)) {
        _rows.remove(row); // extra item set to 0 = not there
      } else {
        row
          ..quantity = result.quantity
          ..damaged = result.damaged
          ..note = result.note;
        if (isNew) {
          _rows.insert(0, row);
        } else {
          _rows
            ..remove(row)
            ..insert(0, row);
        }
        _lastBarcode = row.barcode;
      }
      _dirty = true;
      _countError = null;
    });
  }

  Future<void> _onBarcode(String code) async {
    if (_busy) return;
    final barcode = code.trim();
    final check = _check!;

    // 1. Already counting this product → popup (Add / Subtract)
    final existing = _rowOf(barcode);
    if (existing != null) {
      await _openPopup(existing);
      return;
    }

    // 2. Product on the package list → popup for a new row
    PackageCheckLine? line;
    for (final l in check.lines) {
      if (l.barcode == barcode) line = l;
    }
    if (line != null) {
      await _openPopup(
        CountRow(
          barcode: line.barcode,
          productId: line.productId,
          productCode: line.productCode,
          name: line.name ?? line.barcode,
          expected: line.expectedQuantity,
        ),
      );
      return;
    }

    // 3. Not in the package: must exist in the system → popup as extra
    setState(() {
      _busy = true;
      _countError = null;
    });
    try {
      final p = await ref.read(getProductByBarcodeUseCaseProvider)(
        barcode: barcode,
      );
      if (!mounted) return;
      setState(() => _busy = false);
      await _openPopup(
        CountRow(
          barcode: p.barcode ?? barcode,
          productId: p.productId,
          productCode: p.productCode,
          name: p.name,
          expected: 0,
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(
        () => _countError = e.isNotFound
            ? 'Barcode $barcode does not exist in the system.'
            : e.message,
      );
    } catch (_) {
      if (mounted) setState(() => _countError = 'Could not check $barcode.');
    } finally {
      if (mounted && _busy) setState(() => _busy = false);
    }
  }

  static String _signature(PackageCheck c) => [
    c.status,
    for (final l in c.lines)
      '${l.barcode}:${l.actualQuantity}:${l.damagedQuantity}',
  ].join('|');

  Future<void> _save() async {
    final check = _check!;
    final byBarcode = {for (final r in _rows) r.barcode: r};
    final expectedLines = check.lines
        .where((l) => l.expectedQuantity > 0)
        .toList();
    final expectedBarcodes = {for (final l in expectedLines) l.barcode};
    final notScanned = expectedLines
        .where((l) => (byBarcode[l.barcode]?.quantity ?? 0) == 0)
        .toList();

    // Backend needs EVERY product of the package; not scanned = 0
    final lines = <PackageCountLine>[
      for (final l in expectedLines)
        PackageCountLine(
          barcode: l.barcode,
          actualQuantity: byBarcode[l.barcode]?.quantity ?? 0,
          damagedQuantity: byBarcode[l.barcode]?.damaged ?? 0,
          note: byBarcode[l.barcode]?.note,
        ),
      for (final r in _rows)
        if (!expectedBarcodes.contains(r.barcode) && r.quantity > 0)
          PackageCountLine(
            barcode: r.barcode,
            actualQuantity: r.quantity,
            damagedQuantity: r.damaged,
            note: r.note,
          ),
    ];

    final ok = await showConfirmDialog(
      context,
      title: 'Save count?',
      message: notScanned.isEmpty
          ? 'Save the count of package ${check.packageCode}.'
          : '${notScanned.length} product(s) not scanned will be saved as 0 '
                '(shortage): '
                '${notScanned.map((l) => l.name ?? l.barcode).join(', ')}.',
      confirmLabel: 'Save count',
      destructive: notScanned.isNotEmpty,
    );
    if (!ok || !mounted) return;

    setState(() {
      _busy = true;
      _countError = null;
    });
    try {
      // Someone else saved a count while I was counting?
      final latest = await ref.read(getPackageCheckUseCaseProvider)(
        packageCode: check.packageCode,
      );
      if (!mounted) return;
      if (_signature(latest) != _signature(check)) {
        final replace = await showConfirmDialog(
          context,
          title: 'Counted by someone else',
          message:
              'Package ${check.packageCode} was counted by someone else '
              'while you were counting. Replace their count with yours?',
          confirmLabel: 'Replace',
          destructive: true,
        );
        if (!replace || !mounted) return;
      }
      final saved = await ref.read(savePackageCheckUseCaseProvider)(
        packageCode: check.packageCode,
        lines: lines,
      );
      final manifest = await ref.read(getManifestDetailUseCaseProvider)(
        manifestCode: saved.manifestCode,
      );
      ref.invalidate(uncountedPackagesProvider);
      ref.invalidate(manifestListProvider);
      if (!mounted) return;
      setState(() {
        _check = saved;
        _manifest = manifest;
        _saved = true;
        _dirty = false;
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _countError = e.message);
    } catch (_) {
      if (mounted) {
        setState(() => _countError = 'Could not save. Please try again.');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  // ---------------- Finish inspection (whole manifest) ----------------

  Future<void> _finish() async {
    final code = _manifest!.manifest.manifestCode;
    final ok = await showConfirmDialog(
      context,
      title: 'Finish inspection?',
      message:
          'All received packages of manifest $code are counted. Finishing '
          'locks the counts and sends the result (shortage / extra / '
          'damaged) to the store manager for confirmation.',
      confirmLabel: 'Finish inspection',
    );
    if (!ok || !mounted) return;
    setState(() => _busy = true);
    try {
      final result = await ref.read(finishInspectionUseCaseProvider)(
        manifestCode: code,
      );
      ref.invalidate(uncountedPackagesProvider);
      ref.invalidate(manifestListProvider);
      if (!mounted) return;
      context.pushReplacement(AppRoutes.inspectionResult(code), extra: result);
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  // ---------------- Build ----------------

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: (_step == 1 || _saved) && !_busy && !_finding,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _back();
      },
      child: Scaffold(
        backgroundColor: AppColors.fieldFill,
        appBar: AppBar(
          title: const Text('Package Inspection'),
          backgroundColor: Colors.white,
          surfaceTintColor: Colors.transparent,
        ),
        body: Column(
          children: [
            Container(
              color: Colors.white,
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
              child: StepHeader(labels: _steps, current: _step),
            ),
            Expanded(
              child: _saved
                  ? _buildSaved()
                  : switch (_step) {
                      1 => _buildFind(),
                      2 => _buildReview(),
                      _ => _buildCount(),
                    },
            ),
          ],
        ),
      ),
    );
  }

  /// "Manifest X · counted 1 / 2 packages" (+ missing packages)
  Widget _manifestProgress(ManifestDetail d) {
    final received = d.packages.where((p) => p.isReceived).length;
    final checked = d.packages.where((p) => p.isChecked).length;
    final missing = d.packages.length - received;
    return Wrap(
      spacing: 6,
      runSpacing: 6,
      children: [
        StatusChip(
          label: 'Manifest ${d.manifest.manifestCode}',
          color: AppColors.textGrey,
        ),
        StatusChip(
          label: 'Counted $checked / $received packages',
          color: checked == received && received > 0
              ? AppColors.success
              : AppColors.primary,
        ),
        if (missing > 0)
          StatusChip(
            label: '$missing missing (shortage)',
            color: AppColors.error,
          ),
      ],
    );
  }

  bool _allCounted(ManifestDetail d) =>
      d.packages.any((p) => p.isReceived) && d.allReceivedPackagesChecked;

  // Step 1
  Widget _buildFind() {
    final options = [
      for (final p
          in ref.watch(uncountedPackagesProvider).value ??
              const <UncountedPackage>[])
        CodeOption(
          p.packageCode,
          'Manifest ${p.manifestCode} · ${p.lineCount} products · '
          '${p.pieces} pcs',
        ),
    ];
    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.listPadding),
            children: [
              const Text('Find package', style: AppTextStyles.cardTitle),
              const SizedBox(height: 4),
              const Text(
                'Scan the label on the package, or type its code. '
                'Suggestions show received packages that are not counted yet.',
                style: AppTextStyles.caption,
              ),
              const SizedBox(height: 12),
              CodeSuggestField(
                hintText: 'Scan or enter package code',
                scanTitle: 'Scan package',
                actionLabel: 'Find',
                options: options,
                busy: _finding,
                initialValue: _check?.packageCode,
                onSubmitted: _find,
              ),
              if (_findError != null) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: _findError!),
              ],
              if (_findBlock != null) ...[
                const SizedBox(height: 12),
                InfoBanner(message: _findBlock!, icon: Icons.block_rounded),
              ],
              if (_manifest != null && _findBlock != null) ...[
                const SizedBox(height: 12),
                _manifestProgress(_manifest!),
              ],
            ],
          ),
        ),
        StepFooter(
          onBack: _back,
          nextLabel: 'View products',
          onNext: _check != null && _findBlock == null
              ? () => setState(() => _step = 2)
              : null,
        ),
      ],
    );
  }

  // Step 2 – one scrollable page (no fixed boxes → no overflow on phones)
  Widget _buildReview() {
    final check = _check!;
    final d = _manifest!;
    final expectedLines = check.lines.where((l) => l.expectedQuantity > 0);
    final extraRows = _rows.where(
      (r) => !expectedLines.any((l) => l.barcode == r.barcode),
    );
    final pieces = expectedLines.fold<int>(0, (t, l) => t + l.expectedQuantity);
    final counted = _rows.fold<int>(0, (t, r) => t + r.quantity);
    final started = _rows.isNotEmpty || _dirty;

    Widget productTile({
      required String name,
      required String barcode,
      required int qty,
      required int expected,
      required int damaged,
    }) {
      final done = qty == expected && expected > 0;
      final over = qty > expected;
      final color = over
          ? Colors.orange.shade800
          : done
          ? AppColors.success
          : qty > 0
          ? AppColors.primary
          : AppColors.textGrey;
      return ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 12),
        title: Text(name, style: AppTextStyles.cardTitle),
        subtitle: Text(
          [barcode, if (damaged > 0) '$damaged damaged'].join(' · '),
          style: AppTextStyles.caption,
        ),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              expected == 0 ? '$qty extra' : '$qty / $expected',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: color,
              ),
            ),
            const Text('Counted / Expected', style: TextStyle(fontSize: 10)),
          ],
        ),
      );
    }

    final tiles = <Widget>[
      for (final l in expectedLines)
        productTile(
          name: l.name ?? l.barcode,
          barcode: l.barcode,
          qty: _rowOf(l.barcode)?.quantity ?? 0,
          expected: l.expectedQuantity,
          damaged: _rowOf(l.barcode)?.damaged ?? 0,
        ),
      for (final r in extraRows)
        productTile(
          name: r.name,
          barcode: r.barcode,
          qty: r.quantity,
          expected: 0,
          damaged: r.damaged,
        ),
    ];

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.listPadding),
            children: [
              Container(
                padding: const EdgeInsets.all(AppSizes.cardPadding),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.inventory_2_rounded,
                          color: AppColors.primary,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            check.packageCode,
                            style: AppTextStyles.cardTitle,
                          ),
                        ),
                        StatusChip(
                          label: ReceivingStatusUi.packageLabel(check.status),
                          color: ReceivingStatusUi.packageColor(check.status),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${expectedLines.length} products · counted $counted / '
                      '$pieces pcs',
                      style: AppTextStyles.caption,
                    ),
                    const SizedBox(height: 8),
                    _manifestProgress(d),
                  ],
                ),
              ),
              if (_wasCounted && !_dirty) ...[
                const SizedBox(height: 8),
                const InfoBanner(
                  message:
                      'This package was already counted. Counting again will '
                      'replace the previous count.',
                  icon: Icons.history_rounded,
                ),
              ],
              if (_dirty) ...[
                const SizedBox(height: 8),
                const InfoBanner(
                  message: 'Not saved yet. Continue counting, then Save count.',
                  icon: Icons.edit_note_rounded,
                ),
              ],
              if (_allCounted(d) && !_dirty) ...[
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(48),
                  ),
                  onPressed: _busy ? null : _finish,
                  icon: const Icon(Icons.fact_check_outlined),
                  label: const Text('All packages counted – Finish inspection'),
                ),
              ],
              const SizedBox(height: 16),
              const Text(
                'Products in this package (view only)',
                style: AppTextStyles.sectionHeader,
              ),
              const SizedBox(height: 8),
              Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  children: [
                    for (var i = 0; i < tiles.length; i++) ...[
                      if (i > 0) const Divider(height: 1),
                      tiles[i],
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
        StepFooter(
          onBack: _back,
          nextLabel: !started
              ? (_wasCounted ? 'Count again' : 'Start counting')
              : 'Continue counting',
          nextIcon: Icons.qr_code_scanner_rounded,
          onNext: _busy ? null : _startCounting,
        ),
      ],
    );
  }

  // Step 3 – scan only; every scan opens the count popup
  Widget _buildCount() {
    final check = _check!;
    final expectedLines = check.lines.where((l) => l.expectedQuantity > 0);
    final pcs = _rows.fold<int>(0, (t, r) => t + r.quantity);
    final damaged = _rows.fold<int>(0, (t, r) => t + r.damaged);
    final countedProducts = expectedLines
        .where((l) => (_rowOf(l.barcode)?.quantity ?? 0) > 0)
        .length;
    final last = _lastBarcode == null ? null : _rowOf(_lastBarcode!);

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.listPadding),
            children: [
              CodeSuggestField(
                hintText: 'Scan or enter product barcode',
                scanTitle: 'Scan product',
                options: [
                  for (final l in expectedLines)
                    CodeOption(l.barcode, l.name ?? l.productCode),
                ],
                clearAfterSubmit: true,
                onSubmitted: _onBarcode,
              ),
              if (_busy) ...[
                const SizedBox(height: 8),
                const LinearProgressIndicator(),
              ],
              if (_countError != null) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: _countError!),
              ],
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(AppSizes.cardPadding),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(check.packageCode, style: AppTextStyles.cardTitle),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        StatusChip(
                          label: '$pcs pcs scanned',
                          color: AppColors.primary,
                        ),
                        StatusChip(
                          label:
                              '$countedProducts / ${expectedLines.length} '
                              'products',
                          color: countedProducts == expectedLines.length
                              ? AppColors.success
                              : AppColors.textGrey,
                        ),
                        if (damaged > 0)
                          StatusChip(
                            label: '$damaged damaged',
                            color: AppColors.error,
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              if (last != null) ...[
                const SizedBox(height: 12),
                const Text('Last scanned', style: AppTextStyles.sectionHeader),
                const SizedBox(height: 6),
                Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                  child: ListTile(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                      side: const BorderSide(color: AppColors.border),
                    ),
                    title: Text(last.name, style: AppTextStyles.cardTitle),
                    subtitle: Text(
                      last.isExtra
                          ? 'Extra · ${last.quantity} pcs'
                          : 'Counted ${last.quantity} / Expected ${last.expected}',
                      style: AppTextStyles.caption,
                    ),
                    trailing: const Icon(Icons.edit_outlined),
                    onTap: () => _openPopup(last),
                  ),
                ),
              ],
              const SizedBox(height: 16),
              const Text(
                'Scan a product, type how many and tap Add. Counted wrong? '
                'Scan it again, type the amount and tap Subtract. '
                'Go Back to see Counted / Expected for every product.',
                textAlign: TextAlign.center,
                style: AppTextStyles.caption,
              ),
            ],
          ),
        ),
        StepFooter(
          onBack: _back,
          nextLabel: 'Save count',
          nextIcon: Icons.save_rounded,
          busy: _busy,
          onNext: _save,
        ),
      ],
    );
  }

  // After Save count
  Widget _buildSaved() {
    final check = _check!;
    final d = _manifest!;
    final all = _allCounted(d);
    final pcs = check.lines.fold<int>(0, (t, l) => t + (l.actualQuantity ?? 0));
    final damaged = check.lines.fold<int>(
      0,
      (t, l) => t + (l.damagedQuantity ?? 0),
    );
    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.pagePadding),
            children: [
              const Icon(
                Icons.task_alt_rounded,
                size: 64,
                color: AppColors.success,
              ),
              const SizedBox(height: 12),
              const Text(
                'Count saved',
                textAlign: TextAlign.center,
                style: AppTextStyles.screenTitle,
              ),
              const SizedBox(height: 8),
              Text(
                '${check.packageCode}: $pcs pcs counted'
                '${damaged > 0 ? ', $damaged damaged' : ''}.',
                textAlign: TextAlign.center,
                style: AppTextStyles.subtitle,
              ),
              const SizedBox(height: 16),
              Center(child: _manifestProgress(d)),
              const SizedBox(height: 16),
              InfoBanner(
                message: all
                    ? 'All received packages of manifest '
                          '${d.manifest.manifestCode} are counted. Finish the '
                          'inspection to see shortage / extra / damaged.'
                    : 'Scan the next package of manifest '
                          '${d.manifest.manifestCode}.',
              ),
            ],
          ),
        ),
        all
            ? StepFooter(
                backLabel: 'Later',
                onBack: () => context.pop(),
                nextLabel: 'Finish inspection',
                nextIcon: Icons.fact_check_outlined,
                busy: _busy,
                onNext: _finish,
              )
            : StepFooter(
                backLabel: 'Done',
                onBack: () => context.pop(),
                nextLabel: 'Scan next package',
                nextIcon: Icons.qr_code_scanner_rounded,
                onNext: _restart,
              ),
      ],
    );
  }
}
