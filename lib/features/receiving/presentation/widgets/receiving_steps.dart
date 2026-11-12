import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/widgets/button_spinner.dart';

/// "1 ── 2 ── 3" indicator at the top of a step-by-step screen
class StepHeader extends StatelessWidget {
  final List<String> labels;
  final int current; // 1-based

  const StepHeader({super.key, required this.labels, required this.current});

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < labels.length; i++)
          Expanded(
            child: Column(
              children: [
                CircleAvatar(
                  radius: 14,
                  backgroundColor: i + 1 <= current
                      ? AppColors.primary
                      : AppColors.border,
                  child: i + 1 < current
                      ? const Icon(Icons.check, size: 16, color: Colors.white)
                      : Text(
                          '${i + 1}',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: i + 1 == current
                                ? Colors.white
                                : AppColors.textGrey,
                          ),
                        ),
                ),
                const SizedBox(height: 4),
                Text(
                  labels[i],
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: i + 1 == current
                        ? FontWeight.w700
                        : FontWeight.w500,
                    color: i + 1 == current
                        ? AppColors.textDark
                        : AppColors.textGrey,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

/// Bottom bar of every step: Back on the left, next action on the right.
/// [onNext] null = disabled.
class StepFooter extends StatelessWidget {
  final String backLabel;
  final VoidCallback? onBack;
  final String nextLabel;
  final IconData nextIcon;
  final VoidCallback? onNext;
  final bool busy;

  const StepFooter({
    super.key,
    this.backLabel = 'Back',
    required this.onBack,
    required this.nextLabel,
    this.nextIcon = Icons.arrow_forward_rounded,
    required this.onNext,
    this.busy = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                  ),
                  onPressed: busy ? null : onBack,
                  icon: const Icon(Icons.arrow_back_rounded),
                  label: Text(backLabel),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                  ),
                  onPressed: busy ? null : onNext,
                  child: busy
                      ? const ButtonSpinner()
                      : Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                nextLabel,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const SizedBox(width: 6),
                            Icon(nextIcon, size: 20),
                          ],
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
