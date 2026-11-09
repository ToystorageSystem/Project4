import '../features/fleet/fleet_screen.dart' show FleetScreen;

import 'dart:async';

import 'package:flutter/material.dart';

import 'api.dart';
import 'brand.dart';
import 'main.dart' show ScanPage;

/// Contract from backend branch: /api/delivery-staff/trips and tracking.
class DriverApi {
  final WarehouseApi api;
  DriverApi(this.api);
  Future<List<Json>> trips() async =>
      (await api.call('GET', '/api/delivery-staff/trips') as List)
          .map((e) => Json.from(e as Map))
          .toList();
  Future<Json> detail(int id) async =>
      Json.from(await api.call('GET', '/api/delivery-staff/trips/$id') as Map);
  Future<Json> accept(int id) async => Json.from(
    await api.call('PATCH', '/api/delivery-staff/trips/$id/accept') as Map,
  );
  Future<Json> reject(int id, String reason) async => Json.from(
    await api.call('PATCH', '/api/delivery-staff/trips/$id/reject', {
      'reason': reason,
    }) as Map,
  );
  Future<Json> arrived(int id) async => Json.from(
    await api.call('PATCH', '/api/delivery-staff/trips/$id/arrived') as Map,
  );
  Future<Json> failed(int id) async => Json.from(
    await api.call('PATCH', '/api/delivery-staff/trips/$id/failed') as Map,
  );
  Future<Json> scan(
    int id, {
    required String code,
    required String condition,
    String? seal,
    String? note,
  }) async => Json.from(
    await api.call(
      'POST',
      '/api/delivery-staff/trips/$id/handover/packages/scan',
      {
        'packageCode': code,
        'actualSealNumber': seal,
        'condition': condition,
        'issueNote': note,
      },
    ) as Map,
  );
  Future<Json> complete(int id, int receiverId, String note) async => Json.from(
    await api.call('PATCH', '/api/delivery-staff/trips/$id/handover/complete', {
      'receiverId': receiverId,
      'note': note,
    }) as Map,
  );
  Future<Json> latest(int id) async => Json.from(
    await api.call('GET', '/api/deliveries/$id/tracking/latest') as Map,
  );
  Future<Json> sendLocation(int id, double lat, double lon) async => Json.from(
    await api.call('POST', '/api/deliveries/$id/tracking/location', {
      'latitude': lat,
      'longitude': lon,
    }) as Map,
  );
}

String driverStatus(dynamic value) =>
    const {
      'ASSIGNED': 'Awaiting acceptance',
      'ACCEPTED': 'Accepted',
      'READY_TO_SHIP': 'Ready to ship',
      'IN_TRANSIT': 'In transit',
      'ARRIVED': 'Arrived',
      'DELIVERED': 'Delivered',
      'FAILED': 'Delivery failed',
      'REJECTED': 'Rejected',
    }[value] ??
    '${value ?? 'Unknown'}';

String driverError(Object e) => e.toString().replaceFirst('Exception: ', '');

class DriverHomePage extends StatefulWidget {
  final WarehouseApi api;
  final Json user;
  const DriverHomePage({super.key, required this.api, required this.user});
  @override
  State<DriverHomePage> createState() => _DriverHomeState();
}

class _DriverHomeState extends State<DriverHomePage> {
  Timer? _poll;
  bool _fetching = false;
  late final DriverApi service = DriverApi(widget.api);
  List<Json> rows = [];
  bool loading = true;
  String? error;
  String filter = 'In progress';
  @override
  void initState() {
    super.initState();
    refresh();
    _poll = Timer.periodic(const Duration(seconds: 10), (_) {
      if (mounted) refresh(quiet: true);
    });
  }

  @override
  void dispose() {
    _poll?.cancel();
    super.dispose();
  }

