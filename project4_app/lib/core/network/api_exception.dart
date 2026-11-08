/// The ONLY error type screens/use cases need to know.
/// Pure Dart (no Dio) so the domain layer may use it too.
class ApiException implements Exception {
  final String message;
  final int? statusCode;

  const ApiException(this.message, {this.statusCode});

  bool get isBadRequest => statusCode == 400;
  bool get isUnauthorized => statusCode == 401; // not signed in / session expired
  bool get isForbidden => statusCode == 403; // no permission / not a store user
  bool get isNotFound => statusCode == 404;
  bool get isConflict => statusCode == 409; // changed by someone else / wrong auth stage

  @override
  String toString() => message;
}
