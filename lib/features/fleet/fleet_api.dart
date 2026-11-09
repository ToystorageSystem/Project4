import 'package:dio/dio.dart';

class FleetApi {
  final Dio dio;
  FleetApi(this.dio);
  Future<Map<String, dynamic>> board() async => Map<String, dynamic>.from(
    (await dio.get('/api/fleet/board')).data as Map,
  );
  Future<Map<String, dynamic>> route(int id) async => Map<String, dynamic>.from(
    (await dio.get('/api/fleet/vehicles/$id/route')).data as Map,
  );
  Future<List<Map<String, dynamic>>> recommendations(int id) async =>
      ((await dio.get('/api/fleet/recommendations/$id')).data as List)
          .map((v) => Map<String, dynamic>.from(v as Map))
          .toList();
  Future<void> assign(int manifestId, int vehicleId) async {
    await dio.post(
      '/api/fleet/manifests/$manifestId/assign',
      data: {'vehicleId': vehicleId},
    );
  }

  Future<void> register(String plate, int driverId, int capacity) async {
    await dio.post(
      '/api/fleet/vehicles',
      data: {
        'plate': plate,
        'driverId': driverId,
        'capacityPackages': capacity,
      },
    );
  }

  Future<void> coordinate(int id, double lat, double lon) async {
    await dio.put(
      '/api/fleet/sites/$id',
      data: {'latitude': lat, 'longitude': lon},
    );
  }

  Future<void> available(bool value) async {
    await dio.patch('/api/fleet/me/available', data: {'value': value});
  }

  Future<void> sharing(bool value) async {
    await dio.patch('/api/fleet/me/sharing', data: {'value': value});
  }

  Future<void> position(
    double latitude,
    double longitude,
    double accuracy,
  ) async {
    await dio.post(
      '/api/fleet/me/location',
      data: {
        'latitude': latitude,
        'longitude': longitude,
        'accuracyMeters': accuracy,
      },
    );
  }
}
