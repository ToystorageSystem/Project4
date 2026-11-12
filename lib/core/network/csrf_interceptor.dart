import 'package:dio/dio.dart';

import '../constants/api_constants.dart';
import 'session_store.dart';

/// Fetch a fresh token per write. Never replay inventory/sale/handover mutations.
class CsrfInterceptor extends QueuedInterceptor {
  final Dio _csrfDio;
  final SessionStore _session;
  CsrfInterceptor(Dio main, this._csrfDio, this._session);
  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!{'GET', 'HEAD', 'OPTIONS'}.contains(options.method.toUpperCase())) {
      try {
        final response = await _csrfDio.get<Map<String, dynamic>>(
          ApiConstants.csrf,
        );
        final data = response.data!;
        final token = data['token'];
        if (token is! String || token.isEmpty)
          throw StateError('CSRF token missing');
        options.headers[data['headerName'] as String? ?? 'X-CSRF-TOKEN'] =
            token;
      } catch (error) {
        return handler.reject(
          DioException(requestOptions: options, error: error),
        );
      }
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    _session.clearCsrf();
    handler.next(response);
  }
}
