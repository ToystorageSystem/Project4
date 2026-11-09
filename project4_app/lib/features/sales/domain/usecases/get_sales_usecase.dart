import '../../../../core/models/page_result.dart';
import '../entities/sale_summary.dart';
import '../repositories/sale_repository.dart';

/// Sales history with paging (newest first)
class GetSalesUseCase {
  static const defaultPageSize = 20;
  static const maxPageSize = 50; // backend: size 1–50

  final SaleRepository _repository;

  const GetSalesUseCase(this._repository);

  Future<PageResult<SaleSummary>> call({int page = 0, int size = defaultPageSize}) =>
      _repository.getSales(
        page: page < 0 ? 0 : page,
        size: size.clamp(1, maxPageSize),
      );
}
