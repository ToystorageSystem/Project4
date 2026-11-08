import '../../domain/entities/user_info.dart';

class UserInfoModel extends UserInfo {
  const UserInfoModel({
    required super.id,
    super.userCode,
    required super.name,
    required super.email,
    super.authorities,
  });

  factory UserInfoModel.fromJson(Map<String, dynamic> json) {
    return UserInfoModel(
      id: (json['id'] as num?)?.toInt() ?? 0,
      userCode: json['userCode'] as String?,
      name: json['name'] as String? ?? '',
      email: json['email'] as String? ?? '',
      authorities: _readAuthorities(json['authorities'] ?? json['roles']),
    );
  }

  /// Accepts ["ROLE_X", ...] or [{"authority": "ROLE_X"}, ...]
  static List<String> _readAuthorities(Object? value) {
    if (value is! List) return const [];
    return value
        .map((e) => e is Map ? e['authority'] : e)
        .whereType<String>()
        .toList(growable: false);
  }
}
