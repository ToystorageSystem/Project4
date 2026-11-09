import '../../../../core/models/page_result.dart';
import '../entities/cart_item.dart';
import '../entities/sale_detail.dart';
import '../entities/sale_summary.dart';

/// Sales of the signed-in user's store (backend finds the store itself).
/// Throws ApiException: 400 "Duplicate product in sale" /
/// "Not enough stock: <code> (available N)", 404 "Product not found: <id>",
/// 403 "Access denied" / "User is not assigned to a store".
abstract interface class SaleRepository {
  /// One transaction: an error on any line = nothing is saved
  Future<SaleDetail> createSale({required List<SaleLine> lines, String? note});

  /// Newest first
  Future<PageResult<SaleSummary>> getSales({required int page, required int size});

  Future<SaleDetail> getSale(int saleId);
}
