import '../entities/low_shelf_item.dart';
import '../entities/shelf_detail.dart';
import '../entities/shelf_summary.dart';

/// Shelves of the signed-in user's store (backend finds the store itself).
/// Throws ApiException: 403 "Access denied" / "User is not assigned to a store",
/// 404 "Shelf not found".
abstract interface class ShelfRepository {
  Future<List<ShelfSummary>> getShelves();

  Future<ShelfDetail> getShelf(int shelfId);

  Future<List<LowShelfItem>> getLowShelf();
}
