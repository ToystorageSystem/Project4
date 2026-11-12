import 'package:dio/dio.dart';

import '../../../../core/constants/api_constants.dart';
import '../models/auth_session_model.dart';
import '../models/two_factor_setup_model.dart';

/// Talks to the backend with Dio (cookies + CSRF handled by interceptors).
/// May throw DioException – RepositoryImpl converts it to ApiException.
class AuthRemoteDataSource {
  final Dio _dio;

  AuthRemoteDataSource(this._dio);

  Future<AuthSessionModel> login(String email, String password) async {
    final res = await _dio.post<Map<String, dynamic>>(
      ApiConstants.login,
      data: {'email': email, 'password': password},
    );
    return AuthSessionModel.fromJson(res.data!);
  }

  Future<AuthSessionModel?> verifyTwoFactor(String code) async {
    final res = await _dio.post<dynamic>(
      ApiConstants.twoFactorVerify,
      data: {'code': code},
    );
    return AuthSessionModel.tryFromJson(res.data);
  }

  Future<AuthSessionModel?> recoverWithCode(String recoveryCode) async {
    final res = await _dio.post<dynamic>(
      ApiConstants.twoFactorRecover,
      data: {'code': recoveryCode},
    );
    return AuthSessionModel.tryFromJson(res.data);
  }

  Future<AuthSessionModel> getStatus() async {
    final res = await _dio.get<Map<String, dynamic>>(ApiConstants.status);
    return AuthSessionModel.fromJson(res.data ?? const {});
  }

  Future<AuthSessionModel?> changePassword(
    String currentPassword,
    String newPassword,
  ) async {
    final res = await _dio.post<dynamic>(
      ApiConstants.changePassword,
      data: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    return AuthSessionModel.tryFromJson(res.data);
  }

  Future<TwoFactorSetupModel> setupTwoFactor({
    required String accountEmail,
  }) async {
    // Backend maps this endpoint as GET (POST → "Request method 'POST' is not supported")
    final res = await _dio.get<Map<String, dynamic>>(
      ApiConstants.twoFactorSetup,
    );
    return TwoFactorSetupModel.fromJson(
      res.data ?? const {},
      accountEmail: accountEmail,
    );
  }

  Future<AuthSessionModel?> confirmTwoFactor(String code) async {
    final res = await _dio.post<dynamic>(
      ApiConstants.twoFactorConfirm,
      data: {'code': code},
    );
    return AuthSessionModel.tryFromJson(res.data);
  }

  Future<void> logout() async {
    await _dio.post<dynamic>(ApiConstants.logout); // 204 No Content
  }
}
