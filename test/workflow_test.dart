import 'package:flutter_test/flutter_test.dart';
import 'package:toystorage_staff/workflow.dart';

void main() {
  test('Zero is a valid stock count; recount requires the second count', () {
    final first = {
      'status': 'COUNTING',
      'items': [
        {'itemId': 1, 'firstCountQuantity': 0, 'secondCountQuantity': null},
      ],
    };
    expect(taskComplete(3, first), isTrue);
    final second = {...first, 'status': 'RECOUNTING'};
    expect(taskComplete(3, second), isFalse);
    expect(
      taskComplete(3, {
        ...second,
        'items': [
          {'itemId': 1, 'firstCountQuantity': 3, 'secondCountQuantity': 0},
        ],
      }),
      isTrue,
    );
    expect(
      countField({
        'status': 'PENDING_CONFIRMATION',
        'items': [
          {'secondCountQuantity': 0},
        ],
      }),
      'secondCountQuantity',
    );
  });
  test(
    'Single-item response is merged without losing other lines or status',
    () {
      final task = {
        'taskId': 7,
        'status': 'IN_PROGRESS',
        'items': [
          {'itemId': 10, 'remainingQuantity': 4},
          {'itemId': 11, 'remainingQuantity': 2},
        ],
      };
      final merged = applyTaskResponse(1, task, {
        'itemId': 10,
        'remainingQuantity': 1,
        'putawayQuantity': 3,
      });
      expect(merged['status'], 'IN_PROGRESS');
      expect(documentItems(merged)[0]['remainingQuantity'], 1);
      expect(documentItems(merged)[1]['remainingQuantity'], 2);
      expect(documentItems(task)[0]['remainingQuantity'], 4);
      expect(taskComplete(1, merged), isFalse);
    },
  );
  test(
    'Validate the chosen item, location and quantity before any mutation',
    () {
      final item = {
        'barcode': '893001',
        'toLocationCode': 'A-01',
        'locationCode': 'B-02',
        'remainingQuantity': 3,
      };
      expect(validateScannedItem(1, item, '893001', 'A-01', 3), isNull);
      expect(validateScannedItem(1, item, '893002', 'A-01', 3), isNotNull);
      expect(validateScannedItem(1, item, '893001', 'B-02', 3), isNotNull);
      expect(validateScannedItem(1, item, '893001', 'A-01', 4), isNotNull);
      expect(validateScannedItem(2, item, '893001', 'B-02', 0), isNotNull);
      expect(validateScannedItem(3, item, '893001', 'B-02', 0), isNull);
    },
  );
  test(
    'Packing requires exact totals, sealed nonempty package list and seal numbers',
    () {
      final packed = {
        'totalPickedQuantity': 4,
        'totalPackedQuantity': 4,
        'packages': [
          {'packageStatus': 'PACKED', 'sealNumber': 'SEAL-1'},
        ],
      };
      expect(packingReady(packed), isTrue);
      expect(packingReady({...packed, 'totalPackedQuantity': 3}), isFalse);
      expect(
        packingReady({
          ...packed,
          'packages': [
            {'packageStatus': 'PACKED', 'sealNumber': ''},
          ],
        }),
        isFalse,
      );
      expect(packingReady({...packed, 'packages': []}), isFalse);
      expect(
        packingReady({
          ...packed,
          'totalPickedQuantity': 0,
          'totalPackedQuantity': 0,
        }),
        isFalse,
      );
    },
  );
}
