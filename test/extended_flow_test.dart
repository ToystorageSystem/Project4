import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:toystorage_staff/api.dart';
import 'package:toystorage_staff/staff_features.dart';

class ReturnAdapter implements HttpClientAdapter {
  int writes = 0;
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.method != 'GET') writes++;
    return ResponseBody.fromString(
      jsonEncode({
        'returnId': 201,
        'returnCode': 'SR-TEST',
        'status': options.path.endsWith('/submit') ? 'RECEIVED' : 'INSPECTING',
        'storeName': 'Test store',
        'items': [
          {
            'itemId': 1,
            'productId': 11,
            'productName': 'Product',
            'barcode': '123',
            'expectedQuantity': 5,
            'receivedQuantity': 5,
            'conditionStatus': 'NORMAL',
          },
        ],
      }),
      200,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  test('Packing bookmarks are isolated by server and account', () async {
    SharedPreferences.setMockInitialValues({});
    final a = WarehouseApi('https://one.example')..accountScope = 'staff-a';
    await a.rememberPacking(42);
    await a.rememberPacking(42);
    expect(await a.packingIds(), [42]);
    a.accountScope = 'staff-b';
    expect(await a.packingIds(), isEmpty);
    final b = WarehouseApi('https://two.example')..accountScope = 'staff-a';
    expect(await b.packingIds(), isEmpty);
  });
  testWidgets(
    'Return completion requires explicit confirmation before calling backend',
    (tester) async {
      final adapter = ReturnAdapter();
      final api = WarehouseApi('https://test.example');
      api.dio.httpClientAdapter = adapter;
      await tester.pumpWidget(
        MaterialApp(
          home: ReturnPage(api: api, demo: false, initial: {'returnId': 201}),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Hoàn tất nhận hàng trả'), 200);
      await tester.tap(find.text('Hoàn tất nhận hàng trả'));
      await tester.pumpAndSettle();
      expect(adapter.writes, 0);
      expect(find.text('Hoàn tất nhận hàng trả?'), findsOneWidget);
      await tester.tap(find.text('Kiểm tra lại'));
      await tester.pumpAndSettle();
      expect(adapter.writes, 0);
      await tester.tap(find.text('Hoàn tất nhận hàng trả'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Xác nhận nhận hàng'));
      await tester.pumpAndSettle();
      expect(adapter.writes, 1);
      expect(find.text('Đã nhận'), findsOneWidget);
    },
  );
  testWidgets('Demo return cannot submit until every item is inspected', (
    tester,
  ) async {
    final row = sampleFeature(0).first..['status'] = 'INSPECTING';
    await tester.pumpWidget(
      MaterialApp(
        home: ReturnPage(
          api: WarehouseApi('https://test.example'),
          demo: true,
          initial: row,
        ),
      ),
    );
    await tester.scrollUntilVisible(find.text('Hoàn tất nhận hàng trả'), 200);
    expect(
      tester
          .widget<FilledButton>(
            find.widgetWithText(FilledButton, 'Hoàn tất nhận hàng trả'),
          )
          .onPressed,
      isNull,
    );
  });
}
