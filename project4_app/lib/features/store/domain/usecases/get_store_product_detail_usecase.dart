import '../entities/store_product_detail.dart';
import '../repositories/store_product_repository.dart';

class GetStoreProductDetailUseCase {
  final StoreProductRepository _repository;

  const GetStoreProductDetailUseCase(this._repository);

  Future<StoreProductDetail> call({required int productId}) =>
      _repository.getDetail(productId);
}
