import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/scan_input.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../../auth/presentation/providers/auth_notifier.dart';
import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/receiving_result.dart';
import '../providers/manifest_detail_notifier.dart';
import '../widgets/confirm_receiving_dialog.dart';
import '../widgets/package_tile.dart';
import '../widgets/receiving_status.dart';
import '../widgets/scan_package_sheet.dart';

/// One manifest: F1 scan packages → complete → F2 count → finish → confirm
class ManifestDetailScreen extends ConsumerStatefulWidget {
  final String manifestCode;

  const ManifestDetailScreen({super.key, required this.manifestCode});

  @override
  ConsumerState<ManifestDetailScreen> createState() =>
      _ManifestDetailScreenState();
}

class _ManifestDetailScreenState extends ConsumerState<ManifestDetailScreen> {
  bool _busy = false;

  ManifestDetailNotifier get _notifier =>
      ref.read(manifestDetailProvider(widget.manifestCode).notifier);

  /// Run an action with a spinner; show API errors as SnackBar.
  /// 409 (changed by someone else) → reload the manifest.
  Future<T?> _run<T>(Future<T> Function() action) async {
    if (_busy) return null;
    final messenger = ScaffoldMessenger.of(context);
    setState(() => _busy = true);
    try {
      return await action();
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
      if (e.isConflict)
        ref.invalidate(manifestDetailProvider(widget.manifestCode));
      return null;
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  // ---------- F1 ----------

  Future<void> _onPackageCode(ManifestDetail detail, String code) async {
    ReceivingPackage? package;
    for (final p in detail.packages) {
      if (p.packageCode.toLowerCase() == code.toLowerCase()) package = p;
    }
    final messenger = ScaffoldMessenger.of(context);
    if (package != null && package.isReceived) {
      messenger.showSnackBar(
        SnackBar(
          content: Text('Package ${package.packageCode} was already received'),
        ),
      );
      return;
    }

    final input = await showScanPackageSheet(
      context,
      packageCode: package?.packageCode ?? code,
      package: package,
    );
    if (input == null || !mounted) return;

    final ok = await _run(() async {
      await _notifier.scanPackage(
        packageCode: package?.packageCode ?? code,
        sealStatus: input.sealStatus,
        packageCondition: input.packageCondition,
        notes: input.notes,
      );
      return true;
    });
    if (ok == true) {
      messenger.showSnackBar(
        SnackBar(
          content: Text('Package ${package?.packageCode ?? code} received'),
        ),
      );
    }
  }

  Future<void> _completeReceiving(ManifestDetail detail) async {
    final missing = detail.manifest.missingPackages;
    final confirmed = await showConfirmDialog(
      context,
      title: 'Complete package receiving?',
      message: missing > 0
          ? '$missing package(s) were not scanned and will be counted as MISSING.'
          : 'All packages were received.',
      confirmLabel: 'Complete',
      destructive: missing > 0,
    );
    if (!confirmed || !mounted) return;
    await _run(() => _notifier.completeReceiving());
  }

  // ---------- F2 / result ----------

  Future<void> _finishInspection() async {
    final confirmed = await showConfirmDialog(
      context,
      title: 'Finish inspection?',
      message: 'Counts can no longer be changed after this step.',
      confirmLabel: 'Finish',
    );
    if (!confirmed || !mounted) return;
    final result = await _run(() => _notifier.finishInspection());
    if (result != null && mounted) _openResult(result);
  }

  Future<void> _confirm() async {
    final note = await showConfirmReceivingDialog(context);
    if (note == null || !mounted) return;
    final result = await _run(() => _notifier.confirm(note: note));
    if (result != null && mounted) _openResult(result);
  }

  void _openResult(ReceivingResult result) {
    context.push(AppRoutes.receivingResult(widget.manifestCode), extra: result);
  }

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final provider = manifestDetailProvider(widget.manifestCode);
    final detailAsync = ref.watch(provider);
    final isManager = ref.watch(authProvider.select((s) => s.isStoreManager));

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: Text(widget.manifestCode),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        bottom: _busy
            ? const PreferredSize(
                preferredSize: Size.fromHeight(3),
                child: LinearProgressIndicator(minHeight: 3),
              )
            : null,
      ),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ErrorBanner(
                message: e is ApiException
                    ? e.message
                    : 'Could not load manifest.',
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () => ref.invalidate(provider),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
        data: (detail) => RefreshIndicator(
          onRefresh: () => ref.refresh(provider.future),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            children: [
              _ManifestHeader(detail: detail),
              const SizedBox(height: 12),
              _StepGuide(detail: detail, isManager: isManager),
              if (detail.manifest.isScanning && detail.manifest.canReceive) ...[
                const SizedBox(height: 12),
                ScanInput(
                  hintText: 'Scan or type package code',
                  scanTitle: 'Scan package',
                  enabled: !_busy,
                  onSubmitted: (code) => _onPackageCode(detail, code),
                ),
              ],
              const SizedBox(height: 20),
              SectionHeader('Packages (${detail.packages.length})'),
              const SizedBox(height: 10),
              for (final p in detail.packages) ...[
                PackageTile(
                  package: p,
                  onTap: detail.manifest.isCounting && p.isReceived
                      ? () => context.push(
                          AppRoutes.packageCheck(
                            widget.manifestCode,
                            p.packageCode,
                          ),
                        )
                      : null,
                ),
                const SizedBox(height: 8),
              ],
              if (detail.receipts.isNotEmpty) ...[
                const SizedBox(height: 12),
                const SectionHeader('Store receipts'),
                const SizedBox(height: 8),
                for (final r in detail.receipts)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Text(
                      '${r.receiptCode} · ${r.transferCode ?? ''} · ${r.status ?? ''}',
                      style: AppTextStyles.caption,
                    ),
                  ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: detailAsync.hasValue
          ? _buildActions(detailAsync.value!, isManager)
          : null,
    );
  }

  Widget? _buildActions(ManifestDetail detail, bool isManager) {
    final m = detail.manifest;
    Widget? button;

    if (m.isScanning && m.receivedPackages > 0) {
      button = ElevatedButton(
        onPressed: _busy ? null : () => _completeReceiving(detail),
        child: const Text('Complete package receiving'),
      );
    } else if (m.isCounting) {
      final left = detail.uncheckedReceivedCount;
      button = ElevatedButton(
        onPressed: (_busy || left > 0) ? null : _finishInspection,
        child: Text(
          left > 0 ? '$left package(s) left to count' : 'Finish inspection',
        ),
      );
    } else if (m.isInspected && isManager) {
      button = ElevatedButton(
        onPressed: _busy ? null : _confirm,
        child: _busy ? const ButtonSpinner() : const Text('Confirm into stock'),
      );
    }

    if (button == null) return null;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
        child: button,
      ),
    );
  }
}

class _ManifestHeader extends StatelessWidget {
  final ManifestDetail detail;