  Future<void> refresh({bool quiet = false}) async {
    if (_fetching) return;
    _fetching = true;
    if (mounted)
      setState(() {
        if (!quiet) loading = true;
        error = null;
      });
    try {
      final result = await service.trips();
      if (mounted) setState(() => rows = result);
    } catch (e) {
      if (mounted) setState(() => error = driverError(e));
    } finally {
      _fetching = false;
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> signOut() async {
    try {
      await widget.api.logout();
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted)
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(driverError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final shown = rows
        .where(
          (r) =>
              filter == 'All' ||
              (filter == 'Pending' && r['status'] == 'ASSIGNED') ||
              (filter == 'In progress' && r['status'] != 'ASSIGNED'),
        )
        .toList();
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            BrandLogo(size: 36),
            SizedBox(width: 10),
            Expanded(
              child: Text('Delivery trips', overflow: TextOverflow.ellipsis),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Vehicle & route',
            icon: const Icon(Icons.route_outlined),
            onPressed: () => Navigator.push<void>(
              context,
              MaterialPageRoute(builder: (_) => const FleetScreen()),
            ),
          ),
          IconButton(
            tooltip: 'Refresh',
            onPressed: loading ? null : refresh,
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Sign out',
            onPressed: signOut,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            Text(
              'Hello, ${widget.user['name'] ?? 'driver'}',
              style: Theme.of(context).textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 5),
            const Text(
              'Track the trips assigned to you.',
              style: TextStyle(color: muted),
            ),
            const SizedBox(height: 18),
            Wrap(
              spacing: 8,
              children: ['In progress', 'Pending', 'All']
                  .map(
                    (s) => ChoiceChip(
                      label: Text(s),
                      selected: filter == s,
                      onSelected: (_) => setState(() => filter = s),
                    ),
                  )
                  .toList(),
            ),
            const SizedBox(height: 12),
            if (loading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: CircularProgressIndicator(),
                ),
              ),
            if (error != null) _Notice(error!, retry: refresh),
            if (!loading && error == null && shown.isEmpty)
              const _Notice('No trips in this view.'),
            for (final row in shown)
              Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.all(16),
                  leading: const CircleAvatar(
                    backgroundColor: brandSoft,
                    child: Icon(Icons.local_shipping_outlined, color: brand),
                  ),
                  title: Text(
                    '${row['shipmentCode'] ?? '#${row['deliveryId']}'}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  subtitle: Text(
                    '${row['fromLocationName'] ?? 'Origin warehouse'} → '
                    '${row['toLocationName'] ?? 'Destination'}\n'
                    '${driverStatus(row['status'])} · ${row['packageCount'] ?? 0} packages',
                    style: const TextStyle(height: 1.6),
                  ),
                  isThreeLine: true,
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () async {
                    final id = (row['deliveryId'] as num?)?.toInt();
                    if (id == null) return;
                    await Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                        builder: (_) =>
                            DriverTripPage(service: service, id: id),
                      ),
                    );
                    if (mounted) refresh();
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class DriverTripPage extends StatefulWidget {
  final DriverApi service;
  final int id;
  const DriverTripPage({super.key, required this.service, required this.id});
  @override
  State<DriverTripPage> createState() => _DriverTripState();
}

class _DriverTripState extends State<DriverTripPage> {
  Json? trip, handover;
  String? error;
  bool busy = false;
  @override
  void initState() {
    super.initState();
    refresh();
  }

  Future<void> refresh() async {
    try {
      final value = await widget.service.detail(widget.id);
      if (mounted)
        setState(() {
          trip = value;
          error = null;
        });
    } catch (e) {
      if (mounted) setState(() => error = driverError(e));
    }
  }

  Future<bool> action(Future<dynamic> Function() operation) async {
    if (busy) return false;
    setState(() => busy = true);
    try {
      await operation();
      await refresh();
      if (mounted)
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Trip updated.')));
      return true;
    } catch (e) {
      if (mounted)
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(driverError(e))));
      await refresh(); // Avoid repeating a mutation if server processed it.
      return false;
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<bool> confirm(String title, String explanation) async =>
      await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(title),
          content: Text(explanation),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Confirm'),
            ),
          ],
        ),
      ) ??
      false;

  Future<void> reject() async {
    final reason = await _input(context, 'Reject trip', 'Rejection reason');
    if (reason == null) return;
    if (reason.length > 500) {
      _alert('The reason cannot exceed 500 characters.');
      return;
    }
    final ok = await action(() => widget.service.reject(widget.id, reason));
    if (mounted && ok) Navigator.pop(context);
  }

