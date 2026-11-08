import 'package:dio/dio.dart';

import '../constants/api_constants.dart';
import 'api_call.dart';
import 'session_store.dart';

/// Adds the CSRF header to every POST/PUT/PATCH/DELETE.
/// - No token yet → GET /api/auth/csrf first.
/// - After login / 2FA / password change / logout → forget the token
///   (backend session changes, old token becomes invalid).
/// - 403 with EMPTY body on an unsafe request = CSRF rejected
///   → fetch a new token and retry ONCE.
/// QueuedInterceptor: requests wait for each other, so the token is
/// fetched only once even if several requests start together.
class CsrfInterceptor extends QueuedInterceptor {
  final Dio _dio; // main client – used to retry
  final Dio _csrfDio; // plain client (cookies only) – used to fetch token
  final SessionStore _session;

  /// (main client, csrf client, session) – positional so fields can be
  /// initialized directly (private names cannot be named parameters)
  CsrfInterceptor(this._dio, this._csrfDio, this._session);

  static const _unsafeMethods = {'POST', 'PUT', 'PATCH', 'DELETE'};
  static const _retriedKey = 'csrfRetried';

  /// Successful calls to these change the backend session
  static const _sessionChangingPaths = {
    ApiConstants.login,
    ApiConstants.twoFactorVerify,
    ApiConstants.twoFactorRecover,
    ApiConstants.twoFactorConfirm,
    ApiConstants.changePassword,
    ApiConstants.logout,
  };

  bool _needsToken(RequestOptions o) =>
      _unsafeMethods.contains(o.method.toUpperCase());

  Future<void> _fetchToken() async {
    final res = await _csrfDio.get<Map<String, dynamic>>(ApiConstants.csrf);
    final data = res.data ?? const <String, dynamic>{};
    _session.csrfToken = data['token'] as String?;
    final name = data['headerName'] as String?;
    if (name != null && name.isNotEmpty) _session.csrfHeaderName = name;
  }

  @override
  Future<void> onRequest(
      RequestOptions options, RequestInterceptorHandler handler) async {
    if (_needsToken(options)) {
      try {
        if (_session.csrfToken == null) await _fetchToken();
        final token = _session.csrfToken;
        if (token != null) options.headers[_session.csrfHeaderName] = token;
      } on DioException catch (e) {
        return handler.reject(DioException(
          requestOptions: options,
          response: e.response,
          type: e.type,
          error: e.error,
        ));
      }
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    if (_sessionChangingPaths.contains(response.requestOptions.path)) {
      _session.clearCsrf();
    }
    handler.next(response);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final options = err.requestOptions;
    final csrfRejected = err.response?.statusCode == 403 &&
        isEmptyBody(err.response?.data) &&
        _needsToken(options) &&
        options.extra[_retriedKey] != true;

    if (!csrfRejected) return handler.next(err);

    try {
      _session.clearCsrf();
      await _fetchToken();
      options.extra[_retriedKey] = true;
      final token = _session.csrfToken;
      if (token != null) options.headers[_session.csrfHeaderName] = token;
      final response = await _dio.fetch<dynamic>(options);
      return handler.resolve(response);
    } on DioException catch (e) {
      return handler.next(e);
    }
  }
}
