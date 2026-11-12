import 'api.dart';

List<Json> documentItems(Json document) => (document['items'] as List? ?? [])
    .map((value) => Map<String, dynamic>.from(value as Map))
    .toList();

String countField(Json task) =>
    task['status'] == 'RECOUNTING' ||
        (task['status'] != 'COUNTING' &&
            documentItems(task)
                .any((item) => item['secondCountQuantity'] != null))
    ? 'secondCountQuantity'
    : 'firstCountQuantity';

bool itemComplete(int module, Json task, Json item) => switch (module) {
  0 => item['inspected'] == true,
  3 => item[countField(task)] != null,
  _ => (item['remainingQuantity'] as num?) == 0,
};

bool taskComplete(int module, Json task) {
  final items = documentItems(task);
  return items.isNotEmpty &&
      items.every((item) => itemComplete(module, task, item));
}

/// Merge a single-item response instead of issuing an extra GET after a successful
/// quantity increment. A failed GET must not tempt the operator to submit it twice.
Json applyTaskResponse(int module, Json task, dynamic response) {
  if (response is! Map)
    throw Exception('The server returned an invalid task result.');
  final data = Map<String, dynamic>.from(response);
  if (data.containsKey(modules[module].idKey)) return data;
  if (data['itemId'] != null) {
    final items = documentItems(task);
    final index = items.indexWhere((item) => item['itemId'] == data['itemId']);
    if (index >= 0) {
      items[index] = {...items[index], ...data};
      return {...task, 'items': items};
    }
  }
  throw Exception(
    'The server response was incomplete. Reload the task before another update.',
  );
}

String? validateScannedItem(
  int module,
  Json item,
  String barcode,
  String location,
  int quantity,
) {
  if (barcode != item['barcode'])
    return 'Barcode does not match the selected product.';
  final expectedLocation = module == 1
      ? item['toLocationCode']
      : item['locationCode'];
  if (location.trim().isEmpty) return 'Scan or enter a location code.';
  // A new putaway line deliberately has no destination. The server validates
  // the first scanned location; subsequent increments must use the saved one.
  if ((module != 1 || '${expectedLocation ?? ''}'.trim().isNotEmpty) &&
      location != expectedLocation)
    return 'Location code does not match the selected row.';
  if (quantity < (module == 3 ? 0 : 1))
    return module == 3
        ? 'Count must be zero or greater.'
        : 'Quantity must be greater than zero.';
  if (module != 3 && quantity > (item['remainingQuantity'] as num? ?? 0))
    return 'Quantity exceeds the remaining amount.';
  return null;
}

bool packingReady(Json summary) {
  final packages = (summary['packages'] as List? ?? []).whereType<Map>();
  return packages.isNotEmpty &&
      (summary['totalPickedQuantity'] as num? ?? 0) > 0 &&
      summary['totalPackedQuantity'] == summary['totalPickedQuantity'] &&
      packages.every(
        (p) =>
            p['packageStatus'] == 'PACKED' &&
            '${p['sealNumber'] ?? ''}'.trim().isNotEmpty,
      );
}
