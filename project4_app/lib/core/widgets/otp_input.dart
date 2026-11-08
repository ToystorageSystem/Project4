import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../constants/app_colors.dart';

/// 6 display boxes + 1 invisible TextField on top that receives all input
class OtpInput extends StatefulWidget {
  static const length = 6;

  final TextEditingController controller;
  final FocusNode focusNode;
  final bool hasError;
  final bool enabled;
  final bool autofocus;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onCompleted; // called when 6 digits are entered

  const OtpInput({
    super.key,
    required this.controller,
    required this.focusNode,
    this.hasError = false,
    this.enabled = true,
    this.autofocus = true,
    this.onChanged,
    this.onCompleted,
  });

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  @override
  void initState() {
    super.initState();
    // Redraw boxes whenever text or focus changes
    widget.controller.addListener(_refresh);
    widget.focusNode.addListener(_refresh);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_refresh);
    widget.focusNode.removeListener(_refresh);
    super.dispose();
  }

  void _refresh() => setState(() {});

  Widget _box(int index) {
    final text = widget.controller.text;
    final hasDigit = index < text.length;
    final isCurrent = widget.focusNode.hasFocus && index == text.length;

    Color borderColor = AppColors.border;
    Color fillColor = hasDigit ? Colors.white : AppColors.fieldFill;
    double borderWidth = 1;
    List<BoxShadow>? shadow;

    if (widget.hasError) {
      borderColor = AppColors.error;
      fillColor = AppColors.error.withValues(alpha: 0.05);
    } else if (isCurrent) {
      borderColor = AppColors.primary;
      fillColor = Colors.white;
      borderWidth = 1.5;
      shadow = [
        BoxShadow(
          color: AppColors.primary.withValues(alpha: 0.18),
          blurRadius: 10,
        ),
      ];
    }

    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      height: 60,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: fillColor,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: borderColor, width: borderWidth),
        boxShadow: shadow,
      ),
      child: hasDigit
          ? Text(
              text[index],
              style: const TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.w700,
                color: AppColors.textDark,
              ),
            )
          : isCurrent
              ? Container(width: 2, height: 26, color: AppColors.primary)
              : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // Layer 1: 6 display boxes with a dash in the middle
        Row(
          children: [
            for (var i = 0; i < OtpInput.length; i++) ...[
              if (i == 3)
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 6),
                  child: Text('–',
                      style: TextStyle(fontSize: 20, color: AppColors.textGrey)),
                ),
              Expanded(child: _box(i)),
              if (i != 2 && i != OtpInput.length - 1) const SizedBox(width: 8),
            ],
          ],
        ),

        // Layer 2: invisible TextField on top, receives all input
        Positioned.fill(
          child: Opacity(
            opacity: 0,
            child: TextField(
              controller: widget.controller,
              focusNode: widget.focusNode,
              enabled: widget.enabled,
              autofocus: widget.autofocus,
              showCursor: false,
              enableInteractiveSelection: false,
              keyboardType: TextInputType.number,
              autofillHints: const [AutofillHints.oneTimeCode],
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(OtpInput.length),
              ],
              onChanged: (value) {
                widget.onChanged?.call(value);
                if (value.length == OtpInput.length) {
                  widget.onCompleted?.call(value);
                }
              },
              decoration: const InputDecoration(
                border: InputBorder.none,
                filled: false,
                counterText: '',
              ),
            ),
          ),
        ),
      ],
    );
  }
}
