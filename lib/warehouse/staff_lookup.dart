import 'package:flutter/material.dart';

import 'api.dart';
import 'brand.dart';
import 'main.dart' show ScanPage;
import '../core/widgets/search_paged_list.dart';

Future<Json?> chooseItemLocation(BuildContext context, List<Json> items) async {
  if (items.length == 1) return items.first;
  return showModalBottomSheet<Json>(
    context: context,
    isScrollControlled: true,
    builder: (c) => SafeArea(
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(20),
              child: Text(
                'Choose a location to record',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
            ),
            for (final item in items)
              ListTile(
                title: Text('${item['productName']}'),
                subtitle: Text(
                  'Location: ${item['toLocationCode'] ?? item['locationCode'] ?? '—'}',
                ),
                onTap: () => Navigator.pop(c, item),
              ),
          ],
        ),
      ),
    ),
  );
}

/// Read-only location lookup. Creation/editing/status changes remain manager operations.
class LocationLookupPage extends StatefulWidget {
  final WarehouseApi api;
  const LocationLookupPage({super.key, required this.api});
  @override
  State<LocationLookupPage> createState() => _LocationLookupState();
}

class _LocationLookupState extends State<LocationLookupPage> {
  final search = TextEditingController();
  List<Json> rows = [];
  int listPage = 0;
  bool busy = true;
  String? error;
  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    search.dispose();
    super.dispose();
  }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      final data = await widget.api.call('GET', '/api/warehouse/locations');
      if (mounted)
        setState(() {
          rows = (data as List)
              .map((e) => Map<String, dynamic>.from(e as Map))
              .toList();
          error = null;
        });
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final shown = rows
        .where(
          (r) => matchesSearch(search.text, [r.toString()]),
        )
        .toList();
    final shownPages = shown.isEmpty ? 1 : (shown.length + 19) ~/ 20;
    final pageRows = shown
        .skip(listPage.clamp(0, shownPages - 1) * 20)
        .take(20)
        .toList();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Find location'),
        actions: [
          IconButton(
            onPressed: busy ? null : load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          children: [
            TextField(
              controller: search,
              onChanged: (_) => setState(() => listPage = 0),
              decoration: InputDecoration(
                hintText: 'Location code, area or shelf…',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  tooltip: 'Scan location code',
                  icon: const Icon(Icons.qr_code_scanner),
                  onPressed: () async {
                    final code = await Navigator.push<String>(
                      context,
                      MaterialPageRoute(builder: (_) => const ScanPage()),
                    );
                    if (code != null && mounted)
                      setState(() {
                        search.text = code;
                        listPage = 0;
                      });
                  },
                ),
              ),
            ),
            const SizedBox(height: 16),
            if (busy) const LinearProgressIndicator(),
            if (error != null)
              Text(error!, style: const TextStyle(color: Colors.red)),
            if (!busy && error == null && shown.isEmpty)
              const Padding(
                padding: EdgeInsets.all(24),
                child: Text('No matching location.'),
              ),
            for (final row in pageRows)
              Card(
                elevation: 0,
                color: Colors.white,
                child: ListTile(
                  leading: const Icon(Icons.shelves, color: brand),
                  title: Text(
                    '${row['warehouseCode'] ?? row['warehouseLocationsCode'] ?? row['name']}',
                  ),
                  subtitle: Text(
                    '${row['warehouseName'] ?? ''}\nArea: ${row['zone'] ?? '—'} • Shelf: ${row['shelf'] ?? '—'}\n${row['locationType'] ?? ''} • ${row['status'] ?? ''}',
                  ),
                  isThreeLine: true,
                ),
              ),
            ListPagerBar(
              page: listPage.clamp(0, shownPages - 1),
              pages: shownPages,
              total: shown.length,
              pageSize: 20,
              label: 'locations',
              onPage: (p) => setState(() => listPage = p),
            ),
          ],
        ),
      ),
    );
  }
}
