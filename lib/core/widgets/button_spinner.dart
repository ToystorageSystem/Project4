import 'package:flutter/material.dart';

/// Small spinner shown inside a button while it is loading
class ButtonSpinner extends StatelessWidget {
  final Color color;

  const ButtonSpinner({super.key, this.color = Colors.white});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 22,
      height: 22,
      child: CircularProgressIndicator(strokeWidth: 2.5, color: color),
    );
  }
}
