import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

import '../../core/network/dio_client.dart';
import '../auth/presentation/providers/auth_notifier.dart';
import 'fleet_api.dart';

final fleetApiProvider = Provider((ref) => FleetApi(ref.watch(dioProvider)));
final gpsPublisherProvider = Provider<GpsPublisher>((ref) {
  final publisher = GpsPublisher(ref.read(fleetApiProvider));
  ref.listen(authProvider, (_, next) {
    if (!next.isAuthenticated) unawaited(publisher.stop());
  });
  ref.onDispose(publisher.dispose);
  return publisher;
});

/// Explicit, foreground-only sharing. No tracking resumes silently after leaving the app.
class GpsPublisher extends ChangeNotifier with WidgetsBindingObserver {
  final FleetApi api;
  Timer? _timer;
  bool sharing = false, working = false, _sending = false, _disposed = false;
  int _generation = 0;
  String? error;
  DateTime? lastSentAt;
  GpsPublisher(this.api) {
    WidgetsBinding.instance.addObserver(this);
  }
  void changed() {
    if (!_disposed) notifyListeners();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached ||
        state == AppLifecycleState.hidden)
      unawaited(stop());
  }

  Future<Position> _position() => Geolocator.getCurrentPosition(
    locationSettings: const LocationSettings(
      accuracy: LocationAccuracy.high,
      timeLimit: Duration(seconds: 12),
    ),
  );
  Future<void> start() async {
    if (working || sharing || _disposed) return;
    final generation = ++_generation;
    working = true;
    error = null;
    changed();
    try {
      if (!await Geolocator.isLocationServiceEnabled())
        throw Exception('Turn on location services first.');
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied)
        permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever)
        throw Exception(
          'Location permission is required. Enable it in the device settings.',
        );
      final position = await _position();
      if (generation != _generation || _disposed) return;
      await api.sharing(true);
      if (generation != _generation || _disposed) {
        await api.sharing(false);
        return;
      }
      sharing = true;
      await _publish(position, generation);
      if (generation != _generation || _disposed) return;
      _timer = Timer.periodic(
        const Duration(seconds: 5),
        (_) => unawaited(_tick(generation)),
      );
    } catch (e) {
      error = e.toString().replaceFirst('Exception: ', '');
      sharing = false;
      try {
        await api.sharing(false);
      } catch (_) {}
    } finally {
      working = false;
      changed();
    }
  }

  Future<void> _publish(Position p, int generation) async {
    if (generation != _generation || !sharing || _disposed) return;
    if (!p.latitude.isFinite ||
        !p.longitude.isFinite ||
        !p.accuracy.isFinite ||
        p.accuracy > 1000)
      throw Exception('GPS accuracy is too low. Move to an open area.');
    await api.position(p.latitude, p.longitude, p.accuracy);
    if (generation != _generation || _disposed) return;
    lastSentAt = DateTime.now();
    error = null;
    changed();
  }

  Future<void> _tick(int generation) async {
    if (_sending || !sharing || _disposed) return;
    _sending = true;
    try {
      final position = await _position();
      await _publish(position, generation);
    } catch (e) {
      if (generation == _generation) {
        error = e.toString();
        changed();
      }
    } finally {
      _sending = false;
    }
  }

  Future<void> stop() async {
    final wasSharing = sharing || working;
    ++_generation;
    _timer?.cancel();
    _timer = null;
    sharing = false;
    changed();
    if (wasSharing) {
      try {
        await api.sharing(false);
      } catch (e) {
        error = 'Sharing stopped on this device. Server position will expire within 60 seconds.';
        changed();
      }
    }
  }

  @override
  void dispose() {
    _disposed = true;
    ++_generation;
    _timer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    if (sharing) unawaited(api.sharing(false).catchError((_) {}));
    super.dispose();
  }
}
