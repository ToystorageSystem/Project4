import '../../domain/entities/auth_session.dart';
import 'user_info_model.dart';

/// LoginResponse: { stage, mustChangePassword, twoFactorRequired,
///   twoFactorSetupRequired, recoveryCodes, id, userCode, name, email, authorities[] }
class AuthSessionModel extends AuthSession {
  const AuthSessionModel({
    required super.stage,
    required super.user,
    super.mustChangePassword,
    super.twoFactorRequired,
    super.twoFactorSetupRequired,
    super.recoveryCodes,
  });

  factory AuthSessionModel.fromJson(Map<String, dynamic> json) {
    // User fields are at the root of LoginResponse; accept a nested "user" too
    final userJson = json['user'] is Map<String, dynamic>
        ? json['user'] as Map<String, dynamic>
        : json;

    return AuthSessionModel(
      stage: AuthStage.fromApi(json['stage'] as String?),
      user: UserInfoModel.fromJson(userJson),
      mustChangePassword: json['mustChangePassword'] as bool? ?? false,
      twoFactorRequired: json['twoFactorRequired'] as bool? ?? false,
      twoFactorSetupRequired: json['twoFactorSetupRequired'] as bool? ?? false,
      recoveryCodes: (json['recoveryCodes'] as List<dynamic>?)
              ?.whereType<String>()
              .toList(growable: false) ??
          const [],
    );
  }

  /// null when the body is not a session (e.g. only { "message": ... } or 204)
  static AuthSessionModel? tryFromJson(Object? data) {
    if (data is! Map<String, dynamic>) return null;
    if (!data.containsKey('stage') && !data.containsKey('email')) return null;
    return AuthSessionModel.fromJson(data);
  }
}
