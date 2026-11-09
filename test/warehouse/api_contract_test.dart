import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:project4_app/warehouse/api.dart';
import 'package:project4_app/warehouse/main.dart';
import 'package:project4_app/warehouse/staff_features.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ContractAdapter implements HttpClientAdapter {
  final dynamic Function(RequestOptions options) respond;
  final requests = <RequestOptions>[];
  ContractAdapter(this.respond);
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? stream,
    Future<void>? cancel,
  ) async {
    requests.add(options);
    final data = respond(options);
    return ResponseBody.fromString(
      jsonEncode(data),
      data is Map && data['_http'] is int ? data['_http'] as int : 200,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  testWidgets(
    'Packing queue loads from server even when device has no bookmarks',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      final adapter = ContractAdapter((o) {
        expect(o.method, 'GET');
        expect(o.path, '/api/warehouse/staff/packing-submissions/transfers');
        return [
          {
            'transferId': 42,
            'transferCode': 'ST-0042',
            'status': 'PACKING',
            'toWarehouseName': 'Store 2',
          },
        ];
      });
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await tester.pumpWidget(
        MaterialApp(home: PackingBookmarks(api: api, demo: false)),
      );
      await tester.pumpAndSettle();
      expect(find.text('ST-0042'), findsOneWidget);
      expect(find.textContaining('Store 2'), findsOneWidget);
      expect(adapter.requests.length, 1);
    },
  );

  testWidgets(
    'Unassigned putaway shelf can be saved once and response binds it',
    (tester) async {
      final item = <String, dynamic>{
        'itemId': 10,
        'productId': 1,
        'productName': 'Robot',
        'barcode': '893001',
        'toLocationCode': null,
        'remainingQuantity': 5,
        'putawayQuantity': 0,
      };
      final row = <String, dynamic>{
        'taskId': 7,
        'taskCode': 'PA-7',
        'status': 'IN_PROGRESS',
        'items': [item],
      };
      final adapter = ContractAdapter((o) {
        if (o.method == 'GET') return row;
        expect(o.path, '/api/warehouse/putaway/7/items/10');
        expect(o.data, {
          'productBarcode': '893001',
          'locationCode': 'A-01',
          'quantity': 2,
        });
        return {
          ...item,
          'toLocationCode': 'A-01',
          'toLocationId': 3,
          'putawayQuantity': 2,
          'remainingQuantity': 3,
        };
      });
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await tester.pumpWidget(
        MaterialApp(
          home: TaskPage(api: api, demo: false, module: 1, initial: row),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.textContaining('No shelf assigned'), findsOneWidget);
      await tester.scrollUntilVisible(find.text('Save'), 200);
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField).at(0), '893001');
      await tester.enterText(find.byType(TextFormField).at(1), 'A-01');
      await tester.enterText(find.byType(TextFormField).at(2), '2');
      await tester.ensureVisible(find.text('Confirm'));
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(find.text('A-01'), findsOneWidget);
      expect(find.textContaining('Remaining: 3'), findsOneWidget);
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField).at(0), '893001');
      await tester.enterText(find.byType(TextFormField).at(1), 'B-02');
      await tester.enterText(find.byType(TextFormField).at(2), '1');
      await tester.ensureVisible(find.text('Confirm'));
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(adapter.requests.where((o) => o.method == 'PATCH').length, 1);
      expect(
        find.textContaining('does not match the selected row'),
        findsOneWidget,
      );
      await tester.pump(const Duration(seconds: 1));
    },
  );
  test(
    'Server exceptions become actionable messages without retrying a write',
    () async {
      final adapter = ContractAdapter(
        (_) => {
          '_http': 500,
          'message': 'Cannot invoke getToLocation because null',
        },
      );
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await expectLater(
        api.call('PATCH', '/api/warehouse/putaway/7/start'),
        throwsA(
          predicate(
            (Object e) =>
                e.toString().contains('Server error (500)') &&
                !e.toString().contains('getToLocation'),
          ),
        ),
      );
      expect(adapter.requests.length, 1);
    },
  );

  testWidgets(
    'Putaway saves exact barcode/location/quantity and merges item response without a GET',
    (tester) async {
      final item = {
        'itemId': 10,
        'productId': 1,
        'productName': 'Robot',
        'productCode': 'TOY-1',
        'barcode': '893001',
        'toLocationCode': 'A-01',
        'remainingQuantity': 3,
        'putawayQuantity': 0,
      };
      final row = {
        'taskId': 7,
        'taskCode': 'PA-7',
        'status': 'IN_PROGRESS',
        'items': [item],
      };
      final adapter = ContractAdapter((o) {
        if (o.method == 'GET') return row;
        expect(o.method, 'PATCH');
        expect(o.path, '/api/warehouse/putaway/7/items/10');
        expect(o.data, {
          'productBarcode': '893001',
          'locationCode': 'A-01',
          'quantity': 2,
        });
        return {...item, 'putawayQuantity': 2, 'remainingQuantity': 1};
      });
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await tester.pumpWidget(
        MaterialApp(
          home: TaskPage(api: api, demo: false, module: 1, initial: row),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Save'), 200);
      await tester.tap(find.text('Save'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField).at(0), '893001');
      await tester.enterText(find.byType(TextFormField).at(1), 'A-01');
      await tester.enterText(find.byType(TextFormField).at(2), '2');
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(adapter.requests.where((o) => o.method == 'GET').length, 1);
      expect(adapter.requests.where((o) => o.method == 'PATCH').length, 1);
      expect(find.textContaining('Remaining: 1'), findsOneWidget);
      await tester.pump(const Duration(seconds: 1));
    },
  );
  testWidgets(
    'Receiving completion uses returned INSPECTED detail without querying restricted receiving-detail again',
    (tester) async {
      final row = {
        'receiptId': 8,
        'receiptCode': 'GR-8',
        'status': 'RECEIVING',
        'items': [
          {
            'productId': 1,
            'productName': 'Robot',
            'barcode': '893001',
            'expectedQuantity': 2,
            'actualQuantity': 2,
            'damagedQuantity': 0,
            'inspected': true,
          },
        ],
      };
      final adapter = ContractAdapter((o) {
        if (o.method == 'GET') return row;
        expect(o.method, 'PATCH');
        expect(o.path, '/api/warehouse/goods-receipts/8/finish-inspection');
        return {...row, 'status': 'INSPECTED'};
      });
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await tester.pumpWidget(
        MaterialApp(
          home: TaskPage(api: api, demo: false, module: 0, initial: row),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Complete receiving'), 250);
      await tester.tap(find.text('Complete receiving'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Complete'));
      await tester.pumpAndSettle();
      expect(adapter.requests.length, 2);
      expect(find.text('Awaiting manager approval'), findsOneWidget);
    },
  );
  test('Expired session notifies once, while invalid login never triggers session redirect', () async {
    int expirations = 0;
    final api = WarehouseApi('https://test.example')
      ..onUnauthorized = () => expirations++;
    api.dio.httpClientAdapter = ContractAdapter(
      (_) => {'_http': 401, 'message': 'Unauthorized'},
    );
    await expectLater(api.login('staff@example.com', 'wrong'), throwsException);
    expect(expirations, 0);
    await expectLater(api.call('GET', modules[0].listPath), throwsException);
    await expectLater(api.call('GET', modules[1].listPath), throwsException);
    expect(expirations, 1);
  });
  test(
    'Backend validation errors preserve message and do not retry mutations',
    () async {
      final adapter = ContractAdapter(
        (_) => {
          '_http': 400,
          'message': 'All items must be counted before submission',
        },
      );
      final api = WarehouseApi('https://test.example')
        ..dio.httpClientAdapter = adapter;
      await expectLater(
        api.call('PATCH', '/api/warehouse/staff/stock-counts/9/submit'),
        throwsA(
          predicate(
            (Object e) => e.toString().contains('All items must be counted'),
          ),
        ),
      );
      expect(adapter.requests.length, 1);
    },
  );
}
