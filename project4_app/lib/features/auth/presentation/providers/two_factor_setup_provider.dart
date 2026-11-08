import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/two_factor_setup.dart';
import 'auth_notifier.dart';
import 'auth_providers.dart';

/// Calls the 2FA setup API once when the setup screen opens.
/// autoDispose: leaving the screen throws the secret away.
final twoFactorSetupProvider = FutureProvider.autoDispose<TwoFactorSetup>(
  (ref) => ref.read(setupTwoFactorUseCaseProvider)(
    accountEmail: ref.read(authProvider).user?.email ?? '',
  ),
  retry: (retryCount, error) => null, // show error + Try again instead
);
