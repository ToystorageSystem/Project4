import 'package:flutter/material.dart';

import '../network/api_exception.dart';
import 'error_banner.dart';

/// Full-page error of a `.when(error: ...)`: red banner + "Try again"
class LoadErrorView extends StatelessWidget {
  final Object error;
  final String fallbackMessage;
  final VoidCallback onRetry;

  const LoadErrorView({
    super.key,
    required this.error,
    required this.onRetry,
    this.fallbackMessage = 'Could not load data.',
  });

  @override
  Widget build(BuildContext context) {
    final e = error;
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          ErrorBanner(message: e is ApiException ? e.message : fallbackMessage),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Try again'),
          ),
        ],
      ),
    );
  }
}
