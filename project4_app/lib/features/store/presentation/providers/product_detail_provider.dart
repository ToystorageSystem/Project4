import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/store_product_detail.dart';
import 'store_providers.dart';

/// Detail of one product, by id
final productDetailProvider =
    FutureProvider.autoDispose.family<StoreProductDetail, int>(
  (ref, productId) =>
      ref.read(getStoreProductDetailUseCaseProvider)(productId: productId),
  retry: (retryCount, error) => null,
);
