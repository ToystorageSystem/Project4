// Dependency wiring (DI) for the receiving feature.
// The ONLY presentation file allowed to import data/.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_client.dart';
import '../../data/datasources/receiving_remote_data_source.dart';
import '../../data/repositories/receiving_repository_impl.dart';
import '../../domain/repositories/receiving_repository.dart';
import '../../domain/usecases/complete_receiving_usecase.dart';
import '../../domain/usecases/confirm_receiving_usecase.dart';
import '../../domain/usecases/finish_inspection_usecase.dart';
import '../../domain/usecases/get_manifest_detail_usecase.dart';
import '../../domain/usecases/get_manifests_usecase.dart';
import '../../domain/usecases/get_package_check_usecase.dart';
import '../../domain/usecases/save_package_check_usecase.dart';
import '../../domain/usecases/scan_package_usecase.dart';

final receivingRemoteDataSourceProvider = Provider<ReceivingRemoteDataSource>(
  (ref) => ReceivingRemoteDataSource(ref.watch(dioProvider)),
);

final receivingRepositoryProvider = Provider<ReceivingRepository>(
  (ref) =>
      ReceivingRepositoryImpl(ref.watch(receivingRemoteDataSourceProvider)),
);

final getManifestsUseCaseProvider = Provider(
  (ref) => GetManifestsUseCase(ref.watch(receivingRepositoryProvider)),
);

final getManifestDetailUseCaseProvider = Provider(
  (ref) => GetManifestDetailUseCase(ref.watch(receivingRepositoryProvider)),
);

final scanPackageUseCaseProvider = Provider(
  (ref) => ScanPackageUseCase(ref.watch(receivingRepositoryProvider)),
);

final completeReceivingUseCaseProvider = Provider(
  (ref) => CompleteReceivingUseCase(ref.watch(receivingRepositoryProvider)),
);

final getPackageCheckUseCaseProvider = Provider(
  (ref) => GetPackageCheckUseCase(ref.watch(receivingRepositoryProvider)),
);

final savePackageCheckUseCaseProvider = Provider(
  (ref) => SavePackageCheckUseCase(ref.watch(receivingRepositoryProvider)),
);

final finishInspectionUseCaseProvider = Provider(
  (ref) => FinishInspectionUseCase(ref.watch(receivingRepositoryProvider)),
);

final confirmReceivingUseCaseProvider = Provider(
  (ref) => ConfirmReceivingUseCase(ref.watch(receivingRepositoryProvider)),
);
