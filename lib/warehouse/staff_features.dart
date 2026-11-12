import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import 'api.dart';
import 'main.dart';
import 'brand.dart';
import 'staff_lookup.dart';
import '../core/widgets/search_paged_list.dart';

const conditions = {
  'NORMAL': 'Normal',
  'DAMAGED': 'Damaged',
  'EXPIRED': 'Expired',
  'QUARANTINE': 'Quarantine required',
};
const damages = {
  'BROKEN': 'Broken',
  'DENTED': 'Dented',
  'SCRATCHED': 'Scratched',
  'WET': 'Wet',
  'EXPIRED': 'Expired',
  'MISSING_PARTS': 'Missing parts',
  'OTHER': 'Other',
};
const sources = {
  'MANUAL': 'Found in warehouse',
  'GOODS_RECEIPT': 'Receiving',
  'STOCK_TRANSFER': 'Picking / packing',
  'STOCK_COUNT': 'Stock count',
  'STORE_RETURN': 'Returns',
};
List<Json> maps(dynamic v) => (v is List ? v : <dynamic>[])
    .map((x) => Map<String, dynamic>.from(x as Map))
    .toList();

Future<void> printPackage(Json p) async {
  final doc = pw.Document();
  doc.addPage(
    pw.Page(
      pageFormat: PdfPageFormat(
        100 * PdfPageFormat.mm,
        80 * PdfPageFormat.mm,
        marginAll: 8 * PdfPageFormat.mm,
      ),
      build: (_) => pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.start,
        children: [
          pw.Text(
            'TOYSTORAGE',
            style: pw.TextStyle(fontSize: 16, fontWeight: pw.FontWeight.bold),
          ),
          pw.SizedBox(height: 8),
          pw.BarcodeWidget(
            barcode: pw.Barcode.qrCode(),
            data: '${p['packageCode']}',
            width: 90,
            height: 90,
          ),
          pw.SizedBox(height: 8),
          pw.Text('${p['packageCode']}'),
          pw.Text('SEAL: ${p['sealNumber'] ?? '-'}'),
          pw.Text('QTY: ${p['totalQuantity'] ?? 0}'),
        ],
      ),
    ),
  );
  await Printing.layoutPdf(
    onLayout: (_) => doc.save(),
    name: '${p['packageCode']}.pdf',
  );
}

Future<XFile?> pickEvidence(BuildContext context) async {
  final source = await showModalBottomSheet<ImageSource>(
    context: context,
    builder: (c) => SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: const Icon(Icons.camera_alt_outlined),
            title: const Text('Take evidence photo'),
            onTap: () => Navigator.pop(c, ImageSource.camera),
          ),
          ListTile(
            leading: const Icon(Icons.photo_library_outlined),
            title: const Text('Choose existing photo'),
            onTap: () => Navigator.pop(c, ImageSource.gallery),
          ),
        ],
      ),
    ),
  );
  if (source == null) return null;
  return ImagePicker().pickImage(
    source: source,
    maxWidth: 1600,
    imageQuality: 80,
    requestFullMetadata: false,
  );
}

Future<FormData> multipart(
  Json values,
  XFile? image, {
  bool jsonPart = false,
}) async {
  final data = FormData.fromMap(
    jsonPart
        ? {
            'request': MultipartFile.fromString(
              jsonEncode(values),
              contentType: MediaType('application', 'json'),
            ),
          }
        : values,
  );
  if (image != null) {
    final bytes = await image.readAsBytes();
    if (bytes.length > 10 * 1024 * 1024)
      throw Exception('Photo must be smaller than 10 MB.');
    final mime =
        image.mimeType ??
        (image.name.toLowerCase().endsWith('.png')
            ? 'image/png'
            : 'image/jpeg');
    data.files.add(
      MapEntry(
        'image',
        MultipartFile.fromBytes(
          bytes,
          filename: image.name,
          contentType: MediaType.parse(mime),
        ),
      ),
    );
  }
  return data;
}

