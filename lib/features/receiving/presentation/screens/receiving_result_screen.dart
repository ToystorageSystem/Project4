import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_sizes.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../core/widgets/status_chip.dart';
import '../../../auth/presentation/providers/auth_notifier.dart';
import '../../domain/entities/receiving_enums.dart';
import '../../domain/entities/receiving_result.dart';
import '../providers/manifest_detail_notifier.dart';
import '../widgets/confirm_receiving_dialog.dart';
import '../widgets/receiving_status.dart';

/// Result of finish-inspection / confirm (passed with go_router `extra`).
/// Backend has no GET for this → opened only right after those actions.
class ReceivingResultScreen extends ConsumerStatefulWidget {
  final String manifestCode;
  final ReceivingResult? initialResult;

  const ReceivingResultScreen({
    super.key,
    required this.manifestCode,
    this.initialResult,
  });

  @override
  ConsumerState<ReceivingResultScreen> createState() =>
      _ReceivingResultScreenState();
}

class _ReceivingResultScreenState extends ConsumerState<ReceivingResultScreen> {
  late ReceivingResult? _result = widget.initialResult;
  bool _confirming = false;

  Future<void> _confirm() async {
    final note = await showConfirmReceivingDialog(context);
    if (note == null || !mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    setState(() => _confirming = true);
    try {
      final result = await ref
          .read(manifestDetailProvider(widget.manifestCode).notifier)
          .confirm(note: note);
      if (mounted) setState(() => _result = result);
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _confirming = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final result = _result;
    final isManager = ref.watch(authProvider.select((s) => s.isStoreManager));

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: Text('Result ${widget.manifestCode}'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: result == null
          ? const Padding(
              padding: EdgeInsets.all(24),
              child: InfoBanner(
                message:
                    'Result is not available here. '
                    'Open the manifest again from the list.',
              ),
            )
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
              children: [
                Row(
                  children: [
                    StatusChip(
                      label: ReceivingStatusUi.manifestLabel(
                        result.receivingStatus,
                      ),
                      color: ReceivingStatusUi.manifestColor(
                        result.receivingStatus,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                _Totals(result: result),
                const SizedBox(height: 12),
                if (result.receivingStatus == ReceivingStatus.completed)
                  const InfoBanner(
                    icon: Icons.check_circle_outline_rounded,
                    message: 'Confirmed. Good items were added to the backroom stock.',
                  )
                else if (!isManager)
                  const InfoBanner(
                    message:
                        'Waiting for the store manager to confirm into stock.',
                  ),
                if (result.discrepancyReportCodes.isNotEmpty) ...[
                  const SizedBox(height: 20),
                  const SectionHeader('Discrepancy reports'),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      for (final code in result.discrepancyReportCodes)
                        StatusChip(label: code, color: AppColors.error),
                    ],
                  ),
                ],
                const SizedBox(height: 20),
                SectionHeader('Products (${result.items.length})'),
                const SizedBox(height: 8),
                for (final item in result.items) ...[
                  _ItemRow(item: item),
                  const SizedBox(height: 8),
                ],
              ],
            ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child:
              (result != null &&
                  result.receivingStatus == ReceivingStatus.inspected &&
                  isManager)
              ? ElevatedButton(
                  onPressed: _confirming ? null : _confirm,
                  child: _confirming
                      ? const ButtonSpinner()
                      : const Text('Confirm into stock'),
                )
              : OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
                  ),
                  onPressed: () => context.go(AppRoutes.receiving),
                  child: const Text('Back to manifests'),
                ),
        ),
      ),
    );
  }
}

class _Totals extends StatelessWidget {
  final ReceivingResult result;

  const _Totals({required this.result});

  @override
  Widget build(BuildContext context) {
    Widget tile(String label, int value, Color color) => Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          children: [
            Text(
              '$value',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: color,
              ),
            ),
            Text(label, style: AppTextStyles.caption),
          ],
        ),
      ),
    );

    return Column(
      children: [
        Row(
          children: [
            tile('Expected', result.totalExpected, AppColors.textDark),
            const SizedBox(width: 8),
            tile('Actual', result.totalActual, AppColors.textDark),
            const SizedBox(width: 8),
            tile(
              'Damaged',
              result.totalDamaged,
              result.totalDamaged > 0 ? AppColors.error : AppColors.textGrey,
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            tile(
              'Shortage',
              result.totalShortage,
              result.totalShortage > 0 ? AppColors.error : AppColors.textGrey,
            ),
            const SizedBox(width: 8),
            tile(
              'Surplus',
              result.totalSurplus,
              result.totalSurplus > 0 ? AppColors.primary : AppColors.textGrey,
            ),
          ],
        ),
      ],
    );
  }
}

class _ItemRow extends StatelessWidget {
  final ReceivingResultItem item;

  const _ItemRow({required this.item});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppSizes.radiusCard),
        border: Border.all(
          color: item.hasDifference
              ? AppColors.error.withValues(alpha: 0.35)
              : AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(item.name, style: AppTextStyles.cardTitle),
          Text(
            [
              if (item.productCode != null) item.productCode!,
              if (item.transferCode != null) item.transferCode!,
            ].join(' · '),
            style: AppTextStyles.caption,
          ),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              StatusChip(
                label: 'Expected ${item.expected}',
                color: AppColors.textGrey,
              ),
              StatusChip(
                label: 'Actual ${item.actual}',
                color: AppColors.textDark,
              ),
              if (item.damaged > 0)
                StatusChip(
                  label: 'Damaged ${item.damaged}',
                  color: AppColors.error,
                ),
              if (item.shortage > 0)
                StatusChip(
                  label: 'Short ${item.shortage}',
                  color: AppColors.error,
                ),
              if (item.surplus > 0)
                StatusChip(
                  label: 'Surplus ${item.surplus}',
                  color: AppColors.primary,
                ),
            ],
          ),
        ],
      ),
    );
  }
}
