/// 2026-11-05T07:16:00 → "05/11/2026 07:16"   (null → "Never")
String formatDateTime(DateTime? dt) {
  if (dt == null) return 'Never';
  final d = dt.toLocal();
  String two(int n) => n.toString().padLeft(2, '0');
  return '${two(d.day)}/${two(d.month)}/${d.year} ${two(d.hour)}:${two(d.minute)}';
}
