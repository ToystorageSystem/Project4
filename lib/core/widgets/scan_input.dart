import 'package:flutter/material.dart';

import 'barcode_scanner_page.dart';

/// Text field for a code + camera button.
/// Works with: typing + Enter, hardware/Bluetooth scanners (type + Enter),
/// and the phone camera. Calls [onSubmitted] with the trimmed code.
class ScanInput extends StatefulWidget {
  final String hintText;
  final String scanTitle;
  final bool enabled;
  final bool clearAfterSubmit;
  final ValueChanged<String> onSubmitted;

  const ScanInput({
    super.key,
    required this.hintText,
    required this.onSubmitted,
    this.scanTitle = 'Scan barcode',
    this.enabled = true,
    this.clearAfterSubmit = true,
  });

  @override
  State<ScanInput> createState() => _ScanInputState();
}

class _ScanInputState extends State<ScanInput> {
  final _ctrl = TextEditingController();

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _submit(String value) {
    final code = value.trim();
    if (code.isEmpty) return;
    if (widget.clearAfterSubmit) _ctrl.clear();
    widget.onSubmitted(code);
  }

  Future<void> _openCamera() async {
    final code = await scanBarcode(context, title: widget.scanTitle);
    if (code != null) _submit(code);
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _ctrl,
      enabled: widget.enabled,
      textInputAction: TextInputAction.search,
      onSubmitted: _submit,
      decoration: InputDecoration(
        hintText: widget.hintText,
        prefixIcon: const Icon(Icons.qr_code_rounded),
        suffixIcon: IconButton(
          tooltip: 'Scan with camera',
          onPressed: widget.enabled ? _openCamera : null,
          icon: const Icon(Icons.photo_camera_outlined),
        ),
      ),
    );
  }
}
