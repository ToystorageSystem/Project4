import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:project4_app/warehouse/main.dart';
import 'package:project4_app/warehouse/api.dart';

void main() {
  testWidgets('Empty credentials stay on login and show validation', (
    tester,
  ) async {
    await tester.pumpWidget(const StaffApp());
    await tester.ensureVisible(find.text('Sign in'));
    await tester.tap(find.text('Sign in'));
    await tester.pump();
    expect(find.text('Enter your email and password.'), findsOneWidget);
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
    await tester.tap(find.text('Accept and start'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Complete putaway'), 200);
    final button = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Complete putaway'),
    );
    expect(button.onPressed, isNull);
    expect(row['status'], 'IN_PROGRESS');
  });
}
