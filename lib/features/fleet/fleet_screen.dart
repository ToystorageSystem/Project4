import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/network/api_call.dart';
import 'gps_publisher.dart';

typedef Json = Map<String, dynamic>;
List<Json> rows(dynamic value) => (value is List ? value : const [])
    .whereType<Map>()
    .map((e) => Json.from(e))
    .toList();
String failure(Object error) => error is DioException
    ? apiExceptionFromDio(error).message
    : error.toString().replaceFirst('Exception: ', '');
String km(dynamic value) =>
    value is num ? '${value.toStringAsFixed(1)} km' : '—';

class FleetScreen extends ConsumerStatefulWidget {
  const FleetScreen({super.key});
  @override
  ConsumerState<FleetScreen> createState() => _FleetState();
}

class _FleetState extends ConsumerState<FleetScreen> {
  Json? board, route;
  String? _routeSignature;
  String signature(Json vehicle) => rows(vehicle['stops'])
      .map(
        (s) =>
            "${s['deliveryId']}:${s['status']}:${s['pickupOrder']}:${s['dropOrder']}",
      )
      .join("|");
  String? error;
  bool loading = true, busy = false, _fetching = false;
  Timer? timer;
  final map = MapController();
  @override
  void initState() {
    super.initState();
    unawaited(refresh());
    timer = Timer.periodic(
      const Duration(seconds: 5),
      (_) => unawaited(refresh(quiet: true)),
    );
  }

  @override
  void dispose() {
    timer?.cancel();
    map.dispose();
    super.dispose();
  }

  Future<void> refresh({bool quiet = false}) async {
    if (_fetching) return;
    _fetching = true;
    try {
      final value = await ref.read(fleetApiProvider).board();
      if (mounted)
        setState(() {
          board = value;
          if (!quiet) error = null;
        });
      final selectedId = route?['vehicleId'];
      if (selectedId != null && mounted) {
        final selected = rows(value['vehicles'])
            .where((v) => v['id'] == selectedId)
            .firstOrNull;
        if (selected == null) {
          setState(() => route = null);
        } else if (signature(selected) != _routeSignature) {
          final updated = await ref
              .read(fleetApiProvider)
              .route((selectedId as num).toInt());
          if (mounted && route?['vehicleId'] == selectedId)
            setState(() {
              route = updated;
              _routeSignature = signature(selected);
            });
        }
      }
    } catch (e) {
      if (mounted) setState(() => error = failure(e));
    } finally {
      _fetching = false;
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> run(Future<void> Function() action) async {
    if (busy) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await action();
      await refresh();
    } catch (e) {
      if (mounted) setState(() => error = failure(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> openRoute(Json vehicle) async {
    await run(() async {
      final value = await ref
          .read(fleetApiProvider)
          .route((vehicle['id'] as num).toInt());
      if (!mounted) return;
      setState(() {
        route = value;
        _routeSignature = signature(vehicle);
      });
      final points = rows(value['visits'])
          .map(
            (p) => LatLng(
              (p['latitude'] as num).toDouble(),
              (p['longitude'] as num).toDouble(),
            ),
          )
          .toList();
      if (vehicle['latitude'] is num && vehicle['longitude'] is num)
        points.add(
          LatLng(
            (vehicle['latitude'] as num).toDouble(),
            (vehicle['longitude'] as num).toDouble(),
          ),
        );
      if (points.length > 1)
        map.fitCamera(
          CameraFit.bounds(
            bounds: LatLngBounds.fromPoints(points),
            padding: const EdgeInsets.all(45),
          ),
        );
    });
  }

  Future<void> coordinates() async {
    final id = (board?['myWarehouseId'] as num?)?.toInt();
    if (id == null) return;
    final site = rows(board?['sites']).where((s) => s['id'] == id).firstOrNull;
    final lat = TextEditingController(
          text: site?['latitude']?.toString() ?? '',
        ),
        lon = TextEditingController(text: site?['longitude']?.toString() ?? '');
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Location coordinates'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: lat,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
                signed: true,
              ),
              decoration: const InputDecoration(labelText: 'Latitude'),
            ),
            TextField(
              controller: lon,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
                signed: true,
              ),
              decoration: const InputDecoration(labelText: 'Longitude'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    final a = double.tryParse(lat.text), b = double.tryParse(lon.text);
    lat.dispose();
    lon.dispose();
    if (ok == true && mounted) {
      if (a == null ||
          b == null ||
          !a.isFinite ||
          !b.isFinite ||
          a.abs() > 90 ||
          b.abs() > 180) {
        setState(() => error = 'Enter valid latitude and longitude.');
        return;
      }
      await run(() => ref.read(fleetApiProvider).coordinate(id, a, b));
    }
  }

  Future<void> register() async {
    final drivers = rows(board?['drivers']);
    if (drivers.isEmpty) {
      setState(
        () => error = 'No active drivers are assigned to your warehouse.',
      );
      return;
    }
    final plate = TextEditingController(), capacity = TextEditingController();
    int? driver;
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, update) => AlertDialog(
          title: const Text('Register vehicle'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: plate,
                  maxLength: 30,
                  decoration: const InputDecoration(labelText: 'License plate'),
                ),
                TextField(
                  controller: capacity,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'Capacity (packages)',
                  ),
                ),
                DropdownButtonFormField<int>(
                  initialValue: driver,
                  items: drivers
                      .map(
                        (d) => DropdownMenuItem(
                          value: (d['id'] as num).toInt(),
                          child: Text('${d['name']}'),
                        ),
                      )
                      .toList(),
                  onChanged: (v) => update(() => driver = v),
                  decoration: const InputDecoration(labelText: 'Driver'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Register'),
            ),
          ],
        ),
      ),
    );
    final amount = int.tryParse(capacity.text), number = plate.text.trim();
    plate.dispose();
    capacity.dispose();
    if (ok == true && mounted) {
      if (number.isEmpty ||
          driver == null ||
          amount == null ||
          amount < 1 ||
          amount > 10000) {
        setState(
          () => error = 'Enter a plate, driver and capacity between 1 and 10,000 packages.',
        );
        return;
      }
      await run(
        () => ref.read(fleetApiProvider).register(number, driver!, amount),
      );
    }
  }

