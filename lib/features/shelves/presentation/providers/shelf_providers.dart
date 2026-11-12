// Dependency wiring (DI) for the shelves feature.
// The ONLY presentation file allowed to import data/.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_client.dart';
import '../../data/datasources/shelf_remote_data_source.dart';
import '../../data/repositories/shelf_repository_impl.dart';
import '../../domain/repositories/shelf_repository.dart';
import '../../domain/usecases/get_low_shelf_usecase.dart';
import '../../domain/usecases/get_shelf_detail_usecase.dart';
import '../../domain/usecases/get_shelves_usecase.dart';

final shelfRemoteDataSourceProvider = Provider<ShelfRemoteDataSource>(
  (ref) => ShelfRemoteDataSource(ref.watch(dioProvider)),
);

final shelfRepositoryProvider = Provider<ShelfRepository>(
  (ref) => ShelfRepositoryImpl(ref.watch(shelfRemoteDataSourceProvider)),
);

final getShelvesUseCaseProvider = Provider(
  (ref) => GetShelvesUseCase(ref.watch(shelfRepositoryProvider)),
);

final getShelfDetailUseCaseProvider = Provider(
  (ref) => GetShelfDetailUseCase(ref.watch(shelfRepositoryProvider)),
);

final getLowShelfUseCaseProvider = Provider(
  (ref) => GetLowShelfUseCase(ref.watch(shelfRepositoryProvider)),
);
