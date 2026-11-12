import '../../../../core/models/page_result.dart';
import '../../../../core/network/api_call.dart';
import '../../domain/entities/cart_item.dart';
import '../../domain/entities/sale_detail.dart';
import '../../domain/entities/sale_summary.dart';
import '../../domain/repositories/sale_repository.dart';
import '../datasources/sale_remote_data_source.dart';

class SaleRepositoryImpl implements SaleRepository {
  final SaleRemoteDataSource _remote;

  SaleRepositoryImpl(this._remote);

  @override
  Future<SaleDetail> createSale({
    required List<SaleLine> lines,
    String? note,
  }) => apiCall(() => _remote.createSale(lines, note));

  @override
  Future<PageResult<SaleSummary>> getSales({
    required int page,
    required int size,
  }) => apiCall(() => _remote.getSales(page: page, size: size));

  @override
  Future<SaleDetail> getSale(int saleId) =>
      apiCall(() => _remote.getSale(saleId));
}