  void _alert(String msg) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));

  Future<void> scanPackage() async {
    final code = (await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScanPage()),
    ))?.trim();
    if (!mounted || code == null || code.isEmpty) return;
    final packages = (trip?['packages'] as List? ?? [])
        .whereType<Map>()
        .toList();
    final match = packages.where((p) => p['packageCode'] == code).toList();
    if (match.isEmpty) {
      _alert('This package is not part of the trip.');
      return;
    }
    final selected = await showDialog<_ScanDetails>(
      context: context,
      builder: (ctx) => _ScanDialog(
        code: code,
        expectedSeal: '${match.first['sealNumber'] ?? ''}',
      ),
    );
    if (selected == null) return;
    await action(() async {
      handover = await widget.service.scan(
        widget.id,
        code: code,
        condition: selected.condition,
        seal: selected.seal,
        note: selected.note,
      );
    });
  }

  Future<void> complete() async {
    final packages = (trip?['packages'] as List? ?? []);
    final receiver = await _input(
      context,
      'Hand over to recipient',
      'Destination recipient account ID',
      numbers: true,
    );
    final id = int.tryParse(receiver ?? '');
    if (receiver == null) return;
    if (id == null || id <= 0) {
      _alert('Invalid recipient ID.');
      return;
    }
    if (!await confirm(
      'Complete handover',
      'Confirm all packages were handed over to account #$id? The trip will be completed.',
    ))
      return;
    await action(() async {
      handover = await widget.service.complete(widget.id, id, '');
    });
  }

  Future<void> sendPosition() async {
    await Navigator.push<void>(
      context,
      MaterialPageRoute(builder: (_) => const FleetScreen()),
    );
  }

  Future<void> showLatest() async {
    try {
      final value = await widget.service.latest(widget.id);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Latest location'),
          content: Text(
            value['latitude'] == null || value['longitude'] == null
                ? 'No location has been sent.'
                : 'Latitude: ${value['latitude']}\nLongitude: ${value['longitude']}\n'
                      'Updated: ${value['lastReceivedAt'] ?? 'Unknown'}\n'
                      '${value['stale'] == true ? 'Outdated location' : 'Current location'}',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Close'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) _alert(driverError(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final data = trip, status = data?['status'];
    final packages = data?['packages'] as List? ?? [];
    final scanned = handover?['packages'] as List? ?? [];
    return Scaffold(
      appBar: AppBar(
        title: Text(
          data?['shipmentCode']?.toString() ?? 'Trip details #${widget.id}',
        ),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (error != null) _Notice(error!, retry: refresh),
            if (data == null && error == null)
              const Center(child: CircularProgressIndicator()),
            if (data != null) ...[
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        driverStatus(status),
                        style: const TextStyle(
                          color: brand,
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                        ),
                      ),
                      const SizedBox(height: 12),
                      _place(
                        'Pickup location',
                        data['fromLocationName'],
                        data['fromAddress'],
                        Icons.inventory_2_outlined,
                      ),
                      const Divider(height: 30),
                      _place(
                        'Delivery location',
                        data['toLocationName'],
                        data['toAddress'],
                        Icons.place_outlined,
                      ),
                      const SizedBox(height: 14),
                      Text(
                        'Expected delivery: ${data['expectedDeliveryAt'] ?? 'Not set'}',
                        style: const TextStyle(color: muted),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 14),
              Text(
                'Packages (${packages.length})',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              if (packages.isEmpty)
                const _Notice('No packages in the manifest.'),
              for (final p in packages.whereType<Map>())
                Card(
                  child: ListTile(
                    title: Text(
                      '${p['packageCode'] ?? ''}',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    subtitle: Text('Seal: ${p['sealNumber'] ?? 'Not set'}'),
                    trailing: Icon(
                      scanned.any(
                            (e) =>
                                e is Map &&
                                e['packageCode'] == p['packageCode'],
                          )
                          ? Icons.check_circle
                          : Icons.qr_code_scanner,
                      color: brand,
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              if (status == 'ASSIGNED') ...[
                _button(
                  'Accept trip',
                  Icons.check,
                  busy,
                  () => action(() => widget.service.accept(widget.id)),
                ),
                OutlinedButton.icon(
                  onPressed: busy ? null : reject,
                  icon: const Icon(Icons.close),
                  label: const Text('Reject'),
                ),
              ],
              if (status == 'ACCEPTED' || status == 'READY_TO_SHIP')
                const _Notice(
                  'Waiting for the warehouse to hand over and dispatch the shipment. '
                  'The trip will then move to In transit.',
                ),
              if (status == 'IN_TRANSIT') ...[
                _button(
                  'Confirm arrival',
                  Icons.pin_drop_outlined,
                  busy,
                  () async {
                    if (await confirm(
                      'Arrived',
                      'Have you arrived at the delivery location?',
                    )) {
                      await action(() => widget.service.arrived(widget.id));
                    }
                  },
                ),
                OutlinedButton.icon(
                  onPressed: busy ? null : sendPosition,
                  icon: const Icon(Icons.my_location),
                  label: const Text('Vehicle & location sharing'),
                ),
              ],
              if (status == 'ARRIVED') ...[
                const _Notice(
                  'Scan each package and verify the seal. '
                  'The recipient must belong to the destination location.',
                ),
                _button(
                  'Scan handover packages',
                  Icons.qr_code_scanner,
                  busy || packages.isEmpty,
                  scanPackage,
                ),
                _button(
                  'Complete handover',
                  Icons.inventory_outlined,
                  busy || packages.isEmpty,
                  complete,
                ),
              ],
              if (status == 'IN_TRANSIT' || status == 'ARRIVED')
                OutlinedButton.icon(
                  onPressed: busy
                      ? null
                      : () async {
                          if (await confirm(
                            'Report delivery failure',
                            'This trip will end as failed. Continue?',
                          )) {
                            await action(
                              () => widget.service.failed(widget.id),
                            );
                          }
                        },
                  icon: const Icon(Icons.report_problem_outlined),
                  label: const Text('Report delivery failure'),
                ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _place(String title, dynamic name, dynamic address, IconData icon) =>
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: brand),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: muted)),
                Text(
                  '${name ?? 'Unnamed location'}',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                Text('${address ?? 'No address'}'),
              ],
            ),
          ),
        ],
      );
}

Widget _button(String label, IconData icon, bool disabled, VoidCallback run) =>
    Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: SizedBox(
        width: double.infinity,
        child: FilledButton.icon(
          onPressed: disabled ? null : run,
          icon: Icon(icon),
          label: Text(label),
        ),
      ),
    );

class _Notice extends StatelessWidget {
  final String text;
  final VoidCallback? retry;
  const _Notice(this.text, {this.retry});
  @override
  Widget build(BuildContext context) => Card(
    color: brandSoft,
    child: Padding(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(text),
          if (retry != null)
            TextButton(onPressed: retry, child: const Text('Retry')),
        ],
      ),
    ),
  );
}