  const _ManifestHeader({required this.detail});

  @override
  Widget build(BuildContext context) {
    final m = detail.manifest;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              StatusChip(
                label: ReceivingStatusUi.manifestLabel(m.receivingStatus),
                color: ReceivingStatusUi.manifestColor(m.receivingStatus),
              ),
              if (m.deliveryStatus != null)
                StatusChip(label: m.deliveryStatus!, color: AppColors.textGrey),
            ],
          ),
          const SizedBox(height: 10),
          if (m.fromWarehouseName != null)
            Text('From ${m.fromWarehouseName}', style: AppTextStyles.cardTitle),
          if (m.shipmentCode != null)
            Text('Shipment ${m.shipmentCode}', style: AppTextStyles.caption),
          if (m.expectedDeliveryAt != null)
            Text(
              'Expected ${formatDateTime(m.expectedDeliveryAt)}',
              style: AppTextStyles.caption,
            ),
          const SizedBox(height: 10),
          Text(
            'Received ${m.receivedPackages}/${m.totalPackages} packages · '
            'Checked ${m.checkedPackages}',
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

/// Tells the user what to do now
class _StepGuide extends StatelessWidget {
  final ManifestDetail detail;
  final bool isManager;

  const _StepGuide({required this.detail, required this.isManager});

  @override
  Widget build(BuildContext context) {
    final m = detail.manifest;
    final String text;
    if (m.isScanning && !m.canReceive) {
      text = 'This delivery has not arrived yet. You can receive it once it arrives.';
    } else if (m.isScanning) {
      text =
          'Step 1 – Scan every package, check its seal and condition. '
          'Then tap "Complete package receiving".';
    } else if (m.isCounting) {
      text =
          'Step 2 – Open each received package and count its items. '
          'When all are checked, tap "Finish inspection".';
    } else if (m.isInspected) {
      text = isManager
          ? 'Step 3 – Review the result and confirm the goods into stock.'
          : 'Inspection finished. Waiting for the store manager to confirm.';
    } else {
      text = 'Receiving completed.';
    }
    return InfoBanner(message: text);
  }
}
