import 'package:dio/dio.dart';

import 'api_exception.dart';

/// Run a request; convert any DioException into our ApiException.
/// Every RepositoryImpl wraps its calls with this.
Future<T> apiCall<T>(Future<T> Function() request) async {
  try {
    return await request();
  } on DioException catch (e) {
    throw apiExceptionFromDio(e);
  }
}

/// Backend error bodies:
///  - auth:   { "status": 401, "message": "..." }
///  - others: { "timestamp": ..., "status": 404, "error": "...", "message": "..." }
///  - Spring Security: 401 / 403 with EMPTY body
ApiException apiExceptionFromDio(DioException e) {
  switch (e.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
      return const ApiException('Connection timed out. Please try again.');

    case DioExceptionType.connectionError:
      return const ApiException(
        'Cannot connect to server. Please check your network.',
      );

    case DioExceptionType.badResponse:
      final status = e.response?.statusCode;
      final data = e.response?.data;
      final message =
          (data is Map &&
              data['message'] is String &&
              (data['message'] as String).trim().isNotEmpty)
          ? data['message'] as String
          : _defaultMessage(status);
      return ApiException(message, statusCode: status);

    default:
      return const ApiException('Something went wrong. Please try again.');
  }
}

String _defaultMessage(int? status) => switch (status) {
  401 => 'Your session has expired. Please sign in again.',
  403 => 'You do not have permission to do this.',
  404 => 'Not found.',
  409 => 'Data was changed by someone else. Please reload.',
  _ => 'Server error ($status)',
};

/// true when the server sent no body (typical Spring Security 401/403)
bool isEmptyBody(Object? data) =>
    data == null ||
    (data is String && data.trim().isEmpty) ||
    (data is Map && data.isEmpty);
