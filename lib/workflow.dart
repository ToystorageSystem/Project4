import 'api.dart';

List<Json> documentItems(Json document) =>
    (document['items'] as List? ?? [])
        .map((value) => Map<String, dynamic>.from(value as Map))
        .toList();

String countField(Json task) =>
    task['status'] == 'RECOUNTING' ||
            (task['status'] != 'COUNTING' &&
                documentItems(
                  task,
                ).any((item) => item['secondCountQuantity'] != null))
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
    throw Exception('Máy chủ không trả về kết quả công việc hợp lệ.');
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
    'Đã nhận phản hồi không đầy đủ. Tải lại phiếu trước khi ghi tiếp.',
  );
}

String? validateScannedItem(
  int module,
  Json item,
  String barcode,
  String location,
  int quantity,
) {
  if (barcode != item['barcode']) return 'Mã vạch không khớp sản phẩm đã chọn.';
  final expectedLocation =
      module == 1 ? item['toLocationCode'] : item['locationCode'];
  if (location != expectedLocation)
    return 'Mã vị trí không khớp vị trí của dòng đã chọn.';
  if (quantity < (module == 3 ? 0 : 1))
    return module == 3
        ? 'Số đếm phải từ 0 trở lên.'
        : 'Số lượng phải lớn hơn 0.';
  if (module != 3 && quantity > (item['remainingQuantity'] as num? ?? 0))
    return 'Số lượng vượt phần còn lại.';
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
