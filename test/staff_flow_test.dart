import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:toystorage_staff/main.dart';
import 'package:toystorage_staff/api.dart';

void main() {
  testWidgets('Empty credentials stay on login and show validation', (
    tester,
  ) async {
    await tester.pumpWidget(const StaffApp());
    await tester.ensureVisible(find.text('Đăng nhập'));
    await tester.tap(find.text('Đăng nhập'));
    await tester.pump();
    expect(find.text('Nhập email và mật khẩu.'), findsOneWidget);
  });
  testWidgets('Cannot complete putaway before all quantities are recorded', (
    tester,
  ) async {
    final row = demoRows(1).first;
    await tester.pumpWidget(
      MaterialApp(
        home: TaskPage(
          api: WarehouseApi('http://localhost:8080'),
          demo: true,
          module: 1,
          initial: row,
        ),
      ),
    );
    await tester.tap(find.text('Nhận việc và bắt đầu'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Hoàn tất cất hàng'), 200);
    final button = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Hoàn tất cất hàng'),
    );
    expect(button.onPressed, isNull);
    expect(row['status'], 'IN_PROGRESS');
  });
}
