import '../entities/sale_detail.dart';
import '../repositories/sale_repository.dart';

class GetSaleDetailUseCase {
  final SaleRepository _repository;

  const GetSaleDetailUseCase(this._repository);

  Future<SaleDetail> call({required int saleId}) => _repository.getSale(saleId);
}
