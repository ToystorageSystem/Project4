import '../../../../core/network/api_exception.dart';
import '../entities/cart_item.dart';
import '../entities/sale_detail.dart';
import '../repositories/sale_repository.dart';

/// Checkout. Same rules as backend: 1–50 products, quantity 1–1000,
/// no duplicate product (merged here), note ≤ 500.
class CreateSaleUseCase {
  static const maxLines = 50;
  static const maxQuantity = 1000;
  static const maxNoteLength = 500;

  final SaleRepository _repository;

  const CreateSaleUseCase(this._repository);

  Future<SaleDetail> call({required List<SaleLine> lines, String? note}) async {
    // Merge the same product (backend rejects duplicates)
    final merged = <int, int>{};
    for (final line in lines) {
      merged[line.productId] = (merged[line.productId] ?? 0) + line.quantity;
    }

    if (merged.isEmpty) {
      throw const ApiException('The cart is empty', statusCode: 400);
    }
    if (merged.length > maxLines) {
      throw const ApiException('A sale can have at most $maxLines products',
          statusCode: 400);
    }
    if (merged.values.any((q) => q < 1 || q > maxQuantity)) {
      throw const ApiException('Quantity must be between 1 and $maxQuantity',
          statusCode: 400);
    }
    final cleanNote = note?.trim();
    if (cleanNote != null && cleanNote.length > maxNoteLength) {
      throw const ApiException('Note must be at most $maxNoteLength characters',
          statusCode: 400);
    }

    return _repository.createSale(
      lines: [
        for (final e in merged.entries) SaleLine(productId: e.key, quantity: e.value),
      ],
      note: (cleanNote == null || cleanNote.isEmpty) ? null : cleanNote,
    );
  }
}
