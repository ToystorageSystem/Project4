/// Result of POST /api/2fa/setup
class TwoFactorSetup {
  final String secret; // for manual entry in Google Authenticator
  final String otpAuthUri; // for QR code

  const TwoFactorSetup({required this.secret, required this.otpAuthUri});
}
