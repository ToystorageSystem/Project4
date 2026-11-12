import 'package:dio/dio.dart';

import 'platform_transport_native.dart'
    if (dart.library.html) 'platform_transport_web.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/presentation/providers/auth_notifier.dart';
import '../constants/api_constants.dart';
import 'csrf_interceptor.dart';
import 'session_store.dart';

final dioProvider = Provider<Dio>((ref) {
  final session = ref.watch(sessionStoreProvider);

  final options = BaseOptions(
    baseUrl: ApiConstants.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 20),
    contentType: Headers.jsonContentType,
    headers: {'Accept': 'application/json'},
  );

  final dio = Dio(options);

  // Small client ONLY for GET /api/auth/csrf (shares the same cookies,
  // has no CSRF interceptor → no infinite loop)
  final csrfDio = Dio(options);
  configureTransport(csrfDio, session.cookieJar);

  dio.interceptors.addAll([
    // 1. X-CSRF-TOKEN for POST/PUT/PATCH/DELETE.
    //    MUST run BEFORE the CookieManager: GET /csrf may create a new
    //    session (new JSESSIONID). If cookies were attached first, the
    //    POST would carry no/old JSESSIONID → token mismatch → 403.
    CsrfInterceptor(dio, csrfDio, session),
    // 2. 401 on a normal API = session expired (15 min idle / signed in
    //    on another device / security setup changed) → back to Login
    InterceptorsWrapper(
      onError: (error, handler) {
        final path = error.requestOptions.path;
        if (error.response?.statusCode == 401 &&
            !ApiConstants.authFlowPaths.contains(path)) {
          ref.read(authProvider.notifier).handleSessionExpired();
        }
        handler.next(error); // the caller still receives the error
      },
    ),
  ]);

  // 3. Send/save JSESSIONID cookie (native: CookieManager; web: browser).
  //    Added last so it reads the cookie jar AFTER /csrf has updated it.
  configureTransport(dio, session.cookieJar);

  return dio;
});
