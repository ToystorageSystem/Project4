import '../entities/low_shelf_item.dart';
import '../repositories/shelf_repository.dart';

/// Products to restock: lowest shelf quantity first
class GetLowShelfUseCase {
  final ShelfRepository _repository;

  const GetLowShelfUseCase(this._repository);

  Future<List<LowShelfItem>> call() async {
    final items = await _repository.getLowShelf();
    return [...items]..sort((a, b) {
      final byShelf = a.shelfQuantity.compareTo(b.shelfQuantity);
      return byShelf != 0 ? byShelf : a.name.compareTo(b.name);
    });
  }
}
