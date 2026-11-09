import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/sale_detail.dart';
import 'sale_providers.dart';

/// One sale (receipt), by id
final saleDetailProvider = FutureProvider.autoDispose.family<SaleDetail, int>(
  (ref, saleId) => ref.read(getSaleDetailUseCaseProvider)(saleId: saleId),
  retry: (retryCount, error) => null,
);
