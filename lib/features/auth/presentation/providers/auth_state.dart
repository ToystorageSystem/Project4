import '../../domain/entities/auth_session.dart';
import '../../domain/entities/user_info.dart';

/// unknown = checking at app start; the rest follow the backend stage
enum AuthStatus {
  unknown,
  unauthenticated,
  changePassword,
  setupTwoFactor,
  verifyTwoFactor,
  authenticated,
}

class AuthState {
  final AuthStatus status;
  final AuthSession? session; // null when unknown / unauthenticated
  final String? message; // shown on Login (e.g. "session expired")

  const AuthState._(this.status, {this.session, this.message});

  const AuthState.unknown() : this._(AuthStatus.unknown);

  const AuthState.unauthenticated({String? message})
    : this._(AuthStatus.unauthenticated, message: message);

  factory AuthState.fromSession(AuthSession session) {
    final status = switch (session.stage) {
      AuthStage.changePassword => AuthStatus.changePassword,
      AuthStage.setupTwoFactor => AuthStatus.setupTwoFactor,
      AuthStage.verifyTwoFactor => AuthStatus.verifyTwoFactor,
      AuthStage.authenticated => AuthStatus.authenticated,
    };
    return AuthState._(status, session: session);
  }

  UserInfo? get user => session?.user;
  bool get isAuthenticated => status == AuthStatus.authenticated;
  bool get isStoreUser => user?.isStoreUser ?? false;
  bool get isStoreManager => user?.isStoreManager ?? false;
}
