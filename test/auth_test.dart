import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:toystorage_staff/auth.dart';
import 'package:toystorage_staff/main.dart';

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
    expect(isWarehouseStaff({}), isFalse);
  });
  testWidgets('Shared login has password recovery, no demo or server field', (
    tester,
  ) async {
    await tester.pumpWidget(const StaffApp());
    expect(find.text('Toy Storage System'), findsOneWidget);
    expect(find.textContaining('demo'), findsNothing);
    expect(find.text('Cấu hình kết nối'), findsNothing);
    await tester.ensureVisible(find.text('Quên mật khẩu?'));
    await tester.tap(find.text('Quên mật khẩu?'));
    await tester.pumpAndSettle();
    expect(find.text('Gửi mã khôi phục'), findsOneWidget);
    await tester.ensureVisible(find.text('Gửi mã khôi phục'));
    await tester.tap(find.text('Gửi mã khôi phục'));
    await tester.pump();
    expect(find.text('Vui lòng nhập email hợp lệ.'), findsOneWidget);
    await tester.ensureVisible(find.text('Quay lại đăng nhập'));
    await tester.tap(find.text('Quay lại đăng nhập'));
    await tester.pumpAndSettle();
    expect(find.text('Đăng nhập'), findsOneWidget);
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
    await tester.ensureVisible(find.text('Đăng nhập'));
    expect(tester.takeException(), isNull);
  });
}
