/// 120000 → "120,000 ₫"   (null → "—")
String formatMoney(num? value) {
  if (value == null) return '—';
  final digits = value.round().abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(',');
    buffer.write(digits[i]);
  }
  return '${value < 0 ? '-' : ''}$buffer ₫';
}
