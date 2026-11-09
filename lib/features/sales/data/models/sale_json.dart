// Small helpers to read sale JSON safely (data layer only).
// Money is BigDecimal on the backend → JSON number (sometimes a string).

double readMoney(Object? v) =>
    v is num ? v.toDouble() : double.tryParse('${v ?? ''}') ?? 0;

int readInt(Object? v) => (v as num?)?.toInt() ?? 0;

DateTime? readDate(Object? v) => v is String ? DateTime.tryParse(v) : null;
