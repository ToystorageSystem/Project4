import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:project4_app/warehouse/api.dart';
import 'package:project4_app/warehouse/auth.dart';

void main() {
  testWidgets('OTP is required before opening the role workspace', (
    tester,
  ) async {
    final api = WarehouseApi('https://test.example');
    final requests = <String>[];
    api.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options.path);
          handler.resolve(
            Response<dynamic>(
              requestOptions: options,
              statusCode: 200,
              data: {
                'mustChangePassword': false,
                'requiresTwoFactorSetup': false,
              },
            ),
          );
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: SecurityCheckpoint(
          api: api,
          user: {
            'name': 'Alex',
            'id': 7,
            'authorities': ['ROLE_MANAGER'],
            'requiresTwoFactor': true,
            'requiresTwoFactorSetup': false,
            'mustChangePassword': false,
          },
        ),
      ),
    );
    expect(find.text('Verify your identity'), findsOneWidget);
    expect(
      find.textContaining('A workspace for your role is not available yet.'),
      findsNothing,
    );
    await tester.enterText(find.byType(TextField), '123456');
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();
    expect(requests, ['/api/auth/2fa/verify']);
    expect(
      find.textContaining('A workspace for your role is not available yet.'),
      findsOneWidget,
    );
  });

  testWidgets('Temporary password rotates before second-factor enrollment', (
    tester,
  ) async {
    final api = WarehouseApi('https://test.example');
    final requests = <String>[];
    api.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options.path);
          handler.resolve(
            Response<dynamic>(
              requestOptions: options,
              statusCode: 200,
              data: options.path == '/api/auth/2fa/setup'
                  ? {
                      'secret': 'JBSWY3DPEHPK3PXP',
                      'otpauthUri': 'otpauth://totp/test',
                    }
                  : null,
            ),
          );
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: SecurityCheckpoint(
          api: api,
          user: {
            'name': 'Alex',
            'id': 7,
            'authorities': ['ROLE_MANAGER'],
            'requiresTwoFactor': false,
            'requiresTwoFactorSetup': true,
            'mustChangePassword': true,
          },
        ),
      ),
    );
    expect(find.text('Change your temporary password'), findsOneWidget);
    await tester.enterText(find.byType(TextField).at(0), 'OldSecret12345');
    await tester.enterText(find.byType(TextField).at(1), 'NewSecret12345');
    await tester.enterText(find.byType(TextField).at(2), 'NewSecret12345');
    await tester.tap(find.text('Change password'));
    await tester.pumpAndSettle();
    expect(requests, ['/api/auth/change-password', '/api/auth/2fa/setup']);
    expect(find.text('Set up two-step verification'), findsOneWidget);
    expect(find.text('JBSWY3DPEHPK3PXP'), findsOneWidget);
    await tester.enterText(find.byType(TextField), '123456');
    await tester.tap(find.text('Continue'));
    await tester.pumpAndSettle();
    expect(requests.last, '/api/auth/2fa/setup/confirm');
    expect(
      find.textContaining('A workspace for your role is not available yet.'),
      findsOneWidget,
    );
  });
}
