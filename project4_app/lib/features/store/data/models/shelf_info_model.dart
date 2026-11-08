import '../../domain/entities/shelf_info.dart';

class ShelfInfoModel extends ShelfInfo {
  const ShelfInfoModel({required super.id, super.code, required super.name});

  factory ShelfInfoModel.fromJson(Map<String, dynamic> json) => ShelfInfoModel(
        id: (json['id'] as num?)?.toInt() ?? 0,
        code: json['code'] as String?,
        name: json['name'] as String? ?? '',
      );

  static ShelfInfoModel? tryFromJson(Object? value) =>
      value is Map<String, dynamic> ? ShelfInfoModel.fromJson(value) : null;
}
