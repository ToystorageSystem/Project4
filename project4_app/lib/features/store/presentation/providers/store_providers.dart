// Dependency wiring (DI) for the store (product lookup) feature.
// The ONLY presentation file allowed to import data/.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_client.dart';
import '../../data/datasources/store_product_remote_data_source.dart';
import '../../data/repositories/store_product_repository_impl.dart';
import '../../domain/repositories/store_product_repository.dart';
import '../../domain/usecases/assign_shelf_usecase.dart';
import '../../domain/usecases/change_min_shelf_usecase.dart';
import '../../domain/usecases/get_product_by_barcode_usecase.dart';
import '../../domain/usecases/get_store_product_detail_usecase.dart';
import '../../domain/usecases/move_stock_usecase.dart';
import '../../domain/usecases/search_store_products_usecase.dart';

final storeProductRemoteDataSourceProvider = Provider<StoreProductRemoteDataSource>(
  (ref) => StoreProductRemoteDataSource(ref.watch(dioProvider)),
);

final storeProductRepositoryProvider = Provider<StoreProductRepository>(
  (ref) => StoreProductRepositoryImpl(ref.watch(storeProductRemoteDataSourceProvider)),
);

final searchStoreProductsUseCaseProvider = Provider(
    (ref) => SearchStoreProductsUseCase(ref.watch(storeProductRepositoryProvider)));

final getProductByBarcodeUseCaseProvider = Provider(
    (ref) => GetProductByBarcodeUseCase(ref.watch(storeProductRepositoryProvider)));

final getStoreProductDetailUseCaseProvider = Provider(
    (ref) => GetStoreProductDetailUseCase(ref.watch(storeProductRepositoryProvider)));

// ----- Shelf placement -----
final assignShelfUseCaseProvider = Provider(
    (ref) => AssignShelfUseCase(ref.watch(storeProductRepositoryProvider)));

final moveStockUseCaseProvider = Provider(
    (ref) => MoveStockUseCase(ref.watch(storeProductRepositoryProvider)));

final changeMinShelfUseCaseProvider = Provider(
    (ref) => ChangeMinShelfUseCase(ref.watch(storeProductRepositoryProvider)));