  Future<void> recommend(Json manifest) async {
    await run(() async {
      final candidates = await ref
          .read(fleetApiProvider)
          .recommendations((manifest['id'] as num).toInt());
      if (!mounted) return;
      final choice = await showModalBottomSheet<Json>(
        context: context,
        isScrollControlled: true,
        builder: (context) => SafeArea(
          child: SizedBox(
            height: MediaQuery.sizeOf(context).height * .65,
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(20),
                  child: Text(
                    'Choose vehicle · ${manifest['manifestCode']}',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                if (candidates.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'No available vehicle has enough capacity. Ask drivers to mark their vehicles available.',
                    ),
                  ),
                Expanded(
                  child: ListView.builder(
                    itemCount: candidates.length,
                    itemBuilder: (context, i) {
                      final c = candidates[i];
                      return ListTile(
                        leading: Icon(
                          i == 0
                              ? Icons.recommend_outlined
                              : Icons.local_shipping_outlined,
                        ),
                        title: Text('${c['plate']} · ${c['driverName']}'),
                        subtitle: Text(
                          '${c['activeDeliveries']} active · peak ${c['peakLoad']}/${c['capacityPackages']} packages\n+${km(c['extraKm'])} · ${c['positionSource'] == 'LIVE_GPS' ? 'GPS' : 'Warehouse base'} · ${c['distanceModel'] == 'ROAD_DISTANCE' ? 'Road distance' : 'Estimated distance'}',
                        ),
                        isThreeLine: true,
                        onTap: () => Navigator.pop(context, c),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      );
      if (choice == null || !mounted) return;
      final confirm = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Assign delivery?'),
          content: Text(
            '${manifest['manifestCode']} → ${choice['plate']}\nThe driver must accept before warehouse handover.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Assign'),
            ),
          ],
        ),
      );
      if (confirm == true) {
        await ref
            .read(fleetApiProvider)
            .assign(
              (manifest['id'] as num).toInt(),
              (choice['vehicleId'] as num).toInt(),
            );
        if (mounted) setState(() => route = null);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final gps = ref.watch(gpsPublisherProvider);
    final vehicles = rows(board?['vehicles']);
    final visits = rows(route?['visits']);
    final points = vehicles
        .where((v) => v['latitude'] is num && v['longitude'] is num)
        .toList();
    final site = rows(board?['sites'])
        .where((s) => s['id'] == board?['myWarehouseId'])
        .firstOrNull;
    final center = points.isNotEmpty
        ? LatLng(
            (points.first['latitude'] as num).toDouble(),
            (points.first['longitude'] as num).toDouble(),
          )
        : site?['latitude'] is num
        ? LatLng(
            (site!['latitude'] as num).toDouble(),
            (site['longitude'] as num).toDouble(),
          )
        : const LatLng(10.7769, 106.7009);
    final geometry = (route?['roadGeometry'] as List? ?? [])
        .map((p) => LatLng((p[0] as num).toDouble(), (p[1] as num).toDouble()))
        .toList();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Vehicles & routes'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: busy ? null : () => refresh(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: refresh,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (error != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Text(
                        error!,
                        style: const TextStyle(color: Colors.red),
                      ),
                    ),
                  if (board?['isDriver'] == true)
                    AnimatedBuilder(
                      animation: gps,
                      builder: (context, _) => Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                vehicles.isEmpty
                                    ? 'No vehicle registered'
                                    : '${vehicles.first['plate']}',
                                style: Theme.of(context).textTheme.titleLarge,
                              ),
                              if (vehicles.isNotEmpty) ...[
                                SwitchListTile(
                                  contentPadding: EdgeInsets.zero,
                                  title: const Text(
                                    'Available for new pickups',
                                  ),
                                  value: vehicles.first['available'] == true,
                                  onChanged: busy
                                      ? null
                                      : (value) => run(
                                          () => ref
                                              .read(fleetApiProvider)
                                              .available(value),
                                        ),
                                ),
                                SwitchListTile(
                                  contentPadding: EdgeInsets.zero,
                                  title: const Text('Share vehicle location'),
                                  subtitle: Text(
                                    gps.sharing
                                        ? 'Location updates every 5 seconds'
                                        : 'Sharing is off',
                                  ),
                                  value: gps.sharing,
                                  onChanged: gps.working
                                      ? null
                                      : (value) =>
                                            value ? gps.start() : gps.stop(),
                                ),
                                const Text(
                                  'Sharing pauses when you leave the app. Reopen this screen and switch it on to resume.',
                                ),
                                if (gps.error != null)
                                  Text(
                                    gps.error!,
                                    style: const TextStyle(color: Colors.red),
                                  ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ),
                  SizedBox(
                    height: 310,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: FlutterMap(
                        mapController: map,
                        options: MapOptions(
                          initialCenter: center,
                          initialZoom: 12,
                        ),
                        children: [
                          TileLayer(
                            urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                            userAgentPackageName: 'com.toystorage.unified',
                          ),
                          if (geometry.length > 1)
                            PolylineLayer(
                              polylines: [
                                Polyline(
                                  points: geometry,
                                  strokeWidth: 4,
                                  color: Colors.deepOrange,
                                ),
                              ],
                            ),
                          MarkerLayer(
                            markers: [
                              for (final v in points)
                                Marker(
                                  point: LatLng(
                                    (v['latitude'] as num).toDouble(),
                                    (v['longitude'] as num).toDouble(),
                                  ),
                                  width: 46,
                                  height: 46,
                                  child: Tooltip(
                                    message:
                                        '${v['plate']} · ${v['stale'] == true ? 'Last known location' : 'Live'}',
                                    child: IconButton(
                                      onPressed: () => openRoute(v),
                                      icon: Icon(
                                        Icons.local_shipping,
                                        color: v['stale'] == true
                                            ? Colors.grey
                                            : Colors.deepOrange,
                                        size: 32,
                                      ),
                                    ),
                                  ),
                                ),
                              for (final v in visits)
                                Marker(
                                  point: LatLng(
                                    (v['latitude'] as num).toDouble(),
                                    (v['longitude'] as num).toDouble(),
                                  ),
                                  width: 36,
                                  height: 36,
                                  child: Tooltip(
                                    message: '${v['kind']} · ${v['label']}',
                                    child: const Icon(
                                      Icons.place,
                                      color: Colors.teal,
                                      size: 30,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                          RichAttributionWidget(
                            attributions: [
                              TextSourceAttribution(
                                'OpenStreetMap contributors',
                                onTap: () => launchUrl(
                                  Uri.parse(
                                    'https://www.openstreetmap.org/copyright',
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (board?['canDispatch'] == true ||
                      board?['myWarehouseId'] != null &&
                          board?['isDriver'] != true)
                    Wrap(
                      spacing: 10,
                      children: [
                        OutlinedButton.icon(
                          onPressed: busy ? null : coordinates,
                          icon: const Icon(Icons.pin_drop_outlined),
                          label: const Text('Set my location'),
                        ),
                        if (board?['canDispatch'] == true)
                          FilledButton.icon(
                            onPressed: busy ? null : register,
                            icon: const Icon(Icons.add),
                            label: const Text('Register vehicle'),
                          ),
                      ],
                    ),
                  const SizedBox(height: 16),
                  Text(
                    'Vehicles',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  if (vehicles.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(20),
                      child: Text('No vehicles are visible for this account.'),
                    ),
                  for (final v in vehicles)
                    Card(
                      child: ListTile(
                        leading: const Icon(Icons.local_shipping_outlined),
                        title: Text('${v['plate']} · ${v['driverName']}'),
                        subtitle: Text(
                          '${v['available'] == true ? 'Available for pickups' : 'Not accepting pickups'} · ${rows(v['stops']).length} deliveries\n${v['sharing'] == true ? (v['stale'] == true ? 'Last known / stale location' : 'Live location') : 'Location sharing off'}${v['receivedAt'] != null ? ' · ${v['receivedAt']}' : ''}',
                        ),
                        isThreeLine: true,
                        trailing: const Icon(Icons.route_outlined),
                        onTap: busy ? null : () => openRoute(v),
                      ),
                    ),
                  if (route != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      'Route · ${route!['plate']}',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    Text(
                      '${km(route!['distanceKm'])} · ${route!['distanceModel'] == 'ROAD_DISTANCE' ? 'Road distance' : 'Straight-line estimate; configure OSRM for road routes'}',
                    ),
                    for (int i = 0; i < visits.length; i++)
                      ListTile(
                        leading: CircleAvatar(child: Text('${i + 1}')),
                        title: Text(
                          '${visits[i]['kind']} · ${visits[i]['label']}',
                        ),
                        subtitle: Text('${visits[i]['packages']} packages'),
                      ),
                    if (visits.isNotEmpty)
                      OutlinedButton.icon(
                        onPressed: () async {
                          final p = visits.first;
                          final url = Uri.https(
                            'www.google.com',
                            '/maps/dir/',
                            {
                              'api': '1',
                              'destination':
                                  '${p['latitude']},${p['longitude']}',
                              'travelmode': 'driving',
                            },
                          );
                          if (!await launchUrl(
                                url,
                                mode: LaunchMode.externalApplication,
                              ) &&
                              mounted)
                            setState(
                              () => error = 'Could not open navigation.',
                            );
                        },
                        icon: const Icon(Icons.navigation_outlined),
                        label: const Text('Navigate to next stop'),
                      ),
                  ],
                  if (board?['canDispatch'] == true) ...[
                    const SizedBox(height: 22),
                    Text(
                      'Packed & checked · ready for dispatch',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    if (rows(board?['readyManifests']).isEmpty)
                      const Padding(
                        padding: EdgeInsets.all(16),
                        child: Text('No unassigned checked manifests.'),
                      ),
                    for (final m in rows(board?['readyManifests']))
                      Card(
                        child: ListTile(
                          title: Text(
                            '${m['manifestCode']} · ${m['packageCount']} packages',
                          ),
                          subtitle: Text('${m['fromName']} → ${m['toName']}'),
                          trailing: const Icon(Icons.auto_awesome_outlined),
                          onTap: busy ? null : () => recommend(m),
                        ),
                      ),
                  ],
                ],
              ),
            ),
    );
  }
}