Future<String?> _input(
  BuildContext context,
  String title,
  String label, {
  bool numbers = false,
}) async {
  final controller = TextEditingController();
  try {
    return await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          autofocus: true,
          keyboardType: numbers ? TextInputType.number : TextInputType.text,
          maxLines: numbers ? 1 : 3,
          maxLength: numbers ? null : 500,
          decoration: InputDecoration(labelText: label),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              final s = controller.text.trim();
              if (s.isNotEmpty) Navigator.pop(ctx, s);
            },
            child: const Text('Continue'),
          ),
        ],
      ),
    );
  } finally {
    controller.dispose();
  }
}

class _ScanDetails {
  final String condition, seal, note;
  const _ScanDetails(this.condition, this.seal, this.note);
}

class _ScanDialog extends StatefulWidget {
  final String code, expectedSeal;
  const _ScanDialog({required this.code, required this.expectedSeal});
  @override
  State<_ScanDialog> createState() => _ScanDialogState();
}

class _ScanDialogState extends State<_ScanDialog> {
  final seal = TextEditingController(), note = TextEditingController();
  String condition = 'NORMAL';
  String? error;
  @override
  void initState() {
    super.initState();
    seal.text = widget.expectedSeal;
  }

  @override
  void dispose() {
    seal.dispose();
    note.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text('Package ${widget.code}'),
    content: SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Expected seal: ${widget.expectedSeal.isEmpty ? 'Not set' : widget.expectedSeal}',
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: condition,
            decoration: const InputDecoration(labelText: 'Package condition'),
            items: const [
              DropdownMenuItem(value: 'NORMAL', child: Text('Normal')),
              DropdownMenuItem(value: 'DAMAGED', child: Text('Damaged')),
              DropdownMenuItem(
                value: 'SEAL_MISMATCH',
                child: Text('Seal mismatch'),
              ),
              DropdownMenuItem(
                value: 'SEAL_BROKEN',
                child: Text('Broken seal'),
              ),
              DropdownMenuItem(
                value: 'MISSING',
                child: Text('Missing package'),
              ),
            ],
            onChanged: (v) => setState(() => condition = v ?? condition),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: seal,
            decoration: const InputDecoration(labelText: 'Actual seal number'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: note,
            maxLength: 500,
            decoration: const InputDecoration(labelText: 'Issue note'),
          ),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
        ],
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('Cancel'),
      ),
      FilledButton(
        onPressed: () {
          if (condition != 'NORMAL' && note.text.trim().isEmpty) {
            setState(
              () => error = 'An abnormal package requires an issue note.',
            );
            return;
          }
          Navigator.pop(
            context,
            _ScanDetails(condition, seal.text.trim(), note.text.trim()),
          );
        },
        child: const Text('Save'),
      ),
    ],
  );
}
