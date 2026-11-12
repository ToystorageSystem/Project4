import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:project4_app/warehouse/api.dart';
import 'package:project4_app/warehouse/auth.dart';
import 'package:project4_app/warehouse/driver.dart';

void main() {
  test('Driver role comes from server authorities, not displayed name', () {
    expect(
      isDriver({
        'authorities': ['ROLE_ROLE_DRIVER'],
      }),
      isTrue,
    );
    expect(
      isDriver({
        'authorities': ['ROLE_ROLE-DELIVERY-STAFF'],
      }),
      isTrue,
    );
    expect(
      isDriver({
        'authorities': ['ROLE_DRIVER'],
      }),
      isTrue,
    );
    expect(
      isDriver({
        'authorities': [
          {'authority': 'ROLE-DRIVER'},
        ],
      }),
      isTrue,
    );
    expect(
      isDriver({
        'authorities': ['ROLE_WAREHOUSE_STAFF'],
      }),
      isFalse,
    );
    expect(
      isDriver({
        'name': 'Driver',
        'authorities': ['ROLE_MANAGER'],
      }),
      isFalse,
    );
  });

  test('Trip actions match backend route, method and payload', () async {
    final api = WarehouseApi('http://localhost:8080');
    final requests = <RequestOptions>[];
    api.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          handler.resolve(
            Response<dynamic>(
              requestOptions: options,
              statusCode: 200,
              data: options.path == '/api/delivery-staff/trips'
                  ? <dynamic>[]
                  : <String, dynamic>{},
            ),
          );
        },
      ),
    );
    final service = DriverApi(api);
    await service.trips();
    await service.detail(42);
    await service.accept(42);
    await service.reject(42, 'Không thể nhận');
    await service.arrived(42);
    await service.scan(
      42,
      code: 'PK-001',
      condition: 'SEAL_BROKEN',
      seal: 'S-01',
      note: 'Rách seal',
    );
    await service.complete(42, 9, 'Received');
    await service.failed(42);
    await service.sendLocation(42, 10.77, 106.68);
    await service.latest(42);
    expect(requests.map((r) => '${r.method} ${r.path}'), [
      'GET /api/delivery-staff/trips',
      'GET /api/delivery-staff/trips/42',
      'PATCH /api/delivery-staff/trips/42/accept',
      'PATCH /api/delivery-staff/trips/42/reject',
      'PATCH /api/delivery-staff/trips/42/arrived',
      'POST /api/delivery-staff/trips/42/handover/packages/scan',
      'PATCH /api/delivery-staff/trips/42/handover/complete',
      'PATCH /api/delivery-staff/trips/42/failed',
      'POST /api/deliveries/42/tracking/location',
      'GET /api/deliveries/42/tracking/latest',
    ]);
    expect((requests[3].data as Map)['reason'], 'Không thể nhận');
    expect((requests[5].data as Map)['condition'], 'SEAL_BROKEN');
    expect((requests[6].data as Map)['receiverId'], 9);
  });
}