class FeatureMenu extends StatelessWidget {
  final WarehouseApi api;
  final bool demo;
  const FeatureMenu({super.key, required this.api, required this.demo});
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      const Text(
        'Warehouse operations',
        style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800),
      ),
      const SizedBox(height: 8),
      const Text('Find stock, inspect returns and report issues.'),
      const SizedBox(height: 20),
      for (final entry in const [
        (
          0,
          'Receive returns',
          'Check packages, products and condition',
          Icons.assignment_return_outlined,
        ),
        (
          1,
          'Damaged goods report',
          'Record and track resolution',
          Icons.broken_image_outlined,
        ),
        (
          2,
          'Missing goods report',
          'Check received quantities',
          Icons.report_problem_outlined,
        ),
        (
          3,
          'Look up stock',
          'On hand, reserved and available',
          Icons.warehouse_outlined,
        ),
      ])
        Card(
          elevation: 0,
          color: Colors.white,
          child: ListTile(
            contentPadding: const EdgeInsets.all(16),
            leading: CircleAvatar(
              backgroundColor: brandSoft,
              child: Icon(entry.$4, color: teal),
            ),
            title: Text(
              entry.$2,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            subtitle: Text(entry.$3),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute<void>(
                builder: (_) =>
                    FeatureList(api: api, demo: demo, kind: entry.$1),
              ),
            ),
          ),
        ),
      const SizedBox(height: 16),
      OutlinedButton.icon(
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute<void>(builder: (_) => LocationLookupPage(api: api)),
        ),
        icon: const Icon(Icons.shelves),
        label: const Text('Find location trong kho'),
      ),
      const SizedBox(height: 20),
      const Text(
        'Staff permissions',
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
      const SizedBox(height: 8),
      const Text(
        'Record actual quantities. Receiving and counts require manager approval; completing a return updates inventory.',
      ),
    ],
  );
}

const featureTitles = [
  'Receive returns',
  'Damaged goods',
  'Missing goods',
  'Inventory',
];
const featurePaths = [
  '/api/warehouse/staff/store-returns',
  '/api/warehouse/staff/damaged-goods',
  '/api/warehouse/receiving-shortages/my-reports',
  '/api/warehouse/inventory/balances',
];
List<Json> sampleFeature(int kind) {
  if (kind == 0)
    return [
      {
        'returnId': 201,
        'returnCode': 'SR-2026-0008',
        'storeName': 'District 1 store',
        'status': 'SHIPPED',
        'items': [
          {
            'itemId': 1,
            'productId': 11,
            'productName': 'Creative building set',
            'barcode': '8938500010011',
            'expectedQuantity': 5,
            'receivedQuantity': null,
            'conditionStatus': null,
          },
        ],
      },
    ];
  if (kind == 3)
    return [
      {
        'productId': 11,
        'productCode': 'TOY-001',
        'productName': 'Creative building set',
        'barcode': '8938500010011',
        'onHand': 48,
        'reserved': 12,
        'available': 36,
        'locations': [
          {
            'locationId': 7,
            'locationCode': 'A-01-02',
            'locationName': 'Shelf A • Level 2',
            'onHand': 48,
            'reserved': 12,
            'available': 36,
          },
        ],
      },
    ];
  return [
    {
      'reportId': 301 + kind,
      'reportCode': kind == 1 ? 'DG-2026-0012' : 'SH-2026-0004',
      'status': 'OPEN',
      'description': kind == 1
          ? 'Wet packaging; inspect quality'
          : 'Two products missing from the receipt',
      'receiptCode': 'GR-2026-0042',
      'items': [
        {
          'productName': 'Creative building set',
          'quantity': 2,
          'locationCode': 'A-01-02',
        },
      ],
    },
  ];
}

class FeatureList extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  final int kind;
  const FeatureList({
    super.key,
    required this.api,
    required this.demo,
    required this.kind,
  });
  @override
  State<FeatureList> createState() => _FeatureListState();
}

class _FeatureListState extends State<FeatureList> {
  List<Json> rows = [];
  bool busy = true;
  String? error;
  String query = '', filter = 'All';
  int listPage = 0;
  final searchController = TextEditingController();
  @override
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      rows = widget.demo
          ? (rows.isEmpty ? sampleFeature(widget.kind) : rows)
          : maps(await widget.api.call('GET', featurePaths[widget.kind]));
      error = null;
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> create() async {
    final result = await Navigator.push<Json>(
      context,
      MaterialPageRoute(
        builder: (_) => ReportForm(
          api: widget.api,
          demo: widget.demo,
          shortage: widget.kind == 2,
        ),
      ),
    );
    if (result != null && mounted) {
      if (widget.demo) rows.insert(0, result);
      load();
    }
  }

