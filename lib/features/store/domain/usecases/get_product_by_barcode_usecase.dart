import '../entities/store_product_detail.dart';
import '../repositories/store_product_repository.dart';

/// Exact barcode match (typed or scanned). 404 if not found / not ACTIVE.
class GetProductByBarcodeUseCase {
  final StoreProductRepository _repository;

  const GetProductByBarcodeUseCase(this._repository);

  Future<StoreProductDetail> call({required String barcode}) =>
      _repository.getByBarcode(barcode.trim());
}
