import '../../domain/entities/two_factor_setup.dart';

class TwoFactorSetupModel extends TwoFactorSetup {
  const TwoFactorSetupModel({required super.secret, required super.otpAuthUri});

  /// AuthService.setup returns {qrDataUrl, manualKey, otpauthUri};
  /// AuthProtectionController returns {secret, otpauthUri} → accept both.
  /// Builds the otpauth URI from the secret if the server does not send one.
  factory TwoFactorSetupModel.fromJson(
    Map<String, dynamic> json, {
    required String accountEmail,
  }) {
    final secret =
        _firstString(json, const ['manualKey', 'secret', 'secretKey', 'key']) ??
        '';
    final uri =
        _firstString(json, const [
          'otpAuthUri',
          'otpauthUri',
          'otpAuthUrl',
          'otpauthUrl',
          'qrCodeUri',
          'qrUri',
          'uri',
        ]) ??
        _buildUri(secret, accountEmail);
    return TwoFactorSetupModel(secret: secret, otpAuthUri: uri);
  }

  static String? _firstString(Map<String, dynamic> json, List<String> keys) {
    for (final k in keys) {
      final v = json[k];
      if (v is String && v.isNotEmpty) return v;
    }
    return null;
  }

  static String _buildUri(String secret, String email) {
    const issuer = 'ToyStorage';
    final label = Uri.encodeComponent('$issuer:$email');
    return 'otpauth://totp/$label?secret=$secret&issuer=$issuer'
        '&algorithm=SHA1&digits=6&period=30';
  }
}