  Future<void> open(Json row) async {
    if (widget.kind == 0) {
      await Navigator.push(
        context,
        MaterialPageRoute<void>(
          builder: (_) =>
              ReturnPage(api: widget.api, demo: widget.demo, initial: row),
        ),
      );
      if (mounted) load();
      return;
    }
    try {
      Json detail = row;
      if (widget.kind == 1 && !widget.demo)
        detail = Map<String, dynamic>.from(
          await widget.api.call('GET', '${featurePaths[1]}/${row['reportId']}'),
        );
      if (!mounted) return;
      await Navigator.push(
        context,
        MaterialPageRoute<void>(
          builder: (_) => RecordPage(
            api: widget.api,
            record: detail,
            inventory: widget.kind == 3,
          ),
        ),
      );
    } catch (e) {
      if (mounted) message(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final shown = rows
        .where(
          (r) =>
              matchesSearch(query, [r.toString()]) &&
              (filter == 'All' || r['status'] == filter),
        )
        .toList();
    final shownPages = shown.isEmpty ? 1 : (shown.length + 19) ~/ 20;
    final pageRows = shown
        .skip(listPage.clamp(0, shownPages - 1) * 20)
        .take(20)
        .toList();
    return Scaffold(
      appBar: AppBar(
        title: Text(featureTitles[widget.kind]),
        actions: [
          if (widget.kind == 0)
            IconButton(
              tooltip: 'Open task by ID',
              icon: const Icon(Icons.manage_search),
              onPressed: () async {
                final v = await fields(
                  context,
                  'Open return',
                  {'id': 'Return ID'},
                  numeric: {'id'},
                );
                if (v == null || !mounted) return;
                final id = int.parse(v['id']!);
                if (id <= 0) {
                  message(context, 'ID must be greater than zero.');
                  return;
                }
                await open({'returnId': id});
              },
            ),
          IconButton(
            onPressed: busy ? null : load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      floatingActionButton: widget.kind == 1 || widget.kind == 2
          ? FloatingActionButton.extended(
              onPressed: create,
              icon: const Icon(Icons.add),
              label: const Text('Create report'),
            )
          : null,
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          children: [
            if (widget.demo) const Chip(label: Text('DEMO • Sample data')),
            TextField(
              controller: searchController,
              onChanged: (v) => setState(() {
                query = v;
                listPage = 0;
              }),
              decoration: InputDecoration(
                hintText: 'Search by code or product name…',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.qr_code_scanner),
                  onPressed: () async {
                    final code = await Navigator.push<String>(
                      context,
                      MaterialPageRoute(builder: (_) => const ScanPage()),
                    );
                    if (code != null && mounted)
                      setState(() {
                        searchController.text = code;
                        query = code;
                        listPage = 0;
                      });
                  },
                ),
              ),
            ),
            const SizedBox(height: 14),
            if (widget.kind != 3)
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: {'All', ...rows.map((r) => '${r['status']}')}
                      .map(
                        (s) => Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: ChoiceChip(
                            label: Text(s == 'All' ? s : statusLabel(s)),
                            selected: filter == s,
                            onSelected: (_) => setState(() {
                              filter = s;
                              listPage = 0;
                            }),
                          ),
                        ),
                      )
                      .toList(),
                ),
              ),
            if (busy) const LinearProgressIndicator(),
            if (error != null)
              Padding(
                padding: const EdgeInsets.all(20),
                child: Text(error!, style: const TextStyle(color: Colors.red)),
              ),
            if (!busy && error == null && shown.isEmpty)
              const Padding(
                padding: EdgeInsets.all(40),
                child: Column(
                  children: [
                    Icon(Icons.inbox_outlined, size: 56, color: Colors.grey),
                    SizedBox(height: 14),
                    Text('No matching results.'),
                  ],
                ),
              ),
            for (final r in pageRows)
              Card(
                elevation: 0,
                color: Colors.white,
                margin: const EdgeInsets.symmetric(vertical: 7),
                child: ListTile(
                  contentPadding: const EdgeInsets.all(16),
                  title: Text(
                    '${r['returnCode'] ?? r['reportCode'] ?? r['productName']}',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: Text(
                    widget.kind == 3
                        ? 'On hand: ${r['onHand']} • Reserved: ${r['reserved']} • Available: ${r['available']}'
                        : '${r['storeName'] ?? r['description'] ?? ''}\n${statusLabel(r['status'])}',
                  ),
                  isThreeLine: widget.kind != 3,
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => open(r),
                ),
              ),
            ListPagerBar(
              page: listPage.clamp(0, shownPages - 1),
              pages: shownPages,
              total: shown.length,
              pageSize: 20,
              label: 'results',
              onPage: (p) => setState(() => listPage = p),
            ),
            const SizedBox(height: 90),
          ],
        ),
      ),
    );
  }
}

