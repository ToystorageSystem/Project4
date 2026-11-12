// Small helpers to read backend JSON safely (data layer only)

int readInt(Object? v, [int fallback = 0]) => (v as num?)?.toInt() ?? fallback;

int? readIntOrNull(Object? v) => (v as num?)?.toInt();

DateTime? readDate(Object? v) => v is String ? DateTime.tryParse(v) : null;

List<Map<String, dynamic>> readList(Object? v) =>
    (v as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .toList();
