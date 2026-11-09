import '../entities/shelf_detail.dart';
import '../repositories/shelf_repository.dart';

class GetShelfDetailUseCase {
  final ShelfRepository _repository;

  const GetShelfDetailUseCase(this._repository);

  Future<ShelfDetail> call({required int shelfId}) => _repository.getShelf(shelfId);
}
