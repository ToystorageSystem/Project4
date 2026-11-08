import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/package_check.dart';
import 'receiving_providers.dart';

/// Lines of one package for counting (F2), by packageCode
final packageCheckProvider =
    FutureProvider.autoDispose.family<PackageCheck, String>(
  (ref, packageCode) =>
      ref.read(getPackageCheckUseCaseProvider)(packageCode: packageCode),
  retry: (retryCount, error) => null,
);