class RecordPage extends StatelessWidget {
  final WarehouseApi api;
  final Json record;
  final bool inventory;
  const RecordPage({
    super.key,
    required this.api,
    required this.record,
    this.inventory = false,
  });
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(
        '${record['reportCode'] ?? record['productCode'] ?? 'Details'}',
      ),
    ),
    body: ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Text(
          '${record['productName'] ?? record['description'] ?? ''}',
          style: const TextStyle(fontSize: 23, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),
        if (!inventory) Chip(label: Text(statusLabel(record['status']))),
        for (final key in [
          'receiptCode',
          'warehouseName',
          'reportedByName',
          'sourceType',
          'sourceId',
          'createdAt',
          'reviewedAt',
          'resolvedAt',
        ])
          if (record[key] != null)
            ListTile(
              title: Text(
                const {
                  'sourceType': 'Source of issue',
                  'sourceId': 'Source document',
                  'receiptCode': 'Receipt',
                  'warehouseName': 'Kho',
                  'reportedByName': 'Reported by',
                  'createdAt': 'Created',
                  'reviewedAt': 'Reviewed',
                  'resolvedAt': 'Resolved',
                }[key]!,
              ),
              subtitle: Text('${record[key]}'),
            ),
        for (final item in maps(record[inventory ? 'locations' : 'items']))
          Card(
            elevation: 0,
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${item['productName'] ?? item['locationCode'] ?? 'Products'}',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 10),
                  if (inventory)
                    Text(
                      'On hand: ${item['onHand']}\nReserved: ${item['reserved']}\nAvailable: ${item['available']}',
                      style: const TextStyle(height: 1.8),
                    )
                  else ...[
                    Text(
                      'Quantity: ${item['quantity'] ?? item['shortageQuantity'] ?? '—'}',
                    ),
                    if (item['expectedQuantity'] != null)
                      Text(
                        'Expected: ${item['expectedQuantity']} • Received: ${item['actualQuantity'] ?? '—'}',
                      ),
                    if (item['disposition'] != null)
                      Text('Disposition: ${item['disposition']}'),
                    if (item['status'] != null)
                      Text('Status: ${statusLabel(item['status'])}'),
                    if (item['locationCode'] != null)
                      Text('Location: ${item['locationCode']}'),
                    if (item['damageType'] != null)
                      Text(
                        'Condition: ${damages[item['damageType']] ?? item['damageType']}',
                      ),
                    if (item['conditionNote'] != null)
                      Text('${item['conditionNote']}'),
                    if (item['evidenceImageUrl'] != null)
                      EvidenceView(
                        api: api,
                        url: '${item['evidenceImageUrl']}',
                      ),
                  ],
                ],
              ),
            ),
          ),
      ],
    ),
  );
}

class EvidenceView extends StatelessWidget {
  final WarehouseApi api;
  final String url;
  const EvidenceView({super.key, required this.api, required this.url});
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 12),
    child: Image.network(
      Uri.parse(api.dio.options.baseUrl).resolve(url).toString(),
      height: 180,
      fit: BoxFit.contain,
      errorBuilder: (_, __, ___) =>
          const Text('Could not load the evidence photo.'),
    ),
  );
}

class ReportForm extends StatefulWidget {
  final WarehouseApi api;
  final bool demo, shortage;
  final Json? receipt, item;
  const ReportForm({
    super.key,
    required this.api,
    required this.demo,
    required this.shortage,
    this.receipt,
    this.item,
  });
  @override
  State<ReportForm> createState() => _ReportFormState();
}

