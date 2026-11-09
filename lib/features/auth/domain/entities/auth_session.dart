import 'user_info.dart';

/// Where the user is in the sign-in flow (backend LoginResponse.stage)
enum AuthStage {
  changePassword, // CHANGE_PASSWORD – must set a new password first
  setupTwoFactor, // SETUP_2FA – must enable Google Authenticator
  verifyTwoFactor, // VERIFY_2FA – must enter the 6-digit code
  authenticated; // AUTHENTICATED – fully signed in

  static AuthStage fromApi(String? value) => switch (value) {
    'CHANGE_PASSWORD' => AuthStage.changePassword,
    'SETUP_2FA' => AuthStage.setupTwoFactor,
    'VERIFY_2FA' => AuthStage.verifyTwoFactor,
    _ => AuthStage.authenticated,
  };
}

/// Result of login / 2FA / status calls
class AuthSession {
  final AuthStage stage;
  final UserInfo user;
  final bool mustChangePassword;
  final bool twoFactorRequired;
  final bool twoFactorSetupRequired;

  /// Only after enabling 2FA: one-time codes to sign in without the phone
  final List<String> recoveryCodes;

  const AuthSession({
    required this.stage,
    required this.user,
    this.mustChangePassword = false,
    this.twoFactorRequired = false,
    this.twoFactorSetupRequired = false,
    this.recoveryCodes = const [],
  });

  bool get isAuthenticated => stage == AuthStage.authenticated;
}
