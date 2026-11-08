import '../entities/package_check.dart';
import '../repositories/receiving_repository.dart';

/// F2: products expected in one received package (+ saved counts)
class GetPackageCheckUseCase {
  final ReceivingRepository _repository;

  const GetPackageCheckUseCase(this._repository);

  Future<PackageCheck> call({required String packageCode}) =>
      _repository.getPackageCheck(packageCode);
}