class _ReportFormState extends State<ReportForm> {
  final form = GlobalKey<FormState>();
  final quantity = TextEditingController(),
      description = TextEditingController(),
      note = TextEditingController(),
      sourceId = TextEditingController();
  List<Json> documents = [], products = [], locations = [];
  Json? document, product, location;
  String damage = 'BROKEN', source = 'MANUAL';
  XFile? image;
  Uint8List? imageBytes;
  bool loading = true, sending = false;
  String? error;
  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    quantity.dispose();
    description.dispose();
    note.dispose();
    sourceId.dispose();
    super.dispose();
  }

  Future<void> load() async {
    try {
      if (widget.shortage) {
        documents = widget.demo
            ? demoRows(0)
            : maps(await widget.api.call('GET', modules[0].listPath));
        if (widget.receipt != null) {
          document = widget.receipt;
          if (!documents.any((r) => r['receiptId'] == document!['receiptId']))
            documents.add(document!);
          products = maps(document!['items']);
          product = widget.item;
        }
      } else {
        products = widget.demo
            ? sampleFeature(3)
            : maps(await widget.api.call('GET', featurePaths[3]));
      }
      error = null;
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> selectDocument(Json? value) async {
    if (value == null) return;
    setState(() {
      loading = true;
      document = value;
      product = null;
      products = [];
    });
    try {
      final detail = widget.demo
          ? value
          : Map<String, dynamic>.from(
              await widget.api.call(
                'GET',
                modules[0].detail(value['receiptId']),
              ),
            );
      document = detail;
      products = maps(detail['items']);
    } catch (e) {
      if (mounted) message(context, e);
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  int get missing =>
      ((product?['expectedQuantity'] as num? ?? 0) -
              (product?['actualQuantity'] as num? ?? 0))
          .toInt();
  Future<void> attach() async {
    try {
      final result = await pickEvidence(context);
      if (result == null) return;
      final bytes = await result.readAsBytes();
      if (bytes.length > 10 * 1024 * 1024)
        throw Exception('Photo must be smaller than 10 MB.');
      if (mounted)
        setState(() {
          image = result;
          imageBytes = bytes;
        });
    } catch (e) {
      if (mounted) message(context, e);
    }
  }

  Future<void> save() async {
    if (!form.currentState!.validate() || product == null) return;
    if (widget.shortage &&
        (product!['actualQuantity'] == null || missing <= 0)) {
      message(
        context,
        'Record the lower actual received quantity before reporting a shortage.',
      );
      return;
    }
    if (!widget.shortage &&
        (location == null ||
            int.parse(quantity.text) > (location!['onHand'] as num? ?? 0))) {
      message(context, 'Quantity exceeds stock at this location.');
      return;
    }
    setState(() => sending = true);
    try {
      final payload = widget.shortage
          ? <String, dynamic>{
              'productId': product!['productId'],
              'description': description.text.trim(),
            }
          : <String, dynamic>{
              'productId': product!['productId'],
              'locationId': location!['locationId'],
              'quantity': int.parse(quantity.text),
              'damageType': damage,
              'sourceType': source,
              'sourceId': source == 'MANUAL' ? null : int.parse(sourceId.text),
              'description': description.text.trim(),
              'conditionNote': note.text.trim(),
            };
      final Json result;
      if (widget.demo) {
        result = {
          'reportId': DateTime.now().millisecondsSinceEpoch,
          'reportCode':
              '${widget.shortage ? 'SH' : 'DG'}-DEMO-${DateTime.now().second}',
          'status': 'OPEN',
          'description': description.text,
          'items': [
            {
              ...product!,
              'quantity': widget.shortage ? missing : int.parse(quantity.text),
              'locationCode': location?['locationCode'],
              'damageType': damage,
            },
          ],
        };
      } else {
        final path = widget.shortage
            ? '/api/warehouse/receiving-shortages/goods-receipts/${document!['receiptId']}'
            : featurePaths[1];
        result = Map<String, dynamic>.from(
          await widget.api.call(
            'POST',
            path,
            await multipart(payload, image, jsonPart: !widget.shortage),
          ),
        );
      }
      if (mounted) Navigator.pop(context, result);
    } catch (e) {
      if (mounted) message(context, e);
    } finally {
      if (mounted) setState(() => sending = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(
        widget.shortage ? 'Missing goods report' : 'Damaged goods report',
      ),
    ),
    body: Form(
      key: form,
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const Text(
            'Record issue',
            style: TextStyle(fontSize: 27, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          const Text(
            'Check product and location before submitting the report.',
          ),
          const SizedBox(height: 24),
          if (loading) const LinearProgressIndicator(),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
          if (widget.shortage)
            DropdownButtonFormField<int>(
              initialValue: document?['receiptId'] as int?,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Related receipt'),
              items: documents
                  .map(
                    (r) => DropdownMenuItem(
                      value: r['receiptId'] as int,
                      child: Text('${r['receiptCode']}'),
                    ),
                  )
                  .toList(),
              onChanged: loading || sending
                  ? null
                  : (id) => selectDocument(
                      documents.firstWhere((r) => r['receiptId'] == id),
                    ),
              validator: (v) => v == null ? 'Choose receipt' : null,
            ),
          const SizedBox(height: 14),
          DropdownButtonFormField<int>(
            key: ValueKey(
              'product:${document?['receiptId'] ?? 'inventory'}:${product?['productId']}',
            ),
            initialValue: product?['productId'] as int?,
            isExpanded: true,
            decoration: const InputDecoration(labelText: 'Products'),
            items: products
                .map(
                  (r) => DropdownMenuItem(
                    value: r['productId'] as int,
                    child: Text(
                      '${r['productName']}',
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                )
                .toList(),
            onChanged: loading || sending
                ? null
                : (id) => setState(() {
                    product = products.firstWhere((r) => r['productId'] == id);
                    locations = maps(product!['locations']);
                    location = null;
                  }),
            validator: (v) => v == null ? 'Choose product' : null,
          ),
          const SizedBox(height: 14),
          if (widget.shortage && product != null)
            Card(
              color: const Color(0xFFFFEBC6),
              child: Padding(
                padding: const EdgeInsets.all(18),
                child: Text(
                  'Expected: ${product!['expectedQuantity']}\nReceived: ${product!['actualQuantity'] ?? 'Not inspected'}\nMissing: ${missing > 0 ? missing : 0}',
                  style: const TextStyle(height: 1.8),
                ),
              ),
            )
          else if (!widget.shortage) ...[
            DropdownButtonFormField<int>(
              key: ValueKey(
                'location:${product?['productId']}:${location?['locationId']}',
              ),
              initialValue: location?['locationId'] as int?,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Issue location'),
              items: locations
                  .map(
                    (r) => DropdownMenuItem(
                      value: r['locationId'] as int,
                      child: Text(
                        '${r['locationCode']} • On hand ${r['onHand']}',
                      ),
                    ),
                  )
                  .toList(),
              onChanged: sending
                  ? null
                  : (id) => setState(
                      () => location = locations.firstWhere(
                        (r) => r['locationId'] == id,
                      ),
                    ),
              validator: (v) => v == null ? 'Choose location' : null,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: quantity,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Affected quantity'),
              validator: (v) => (int.tryParse(v ?? '') ?? 0) <= 0
                  ? 'Enter a whole number greater than zero'
                  : null,
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: damage,
              decoration: const InputDecoration(labelText: 'Goods condition'),
              items: damages.entries
                  .map(
                    (e) => DropdownMenuItem(value: e.key, child: Text(e.value)),
                  )
                  .toList(),
              onChanged: (v) => setState(() => damage = v!),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: source,
              decoration: const InputDecoration(labelText: 'Detected during'),
              items: sources.entries
                  .map(
                    (e) => DropdownMenuItem(value: e.key, child: Text(e.value)),
                  )
                  .toList(),
              onChanged: (v) => setState(() => source = v!),
            ),
            if (source != 'MANUAL')
              Padding(
                padding: const EdgeInsets.only(top: 14),
                child: TextFormField(
                  controller: sourceId,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'Source document ID',
                  ),
                  validator: (v) => (int.tryParse(v ?? '') ?? 0) <= 0
                      ? 'Enter an ID greater than zero'
                      : null,
                ),
              ),
            const SizedBox(height: 14),
            TextFormField(
              controller: note,
              decoration: const InputDecoration(
                labelText: 'Condition notes (optional)',
              ),
            ),
          ],
          const SizedBox(height: 14),
          TextFormField(
            controller: description,
            maxLines: 3,
            maxLength: 2000,
            decoration: const InputDecoration(labelText: 'Description / cause'),
            validator: (v) =>
                (v ?? '').trim().isEmpty ? 'Enter a description' : null,
          ),
          const SizedBox(height: 14),
          if (imageBytes != null)
            Stack(
              children: [
                Image.memory(imageBytes!, height: 180),
                Positioned(
                  right: 0,
                  top: 0,
                  child: IconButton(
                    onPressed: () => setState(() {
                      image = null;
                      imageBytes = null;
                    }),
                    icon: const Icon(Icons.close),
                  ),
                ),
              ],
            ),
          OutlinedButton.icon(
            onPressed: sending ? null : attach,
            icon: const Icon(Icons.add_a_photo_outlined),
            label: const Text('Attach evidence photo'),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: loading || sending ? null : save,
            icon: const Icon(Icons.send_outlined),
            label: Text(sending ? 'Submitting…' : 'Submit report'),
          ),
        ],
      ),
    ),
  );
}

class ReturnPage extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  final Json initial;
  const ReturnPage({
    super.key,
    required this.api,
    required this.demo,
    required this.initial,
  });
  @override
  State<ReturnPage> createState() => _ReturnState();
}

class _ReturnState extends State<ReturnPage> {
  late Json data;
  bool busy = false;
  String? error;
  String get base => '${featurePaths[0]}/${data['returnId']}';
  List<Json> get items => maps(data['items']);
  @override
  void initState() {
    super.initState();
    data = widget.initial;
    if (!widget.demo) load();
  }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      data = Map<String, dynamic>.from(await widget.api.call('GET', base));
      error = null;
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> send(String method, String suffix, [dynamic body]) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      final r = await widget.api.call(method, '$base$suffix', body);
      if (r is Map && r.containsKey('returnId')) {
        data = Map<String, dynamic>.from(r);
      } else if (r is Map && r.containsKey('imageUrl')) {
        final itemId = int.parse(suffix.split('/')[2]);
        data = {
          ...data,
          'items': items
              .map(
                (item) => item['itemId'] == itemId
                    ? {...item, 'evidenceImageUrl': r['imageUrl']}
                    : item,
              )
              .toList(),
        };
      } else if (r is Map && r.containsKey('packageCode')) {
        if (mounted)
          await showDialog<void>(
            context: context,
            builder: (c) => AlertDialog(
              title: Text('Package ${r['packageCode']}'),
              content: Text(
                'Sealed: ${r['sealNumber'] ?? '—'}\nStatus: ${statusLabel(r['status'])}',
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(c),
                  child: const Text('Close'),
                ),
              ],
            ),
          );
      }
      error = null;
      if (mounted)
        message(
          context,
          suffix == '/submit' ? 'Return receiving completed.' : 'Result saved.',
        );
    } catch (e) {
      if (mounted) message(context, e);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> submitReturn() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Complete return receiving?'),
        content: const Text(
          'This action updates inventory immediately and marks the return Received. Normal goods become available; damaged, expired and quarantined goods go to quarantine. Confirm only after checking and sorting every item.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: const Text('Review'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('Confirm receipt'),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    if (widget.demo) {
      setState(() => data['status'] = 'RECEIVED');
    } else {
      await send('PATCH', '/submit');
    }
  }

  Future<void> inspect(Json item) async {
    final condition = await showModalBottomSheet<String>(
      context: context,
      builder: (c) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: conditions.entries
              .map(
                (e) => ListTile(
                  title: Text(e.value),
                  onTap: () => Navigator.pop(c, e.key),
                ),
              )
              .toList(),
        ),
      ),
    );
    if (condition == null || !mounted) return;
    final v = await fields(
      context,
      'Inspect returns',
      {
        'productBarcode': 'Product barcode',
        'receivedQuantity': 'Received quantity',
        'note': 'Notes',
      },
      numeric: {'receivedQuantity'},
      optional: {'note'},
      initial: {
        'productBarcode': '${item['barcode'] ?? ''}',
        'receivedQuantity': '${item['receivedQuantity'] ?? ''}',
        'note': '${item['note'] ?? ''}',
      },
    );
    if (v == null || !mounted) return;
    if (v['productBarcode'] != item['barcode']) {
      message(context, 'Barcode does not match the product.');
      return;
    }
    final qty = int.parse(v['receivedQuantity']!);
    if ((qty != item['expectedQuantity'] || condition != 'NORMAL') &&
        v['note']!.isEmpty) {
      message(context, 'Add a note for discrepancies or abnormal condition.');
      return;
    }
    final payload = <String, dynamic>{
      'productBarcode': v['productBarcode'],
      'receivedQuantity': qty,
      'conditionStatus': condition,
      'note': v['note'],
    };
    if (widget.demo) {
      setState(() {
        final target = (data['items'] as List).firstWhere(
          (r) => r['itemId'] == item['itemId'],
        );
        target.addAll(payload);
      });
    } else {
      await send('PUT', '/items', payload);
    }
  }

  Future<void> evidence(Json item) async {
    try {
      final image = await pickEvidence(context);
      if (image == null || !mounted) return;
      if (widget.demo) {
        message(context, 'Demo photo selected: ${image.name}');
        return;
      }
      await send(
        'POST',
        '/items/${item['itemId']}/evidence',
        await multipart({}, image),
      );
    } catch (e) {
      if (mounted) message(context, e);
    }
  }

  Future<void> scan(bool package) async {
    final value = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScanPage()),
    );
    if (value == null || !mounted) return;
    if (package) {
      if (widget.demo) {
        message(context, 'Demo package checked $value');
      } else {
        await send('POST', '/scan-package', {'packageBarcode': value});
      }
    } else {
      final found = items.where((r) => r['barcode'] == value).toList();
      if (found.isEmpty) {
        message(context, 'This product is not in the return.');
        return;
      }
      await inspect(found.first);
    }
  }

  @override
  Widget build(BuildContext context) {
    final editable = data['status'] == 'INSPECTING';
    final ready =
        items.isNotEmpty &&
        items.every(
          (r) => r['receivedQuantity'] != null && r['conditionStatus'] != null,
        );
    return Scaffold(
      appBar: AppBar(
        title: const Text('Inspect returned goods'),
        actions: [
          if (!widget.demo)
            IconButton(
              onPressed: busy ? null : load,
              icon: const Icon(Icons.refresh),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(
            '${data['returnCode']}',
            style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
          ),
          Text('${data['storeName'] ?? ''}'),
          const SizedBox(height: 8),
          Chip(label: Text(statusLabel(data['status']))),
          if (busy) const LinearProgressIndicator(),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
          if (data['status'] == 'SHIPPED')
            FilledButton(
              onPressed: busy
                  ? null
                  : () {
                      if (widget.demo) {
                        setState(() => data['status'] = 'INSPECTING');
                      } else {
                        send('PATCH', '/start');
                      }
                    },
              child: const Text('Accept return inspection'),
            ),
          if (editable)
            Wrap(
              spacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: busy ? null : () => scan(true),
                  icon: const Icon(Icons.qr_code),
                  label: const Text('Scan package'),
                ),
                OutlinedButton.icon(
                  onPressed: busy ? null : () => scan(false),
                  icon: const Icon(Icons.qr_code_scanner),
                  label: const Text('Scan product'),
                ),
              ],
            ),
          const SizedBox(height: 18),
          for (final r in items)
            Card(
              elevation: 0,
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${r['productName']}',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text('${r['barcode']}'),
                    const Divider(height: 24),
                    Text(
                      'Expected: ${r['expectedQuantity']} • Received: ${r['receivedQuantity'] ?? 'Not inspected'}',
                    ),
                    if (r['conditionStatus'] != null)
                      Text('Condition: ${conditions[r['conditionStatus']]}'),
                    if (r['note'] != null) Text('${r['note']}'),
                    if (r['evidenceImageUrl'] != null)
                      EvidenceView(
                        api: widget.api,
                        url: '${r['evidenceImageUrl']}',
                      ),
                    if (editable)
                      Wrap(
                        children: [
                          TextButton(
                            onPressed: busy ? null : () => inspect(r),
                            child: const Text('Save'),
                          ),
                          TextButton.icon(
                            onPressed: busy ? null : () => evidence(r),
                            icon: const Icon(Icons.photo_camera_outlined),
                            label: const Text('Evidence photo'),
                          ),
                        ],
                      ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 20),
          if (editable) ...[
            FilledButton(
              onPressed: busy || error != null || !ready ? null : submitReturn,
              child: const Text('Complete return receiving'),
            ),
            const Padding(
              padding: EdgeInsets.all(14),
              child: Text(
                'Completing this return updates stock immediately. The server checks that all products have been recorded.',
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class PackingBookmarks extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  const PackingBookmarks({super.key, required this.api, required this.demo});
  @override
  State<PackingBookmarks> createState() => _PackingBookmarksState();
}

class _PackingBookmarksState extends State<PackingBookmarks> {
  List<int> ids = [];
  List<Json> queue = [];
  bool busy = true;
  String? error;
  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    if (mounted) setState(() => busy = true);
    try {
      final values = await widget.api.packingIds();
      if (mounted)
        setState(() => ids = widget.demo && values.isEmpty ? [103] : values);
      final rows = widget.demo
          ? <Json>[]
          : maps(
              await widget.api.call(
                'GET',
                '/api/warehouse/staff/packing-submissions/transfers',
              ),
            );
      if (mounted)
        setState(() {
          queue = rows;
          error = null;
        });
    } catch (e) {
      if (mounted)
        setState(
          () => error =
              'Could not load the packing list from the server. ${e.toString().replaceFirst('Exception: ', '')}',
        );
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> open(int id) async {
    await Navigator.push(
      context,
      MaterialPageRoute<void>(
        builder: (_) =>
            PackingPage(api: widget.api, demo: widget.demo, transferId: id),
      ),
    );
    if (mounted) load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Pack and print package code'),
      actions: [
        IconButton(
          onPressed: busy ? null : load,
          icon: const Icon(Icons.refresh),
        ),
      ],
    ),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: () async {
        final v = await fields(
          context,
          'Open packing task',
          {'id': 'Transfer ID'},
          numeric: {'id'},
        );
        if (v == null || !mounted) return;
        final id = int.parse(v['id']!);
        if (id <= 0) {
          message(context, 'ID must be greater than zero.');
          return;
        }
        await open(id);
      },
      label: const Text('Open task'),
      icon: const Icon(Icons.add),
    ),
    body: RefreshIndicator(
      onRefresh: load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        children: [
          const Text(
            'Pending packing tasks',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          const Text(
            'Tasks from your warehouse. Open one to check quantities, create packages and seal them.',
          ),
          const SizedBox(height: 20),
          if (busy) const LinearProgressIndicator(),
          if (error != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Text(error!, style: const TextStyle(color: Colors.red)),
            ),
          for (final row in queue)
            Card(
              elevation: 0,
              color: Colors.white,
              child: ListTile(
                leading: const Icon(Icons.inventory_2_outlined, color: teal),
                title: Text('${row['transferCode'] ?? row['transferId']}'),
                subtitle: Text(
                  '${row['toWarehouseName'] ?? ''} • ${statusLabel(row['status'])}',
                ),
                onTap: busy ? null : () => open(row['transferId'] as int),
              ),
            ),
          if (!busy && error == null && queue.isEmpty)
            const Text('No packing tasks in this warehouse.'),
          const SizedBox(height: 24),
          const Text(
            'Recently opened on this device',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          if (!busy && ids.isEmpty)
            const Text('No task yet. Finish picking or open a task by ID.'),
          for (final id in ids)
            Card(
              elevation: 0,
              color: Colors.white,
              child: ListTile(
                leading: const Icon(Icons.inventory_2_outlined, color: teal),
                title: Text('Transfer #$id'),
                subtitle: const Text('Open to check progress'),
                onTap: () => open(id),
                trailing: IconButton(
                  tooltip: 'Remove from this device',
                  icon: const Icon(Icons.bookmark_remove_outlined),
                  onPressed: () async {
                    await widget.api.forgetPacking(id);
                    load();
                  },
                ),
              ),
            ),
          const SizedBox(height: 90),
        ],
      ),
    ),
  );
}
