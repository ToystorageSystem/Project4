import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/widgets/barcode_scanner_page.dart';
import '../../../../core/widgets/button_spinner.dart';

/// One suggestion under a [CodeSuggestField]
class CodeOption {
  final String code;
  final String? subtitle;

  const CodeOption(this.code, [this.subtitle]);
}

/// Code input for receiving: type by hand (with suggestions), hardware
/// scanner (types + Enter) or phone camera. Optional action button (e.g. Find)
/// on the right. Choosing a suggestion submits it immediately.
class CodeSuggestField extends StatefulWidget {
  final String hintText;
  final String scanTitle;
  final List<CodeOption> options;
  final ValueChanged<String> onSubmitted;
  final String? actionLabel;
  final bool busy;
  final bool enabled;
  final bool clearAfterSubmit;
  final String? initialValue;

  const CodeSuggestField({
    super.key,
    required this.hintText,
    required this.onSubmitted,
    this.options = const [],
    this.scanTitle = 'Scan barcode',
    this.actionLabel,
    this.busy = false,
    this.enabled = true,
    this.clearAfterSubmit = false,
    this.initialValue,
  });

  @override
  State<CodeSuggestField> createState() => _CodeSuggestFieldState();
}

class _CodeSuggestFieldState extends State<CodeSuggestField> {
  late final TextEditingController _ctrl = TextEditingController(
    text: widget.initialValue,
  );
  final _focus = FocusNode();

  bool get _active => widget.enabled && !widget.busy;

  @override
  void dispose() {
    _ctrl.dispose();
    _focus.dispose();
    super.dispose();
  }

  void _submit([String? value]) {
    final code = (value ?? _ctrl.text).trim();
    if (code.isEmpty || !_active) return;
    if (widget.clearAfterSubmit) {
      _ctrl.clear();
    } else {
      _ctrl.text = code;
    }
    _focus.unfocus();
    widget.onSubmitted(code);
  }

  Future<void> _openCamera() async {
    final code = await scanBarcode(context, title: widget.scanTitle);
    if (code != null && mounted) _submit(code);
  }

  Iterable<CodeOption> _filter(TextEditingValue value) {
    final q = value.text.trim().toLowerCase();
    return widget.options
        .where((o) => q.isEmpty || o.code.toLowerCase().contains(q))
        .take(20);
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: LayoutBuilder(
            builder: (context, constraints) => RawAutocomplete<CodeOption>(
              textEditingController: _ctrl,
              focusNode: _focus,
              displayStringForOption: (o) => o.code,
              optionsBuilder: _filter,
              onSelected: (o) => _submit(o.code),
              fieldViewBuilder: (context, controller, focusNode, _) =>
                  TextField(
                    controller: controller,
                    focusNode: focusNode,
                    enabled: _active,
                    textInputAction: TextInputAction.search,
                    onSubmitted: (_) => _submit(),
                    decoration: InputDecoration(
                      hintText: widget.hintText,
                      prefixIcon: const Icon(Icons.qr_code_rounded),
                      suffixIcon: IconButton(
                        tooltip: 'Scan with camera',
                        onPressed: _active ? _openCamera : null,
                        icon: const Icon(Icons.photo_camera_outlined),
                      ),
                    ),
                  ),
              optionsViewBuilder: (context, onSelected, options) => Align(
                alignment: Alignment.topLeft,
                child: Material(
                  elevation: 4,
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      maxHeight: 260,
                      maxWidth: constraints.maxWidth,
                    ),
                    child: ListView(
                      padding: EdgeInsets.zero,
                      shrinkWrap: true,
                      children: [
                        for (final o in options)
                          ListTile(
                            dense: true,
                            leading: const Icon(
                              Icons.history_rounded,
                              color: AppColors.textGrey,
                              size: 20,
                            ),
                            title: Text(o.code, style: AppTextStyles.cardTitle),
                            subtitle: o.subtitle == null
                                ? null
                                : Text(o.subtitle!, style: AppTextStyles.caption),
                            onTap: () => onSelected(o),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
        if (widget.actionLabel != null) ...[
          const SizedBox(width: 8),
          SizedBox(
            width: 96,
            height: 56,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(96, 56),
                padding: const EdgeInsets.symmetric(horizontal: 12),
              ),
              onPressed: _active ? () => _submit() : null,
              child: widget.busy
                  ? const ButtonSpinner()
                  : Text(widget.actionLabel!),
            ),
          ),
        ],
      ],
    );
  }
}
