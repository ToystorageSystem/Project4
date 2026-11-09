import '../../../../core/network/api_call.dart';
import '../../domain/entities/low_shelf_item.dart';
import '../../domain/entities/shelf_detail.dart';
import '../../domain/entities/shelf_summary.dart';
import '../../domain/repositories/shelf_repository.dart';
import '../datasources/shelf_remote_data_source.dart';

class ShelfRepositoryImpl implements ShelfRepository {
  final ShelfRemoteDataSource _remote;

  ShelfRepositoryImpl(this._remote);

  @override
  Future<List<ShelfSummary>> getShelves() => apiCall(_remote.getShelves);

  @override
  Future<ShelfDetail> getShelf(int shelfId) => apiCall(() => _remote.getShelf(shelfId));

  @override
  Future<List<LowShelfItem>> getLowShelf() => apiCall(_remote.getLowShelf);
}
