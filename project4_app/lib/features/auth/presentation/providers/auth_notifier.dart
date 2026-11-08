import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../domain/entities/auth_session.dart';
import 'auth_providers.dart';
import 'auth_state.dart';

/// The sign-in session of the whole app. Only calls USE CASES.
/// Screens never navigate after sign-in changes: they change this state,
/// go_router redirect moves to the right screen.
class AuthNotifier extends Notifier<AuthState> {
  @override
  AuthState build() {
    // Ask the server if a session is still alive (app start)
    Future.microtask(_restore);
    return const AuthState.unknown();
  }

  Future<void> _restore() async {
    try {
      final session = await ref.read(getAuthStatusUseCaseProvider)();
      state = session == null
          ? const AuthState.unauthenticated()
          : AuthState.fromSession(session);
    } on ApiException catch (e) {
      state = AuthState.unauthenticated(message: e.message); // e.g. offline
    }
  }

  // ---------- Sign-in flow ----------

  Future<void> login(String email, String password) async {
    final session = await ref.read(loginUseCaseProvider)(
      email: email,
      password: password,
    );
    state = AuthState.fromSession(session);
  }

  Future<void> verifyOtp(String code) =>
      _pendingStep(() => ref.read(verifyOtpUseCaseProvider)(code: code));

  Future<void> recoverWithCode(String recoveryCode) => _pendingStep(
      () => ref.read(recoverWithCodeUseCaseProvider)(recoveryCode: recoveryCode));

  /// Stage SETUP_2FA: returns the session WITHOUT applying it, so the
  /// screen can show the recovery codes first, then call [applySession].
  Future<AuthSession> confirmTwoFactor(String code) =>
      ref.read(confirmTwoFactorUseCaseProvider)(code: code);

  void applySession(AuthSession session) {
    state = AuthState.fromSession(session);
  }

  /// Returns false when the password was changed but the user must sign
  /// in again (server ended the session).
  Future<bool> changePassword(String current, String newPassword) async {
    final session = await ref.read(changePasswordUseCaseProvider)(
      currentPassword: current,
      newPassword: newPassword,
    );
    if (session == null) {
      state = const AuthState.unauthenticated(
        message: 'Password changed. Please sign in with your new password.',
      );
      return false;
    }
    state = AuthState.fromSession(session);
    return true;
  }

  /// Reload GET /api/auth/status (pull to refresh on Home)
  Future<void> refreshStatus() async {
    final session = await ref.read(getAuthStatusUseCaseProvider)();
    state = session == null
        ? const AuthState.unauthenticated(
            message: 'Your session has expired. Please sign in again.')
        : AuthState.fromSession(session);
  }

  // ---------- Sign out ----------

  /// Sign out, or give up a pending step (OTP / forced screens)
  Future<void> logout({String? message}) async {
    await ref.read(logoutUseCaseProvider)();
    state = AuthState.unauthenticated(message: message);
  }

  /// Called by Dio when a normal API answers 401
  void handleSessionExpired() {
    if (state.status == AuthStatus.unknown ||
        state.status == AuthStatus.unauthenticated) {
      return;
    }
    ref.read(logoutUseCaseProvider)(); // clears cookies (server call may 401)
    state = const AuthState.unauthenticated(
      message: 'Your session has expired. Please sign in again.',
    );
  }

  // ---------- Helper ----------

  /// OTP / recovery code: 401 = the pending sign-in expired (5 minutes)
  Future<void> _pendingStep(Future<AuthSession> Function() step) async {
    try {
      state = AuthState.fromSession(await step());
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await ref.read(logoutUseCaseProvider)();
        state = AuthState.unauthenticated(message: e.message);
      }
      rethrow; // 400 wrong code → stay on the screen
    }
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(AuthNotifier.new);
