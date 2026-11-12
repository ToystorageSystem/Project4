import 'dart:convert';

/// Form validators. Return null = valid, String = error message.
/// Rules must match backend validation.
class Validators {
  // Backend (team code): new password at least 6 characters, at most 72 UTF-8 bytes
  static const passwordMinLength = 6;
  static const passwordMaxLength = 72;
  static const otpLength = 6;

  static final _emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');

  static String? required(
    String? value, {
    String message = 'This field is required',
  }) => (value == null || value.trim().isEmpty) ? message : null;

  static String? email(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Please enter your email';
    if (!_emailRegex.hasMatch(v)) return 'Invalid email address';
    return null;
  }

  /// Login password: only "not empty" (old/temporary passwords may be short)
  static String? loginPassword(String? value) =>
      (value == null || value.isEmpty) ? 'Please enter your password' : null;

  static String? newPassword(String? value, {String? currentPassword}) {
    final v = value ?? '';
    if (v.isEmpty) return 'Please enter a new password';
    if (v.length < passwordMinLength) {
      return 'At least $passwordMinLength characters';
    }
    if (utf8.encode(v).length > passwordMaxLength) {
      return 'At most $passwordMaxLength UTF-8 bytes';
    }
    if (currentPassword != null && v == currentPassword) {
      return 'New password must be different from current password';
    }
    return null;
  }

  static String? confirmPassword(String? value, String newPassword) =>
      value != newPassword ? 'Passwords do not match' : null;

  static bool isValidPasswordLength(String value) =>
      value.length >= passwordMinLength &&
      utf8.encode(value).length <= passwordMaxLength;

  /// Whole number >= 0 (quantities). Empty → error.
  static String? quantity(String? value) {
    final v = value?.trim() ?? '';
    if (v.isEmpty) return 'Required';
    final n = int.tryParse(v);
    if (n == null || n < 0) return 'Invalid number';
    return null;
  }
}
