import '../../../../core/models/page_result.dart';
import '../entities/store_product.dart';
import '../repositories/store_product_repository.dart';

/// Search by name / product code (contains, case-insensitive) with paging
class SearchStoreProductsUseCase {
  static const defaultPageSize = 20;
  static const maxPageSize = 50; // backend: size 1–50

  final StoreProductRepository _repository;

  const SearchStoreProductsUseCase(this._repository);

  Future<PageResult<StoreProduct>> call({
    String keyword = '',
    int? categoryId,
    int page = 0,
    int size = defaultPageSize,
  }) => _repository.searchProducts(
    keyword: keyword.trim(),
    categoryId: categoryId,
    page: page < 0 ? 0 : page,
    size: size.clamp(1, maxPageSize),
  );
}
