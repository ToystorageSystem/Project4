import '../../domain/entities/cart_item.dart';
import '../../domain/entities/sale_detail.dart';
import 'sale_json.dart';

class SaleItemLineModel extends SaleItemLine {
  const SaleItemLineModel({
    required super.productId,
    required super.productCode,
    super.barcode,
    required super.name,
    required super.quantity,
    required super.unitPrice,
    required super.lineTotal,
    required super.fromShelf,
    required super.fromBackroom,
  });

  factory SaleItemLineModel.fromJson(Map<String, dynamic> json) =>
      SaleItemLineModel(
        productId: readInt(json['productId']),
        productCode: json['productCode'] as String? ?? '',
        barcode: json['barcode'] as String?,
        name: json['name'] as String? ?? '',
        quantity: readInt(json['quantity']),
        unitPrice: readMoney(json['unitPrice']),
        lineTotal: readMoney(json['lineTotal']),
        fromShelf: readInt(json['fromShelf']),
        fromBackroom: readInt(json['fromBackroom']),
      );
}

class SaleDetailModel extends SaleDetail {
  const SaleDetailModel({
    required super.id,
    required super.code,
    required super.totalQuantity,
    required super.totalAmount,
    super.note,
    super.createdByName,
    super.createdAt,
    required super.items,
  });

  factory SaleDetailModel.fromJson(Map<String, dynamic> json) =>
      SaleDetailModel(
        id: readInt(json['id']),
        code: json['code'] as String? ?? '',
        totalQuantity: readInt(json['totalQuantity']),
        totalAmount: readMoney(json['totalAmount']),
        note: json['note'] as String?,
        createdByName: json['createdByName'] as String?,
        createdAt: readDate(json['createdAt']),
        items: (json['items'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(SaleItemLineModel.fromJson)
            .toList(),
      );

  /// Body of POST /api/store/sales
  static Map<String, dynamic> createBody(
    List<SaleLine> lines,
    String? note,
  ) => {
    'items': [
      for (final l in lines) {'productId': l.productId, 'quantity': l.quantity},
    ],
    'note': ?note,
  };
}
