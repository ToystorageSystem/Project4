// Dependency wiring (DI) for the sales feature.
// The ONLY presentation file allowed to import data/.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_client.dart';
import '../../data/datasources/sale_remote_data_source.dart';
import '../../data/repositories/sale_repository_impl.dart';
import '../../domain/repositories/sale_repository.dart';
import '../../domain/usecases/create_sale_usecase.dart';
import '../../domain/usecases/get_sale_detail_usecase.dart';
import '../../domain/usecases/get_sales_usecase.dart';

final saleRemoteDataSourceProvider = Provider<SaleRemoteDataSource>(
  (ref) => SaleRemoteDataSource(ref.watch(dioProvider)),
);

final saleRepositoryProvider = Provider<SaleRepository>(
  (ref) => SaleRepositoryImpl(ref.watch(saleRemoteDataSourceProvider)),
);

final createSaleUseCaseProvider = Provider(
  (ref) => CreateSaleUseCase(ref.watch(saleRepositoryProvider)),
);

final getSalesUseCaseProvider = Provider(
  (ref) => GetSalesUseCase(ref.watch(saleRepositoryProvider)),
);

final getSaleDetailUseCaseProvider = Provider(
  (ref) => GetSaleDetailUseCase(ref.watch(saleRepositoryProvider)),
);
