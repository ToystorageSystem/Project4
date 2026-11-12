import '../entities/shelf_summary.dart';
import '../repositories/shelf_repository.dart';

/// ACTIVE shelves, sorted by name (backend sorts)
class GetShelvesUseCase {
  final ShelfRepository _repository;

  const GetShelvesUseCase(this._repository);

  Future<List<ShelfSummary>> call() => _repository.getShelves();
}
