import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:project4_app/warehouse/auth.dart';
import 'package:project4_app/warehouse/main.dart';

void main() {
  test('Only exact warehouse staff roles enter staff workspace', () {
    expect(
      isWarehouseStaff({
        'authorities': ['ROLE_WAREHOUSE_STAFF'],
      }),
      isTrue,
    );
    expect(
      isWarehouseStaff({
        'authorities': ['WAREHOUSE_STAFF'],
      }),
      isTrue,
    );
    expect(
      isWarehouseStaff({
        'authorities': [
          {'authority': 'ROLE_WAREHOUSE_STAFF'},
        ],
      }),
      isTrue,
    );
    expect(
      isWarehouseStaff({
        'authorities': ['ROLE_WAREHOUSE_MANAGER'],
      }),
      isFalse,
    );
    expect(
      isWarehouseStaff({
        'authorities': ['STAFF_VIEW'],
      }),
      isFalse,
    );
    for (final role in [
      'ROLE_ROLE-WH-STAFF',
      'ROLE-WH-STAFF',
      ' ROLE_ROLE_WH_STAFF ',
    ]) {
      expect(
        isWarehouseStaff({
          'authorities': [role],
        }),
        isTrue,
      );
    }
    for (final role in [
      'ROLE_ROLE-WH-MANAGER',
      'ROLE_STORE_STAFF',
      'ROLE_ROLE-WH-STAFF_EXTRA',
    ]) {
      expect(
        isWarehouseStaff({
          'authorities': [role],
        }),
        isFalse,
      );
    }
    expect(isWarehouseStaff({}), isFalse);
  });
  testWidgets('Shared login has password recovery, no demo or server field', (
    tester,
  ) async {
    await tester.pumpWidget(const StaffApp());
    expect(find.text('Toy Storage System'), findsOneWidget);
    expect(find.textContaining('demo'), findsNothing);
    expect(find.text('Server configuration'), findsNothing);
    await tester.ensureVisible(find.text('Forgot password?'));
    await tester.tap(find.text('Forgot password?'));
    await tester.pumpAndSettle();
    expect(find.text('Send recovery code'), findsOneWidget);
    await tester.ensureVisible(find.text('Send recovery code'));
    await tester.tap(find.text('Send recovery code'));
    await tester.pump();
    expect(find.text('Enter a valid email address.'), findsOneWidget);
    await tester.ensureVisible(find.text('Back to sign in'));
    await tester.tap(find.text('Back to sign in'));
    await tester.pumpAndSettle();
    expect(find.text('Sign in'), findsOneWidget);
  });
  testWidgets('Login scrolls on a small phone with large text', (tester) async {
    tester.view.physicalSize = const Size(320, 568);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(
            size: Size(320, 568),
            textScaler: TextScaler.linear(1.5),
          ),
          child: const LoginPage(),
        ),
      ),
    );
    await tester.ensureVisible(find.text('Sign in'));
    expect(tester.takeException(), isNull);
  });
}
