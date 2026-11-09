import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Holds the login session of the app:
///  - cookies (JSESSIONID) → CookieJar, kept in memory only:
///    closing the app = signed out (backend session also expires after
///    15 minutes idle, 1 session per user).
///  - current CSRF token (+ header name) for POST/PUT/PATCH/DELETE.
class SessionStore {
  final CookieJar cookieJar;
  String? csrfToken;
  String csrfHeaderName = 'X-CSRF-TOKEN';

  SessionStore(this.cookieJar);

  /// Session id changes after login / 2FA / logout → token must be reloaded
  void clearCsrf() => csrfToken = null;

  /// Forget everything (logout, session expired)
  Future<void> clear() async {
    csrfToken = null;
    await cookieJar.deleteAll();
  }
}

final sessionStoreProvider = Provider<SessionStore>(
  (ref) => SessionStore(CookieJar()),
);
