/// The shelf a product is placed on (1 product = 1 shelf in a store)
class ShelfInfo {
  final int id;
  final String? code;
  final String name;

  const ShelfInfo({required this.id, this.code, required this.name});
}
