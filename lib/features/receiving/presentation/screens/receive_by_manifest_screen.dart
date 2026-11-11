import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../../domain/entities/manifest_detail.dart';
import '../../domain/entities/manifest_summary.dart';
import '../../domain/entities/receiving_enums.dart';
import '../providers/manifest_detail_notifier.dart';
import '../providers/manifest_list_provider.dart';
import '../providers/receiving_providers.dart';
import '../widgets/code_suggest_field.dart';
import '../widgets/manifest_info_card.dart';
import '../widgets/package_tile.dart';
import '../widgets/receiving_steps.dart';

/// Receive by Manifest (F1) in 3 steps:
/// 1. Find manifest  → 2. Review packages (read only) → 3. Scan packages
/// then Complete receiving. Uses the existing receiving API only.
/// Scanning a package receives it right away (seal INTACT, condition GOOD):
/// the store cannot refuse a package; products are checked later
/// in Package Inspection.
class ReceiveByManifestScreen extends ConsumerStatefulWidget {
  const ReceiveByManifestScreen({super.key});

  @override
  ConsumerState<ReceiveByManifestScreen> createState() =>
      _ReceiveByManifestScreenState();
}

class _ReceiveByManifestScreenState
    extends ConsumerState<ReceiveByManifestScreen> {
  static const _steps = ['Find manifest', 'Review packages', 'Scan packages'];

  int _step = 1;

  // Step 1
  ManifestSummary? _found; // last manifest looked up
  String? _lookupError; // red: not found, network...
  String? _lookupInfo; // orange: not arrived, already received...
  bool _finding = false;

  // Step 2–3 (set when the manifest can be received)
  String? _code;

  // Step 3
  String? _scanError;
  String? _lastReceived; // code of the package received last (highlight)
  bool _busy = false;
  bool _completed = false;

  bool get _canOpenFound => _found != null && _canReceive(_found!);

  static bool _canReceive(ManifestSummary m) => m.hasArrived && m.isScanning;

  // ---------------- Navigation ----------------

  void _back() {
    if (_busy || _finding) return;
    if (_step == 1 || _completed) {
      context.pop();
      return;
    }
    setState(() {
      _step--;
      _scanError = null;
    });
  }

  void _restart() {
    setState(() {
      _step = 1;
      _found = null;
      _code = null;
      _lookupError = null;
      _lookupInfo = null;
      _scanError = null;
      _lastReceived = null;
      _completed = false;
    });
  }

  // ---------------- Step 1: find manifest ----------------

  Future<void> _find(String code) async {
    setState(() {
      _finding = true;
      _found = null;
      _lookupError = null;
      _lookupInfo = null;
    });
    try {
      final detail = await ref.read(getManifestDetailUseCaseProvider)(
        manifestCode: code,
      );
      if (!mounted) return;
      final m = detail.manifest;
      setState(() {
        _finding = false;
        _found = m;
        _lookupInfo = _whyNotReceivable(m);
      });
      if (_canReceive(m)) _open(m);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _finding = false;
        _lookupError = e.isNotFound
            ? 'Manifest "$code" not found. Check the code – only manifests '
                  'sent to your store can be received.'
            : e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _finding = false;
        _lookupError = 'Could not look up the manifest. Please try again.';
      });
    }
  }

  /// null = can be received now
  String? _whyNotReceivable(ManifestSummary m) {
    if (!m.hasArrived) {
      final eta = m.expectedDeliveryAt == null
          ? ''
          : ' Expected: ${formatDateTime(m.expectedDeliveryAt)}.';
      return 'Shipment has not arrived yet '
          '(${ManifestInfoCard.deliveryLabel(m.deliveryStatus)}).$eta';
    }
    return switch (m.receivingStatus) {
      ReceivingStatus.packagesReceived =>
        'Package receiving is already completed for this manifest. '
            'Continue with Package Inspection.',
      ReceivingStatus.inspected =>
        'This manifest has been inspected and is waiting for the store '
            'manager to confirm.',
      ReceivingStatus.completed => 'This manifest has already been received.',
      _ => null,
    };
  }

  /// Go to step 2 – or straight to step 3 when receiving was started before
  void _open(ManifestSummary m) {
    final resume = m.receivedPackages > 0;
    setState(() {
      _code = m.manifestCode;
      _step = resume ? 3 : 2;
      _completed = false;
      _scanError = null;
      _lastReceived = null;
    });
    if (resume) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Continue receiving: ${m.receivedPackages}/${m.totalPackages} '
            'packages already received.',
          ),
        ),
      );
    }
  }

  // ---------------- Step 3: scan packages ----------------

  /// Check the code locally (faster message), then receive the package.
  /// The backend checks the same rules again.
  Future<void> _onPackageCode(ManifestDetail detail, String code) async {
    if (_busy) return;
    ReceivingPackage? match;
    for (final p in detail.packages) {
      if (p.packageCode.toUpperCase() == code.toUpperCase()) match = p;
    }
    final found = match;
    if (found == null) {
      setState(
        () => _scanError = 'Package "$code" does not belong to this manifest.',
      );
      return;
    }
    if (found.isReceived) {
      setState(
        () => _scanError =
            'Package ${found.packageCode} has already been received.',
      );
      return;
    }
    await _receive(found);
  }

  Future<void> _receive(ReceivingPackage p) async {
    final messenger = ScaffoldMessenger.of(context);
    setState(() {
      _busy = true;
      _scanError = null;
    });
    try {
      await ref
          .read(manifestDetailProvider(_code!).notifier)
          .scanPackage(
            packageCode: p.packageCode,
            sealStatus: SealStatus.intact,
            packageCondition: PackageCondition.good,
          );
      if (!mounted) return;
      setState(() => _lastReceived = p.packageCode);
      messenger.showSnackBar(
        SnackBar(
          content: Text('${p.packageCode} received.'),
          duration: const Duration(seconds: 2),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _scanError = e.message);
      if (e.isConflict) ref.invalidate(manifestDetailProvider(_code!));
    } catch (_) {
      if (mounted) {
        setState(() => _scanError = 'Could not save. Please try again.');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _complete(ManifestDetail detail) async {
    final missing = detail.packages.where((p) => !p.isReceived).toList();
    final total = detail.packages.length;
    final ok = await showConfirmDialog(
      context,
      title: 'Complete receiving?',
      message: missing.isEmpty
          ? 'All $total packages have been received.'
          : '${missing.length} of $total package(s) not received '
                '(${missing.map((p) => p.packageCode).join(', ')}) will be '
                'recorded as missing. This cannot be undone.',
      confirmLabel: 'Complete receiving',
      destructive: missing.isNotEmpty,
    );
    if (!ok || !mounted) return;
    setState(() {
      _busy = true;
      _scanError = null;
    });
    try {
      await ref.read(manifestDetailProvider(_code!).notifier).completeReceiving();
      if (mounted) setState(() => _completed = true);
    } on ApiException catch (e) {
      if (mounted) setState(() => _scanError = e.message);
    } catch (_) {
      if (mounted) {
        setState(() => _scanError = 'Could not complete. Please try again.');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  // ---------------- Build ----------------

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: (_step == 1 || _completed) && !_busy && !_finding,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _back();
      },
      child: Scaffold(
        backgroundColor: AppColors.fieldFill,
        appBar: AppBar(
          title: const Text('Receive by Manifest'),
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
              child: switch (_step) {
                1 => _buildFind(),
                2 => _buildReview(),
                _ => _buildScan(),
              },
            ),
          ],
        ),
      ),
    );
  }

  // Step 1
  Widget _buildFind() {
    final waiting = (ref.watch(manifestListProvider).value ?? const [])
        .where(_canReceive)
        .map(
          (m) => CodeOption(
            m.manifestCode,
            [
              if (m.fromWarehouseName != null) m.fromWarehouseName!,
              '${m.receivedPackages}/${m.totalPackages} packages received',
            ].join(' · '),
          ),
        )
        .toList();

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.listPadding),
            children: [
              const Text('Find manifest', style: AppTextStyles.cardTitle),
              const SizedBox(height: 4),
              const Text(
                'Scan the barcode on the shipment manifest, or type the code. '
                'Suggestions show manifests waiting to be received.',
                style: AppTextStyles.caption,
              ),
              const SizedBox(height: 12),
              CodeSuggestField(
                hintText: 'Scan or enter manifest code',
                scanTitle: 'Scan manifest',
                actionLabel: 'Find',
                options: waiting,
                busy: _finding,
                initialValue: _found?.manifestCode,
                onSubmitted: _find,
              ),
              if (_lookupError != null) ...[
                const SizedBox(height: 12),
                ErrorBanner(message: _lookupError!),
              ],
              if (_lookupInfo != null) ...[
                const SizedBox(height: 12),
                InfoBanner(
                  message: _lookupInfo!,
                  icon: Icons.local_shipping_outlined,
                ),
              ],
              if (_found != null) ...[
                const SizedBox(height: 12),
                ManifestInfoCard(manifest: _found!),
              ],
            ],
          ),
        ),
        StepFooter(
          onBack: _back,
          nextLabel: 'View packages',
          onNext: _canOpenFound ? () => _open(_found!) : null,
        ),
      ],
    );
  }

  // Step 2
  Widget _buildReview() {
    final async = ref.watch(manifestDetailProvider(_code!));
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Column(
        children: [
          Expanded(
            child: LoadErrorView(
              error: e,
              fallbackMessage: 'Could not load the manifest.',
              onRetry: () => ref.invalidate(manifestDetailProvider(_code!)),
            ),
          ),
          StepFooter(onBack: _back, nextLabel: 'Start receiving', onNext: null),
        ],
      ),
      data: (d) {
        final pieces = d.packages.fold<int>(0, (t, p) => t + p.pieces);
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: ManifestInfoCard(manifest: d.manifest, totalPieces: pieces),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      'Packages in this manifest (${d.packages.length})',
                      style: AppTextStyles.cardTitle,
                    ),
                  ),
                  const Text('View only', style: AppTextStyles.caption),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: Container(
                margin: const EdgeInsets.symmetric(horizontal: 16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppSizes.radiusCard),
                  border: Border.all(color: AppColors.border),
                ),
                child: ListView.separated(
                  padding: const EdgeInsets.all(10),
                  itemCount: d.packages.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                  itemBuilder: (_, i) => PackageTile(package: d.packages[i]),
                ),
              ),
            ),
            const SizedBox(height: 12),
            StepFooter(
              onBack: _back,
              nextLabel: 'Start receiving',
              nextIcon: Icons.qr_code_scanner_rounded,
              onNext: d.manifest.isScanning
                  ? () => setState(() {
                      _step = 3;
                      _scanError = null;
                    })
                  : null,
            ),
          ],
        );
      },
    );
  }

  // Step 3
  Widget _buildScan() {
    final async = ref.watch(manifestDetailProvider(_code!));
    final d = async.value;
    if (d == null) {
      return async.hasError
          ? LoadErrorView(
              error: async.error!,
              onRetry: () => ref.invalidate(manifestDetailProvider(_code!)),
            )
          : const Center(child: CircularProgressIndicator());
    }
    if (_completed) return _buildDone(d);

    final pending = d.packages.where((p) => !p.isReceived).toList();
    final received = d.packages.where((p) => p.isReceived).toList();
    final total = d.packages.length;
    final stillOpen = d.manifest.isScanning;

    return Column(
      children: [
        Container(
          color: Colors.white,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CodeSuggestField(
                hintText: 'Scan or enter package code to receive',
                scanTitle: 'Scan package',
                options: [
                  for (final p in pending)
                    CodeOption(
                      p.packageCode,
                      '${p.lineCount} products · ${p.pieces} pcs',
                    ),
                ],
                enabled: stillOpen && !_busy,
                clearAfterSubmit: true,
                onSubmitted: (code) => _onPackageCode(d, code),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      '${d.manifest.manifestCode} · Received '
                      '${received.length} / $total packages',
                      style: AppTextStyles.fieldLabel,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              LinearProgressIndicator(
                value: total == 0 ? 0 : received.length / total,
                minHeight: 6,
                borderRadius: BorderRadius.circular(3),
                backgroundColor: AppColors.border,
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(AppSizes.listPadding),
            children: [
              if (!stillOpen) ...[
                const InfoBanner(
                  message:
                      'Package receiving for this manifest is already '
                      'completed (maybe on another device).',
                ),
                const SizedBox(height: 12),
              ],
              if (_scanError != null) ...[
                ErrorBanner(message: _scanError!),
                const SizedBox(height: 12),
              ],
              if (_busy) ...[
                const LinearProgressIndicator(),
                const SizedBox(height: 12),
              ],
              Text(
                'Pending (${pending.length})',
                style: AppTextStyles.sectionHeader,
              ),
              const SizedBox(height: 8),
              if (pending.isEmpty)
                const Text(
                  'All packages received. Tap Complete receiving.',
                  style: AppTextStyles.caption,
                ),
              for (final p in pending)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: PackageTile(package: p),
                ),
              const SizedBox(height: 8),
              Text(
                'Received (${received.length})',
                style: AppTextStyles.sectionHeader,
              ),
              const SizedBox(height: 8),
              // newest first: the package just scanned is on top
              for (final p in [
                ...received.where((p) => p.packageCode == _lastReceived),
                ...received.where((p) => p.packageCode != _lastReceived),
              ])
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: PackageTile(package: p),
                ),
            ],
          ),
        ),
        StepFooter(
          onBack: _back,
          nextLabel: 'Complete receiving',
          nextIcon: Icons.task_alt_rounded,
          busy: _busy,
          onNext: stillOpen && received.isNotEmpty
              ? () => _complete(d)
              : null,
        ),
      ],
    );
  }

  // After Complete receiving
  Widget _buildDone(ManifestDetail d) {
    final missing = d.packages.where((p) => !p.isReceived).toList();
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
                'Package receiving completed',
                textAlign: TextAlign.center,
                style: AppTextStyles.screenTitle,
              ),
              const SizedBox(height: 8),
              Text(
                '${d.manifest.manifestCode}: received '
                '${d.packages.length - missing.length} of '
                '${d.packages.length} packages.',
                textAlign: TextAlign.center,
                style: AppTextStyles.subtitle,
              ),
              const SizedBox(height: 16),
              if (missing.isNotEmpty) ...[
                ErrorBanner(
                  message:
                      'Missing (recorded as shortage): '
                      '${missing.map((p) => p.packageCode).join(', ')}',
                ),
                const SizedBox(height: 12),
              ],
              const InfoBanner(
                message:
                    'Next: count the products in each package with '
                    'Package Inspection.',
              ),
            ],
          ),
        ),
        StepFooter(
          backLabel: 'New manifest',
          onBack: _restart,
          nextLabel: 'Done',
          nextIcon: Icons.home_rounded,
          onNext: () => context.pop(),
        ),
      ],
    );
  }
}
