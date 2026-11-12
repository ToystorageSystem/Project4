import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import 'api.dart';
import 'staff_features.dart';
import 'brand.dart';
import 'auth.dart';
import 'workflow.dart';
import 'staff_lookup.dart';
import '../core/widgets/search_paged_list.dart';
export 'auth.dart' show LoginPage;

const teal = brand; // Compatibility alias for existing staff widgets.
void main() => runApp(const StaffApp());

class StaffApp extends StatelessWidget {
  const StaffApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Toy Storage System',
    theme: appTheme(),
    home: const LoginPage(),
  );
}

void message(BuildContext context, Object error) {
  final messenger = ScaffoldMessenger.of(context);
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(
    SnackBar(content: Text(error.toString().replaceFirst('Exception: ', ''))),
  );
}

final moduleIcons = [
  Icons.move_to_inbox_outlined,
  Icons.shelves,
  Icons.shopping_basket_outlined,
  Icons.fact_check_outlined,
];

class HomePage extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  final Json user;
  const HomePage({
    super.key,
    required this.api,
    required this.demo,
    required this.user,
  });
  @override
  State<HomePage> createState() => _HomeState();
}

class _HomeState extends State<HomePage> {
  List<Json> get _taskMatches => data[selected]
      .where((r) => matchesSearch(query, [r.toString()]))
      .toList();
  int get _taskPages =>
      _taskMatches.isEmpty ? 1 : (_taskMatches.length + 19) ~/ 20;
  int nav = 0, selected = 0;
  bool loading = true;
  String query = '';
  int taskPage = 0;
  List<List<Json>> data = List.generate(4, (_) => []);
  Map<int, String> errors = {};
  @override
  void initState() {
    super.initState();
    refresh();
  }

  Future<void> refresh() async {
    setState(() => loading = true);
    final next = List<List<Json>>.generate(4, (_) => []);
    final failures = <int, String>{};
    await Future.wait(
      List.generate(4, (i) async {
        try {
          next[i] = widget.demo
              ? (data[i].isEmpty ? demoRows(i) : data[i])
              : (await widget.api.call('GET', modules[i].listPath) as List)
                    .map((x) => Map<String, dynamic>.from(x as Map))
                    .toList();
        } catch (e) {
          failures[i] = e.toString();
        }
      }),
    );
    if (mounted)
      setState(() {
        data = next;
        errors = failures;
        loading = false;
      });
  }

  Future<void> open(int i, Json row, {bool autoStart = false}) async {
    await Navigator.push(
      context,
      MaterialPageRoute<void>(
        builder: (_) => TaskPage(
          api: widget.api,
          demo: widget.demo,
          module: i,
          initial: row,
          autoStart: autoStart,
        ),
      ),
    );
    if (mounted) refresh();
  }

  /// Supplier delivery: scan the purchase order (PO) code on the delivery note
  /// -> the matching goods receipt opens and inspection starts right away.
  Future<void> scanPurchaseOrder() async {
    final code = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScanPage()),
    );
    if (code == null || !mounted) return;
    Json? match(List<Json> rows) {
      final wanted = code.trim().toUpperCase();
      for (final r in rows) {
        final po = '${r['purchaseOrderCode'] ?? ''}'.trim().toUpperCase();
        final receipt = '${r['receiptCode'] ?? ''}'.trim().toUpperCase();
        if (wanted.isNotEmpty && (po == wanted || receipt == wanted)) return r;
      }
      return null;
    }

    var found = match(data[0]);
    if (found == null && !widget.demo) {
      await refresh();
      if (!mounted) return;
      found = match(data[0]);
    }
    if (found == null) {
      message(
        context,
        'No goods receipt is waiting for $code. Check the PO code or ask the manager.',
      );
      return;
    }
    await open(0, found, autoStart: true);
  }

  Widget tile(int i, Json row) => Card(
    margin: const EdgeInsets.only(bottom: 12),
    elevation: 0,
    color: Colors.white,
    child: ListTile(
      contentPadding: const EdgeInsets.all(16),
      leading: CircleAvatar(
        backgroundColor: brandSoft,
        child: Icon(moduleIcons[i], color: teal),
      ),
      title: Text(
        '${row[modules[i].codeKey]}',
        style: const TextStyle(fontWeight: FontWeight.w700),
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 7),
        child: Text(
          '${row['supplierName'] ?? row['toWarehouseName'] ?? row['warehouseName'] ?? modules[i].title}\n${statusLabel(row['status'])}',
          style: const TextStyle(height: 1.7),
        ),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => open(i, row),
    ),
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Row(
        children: [
          BrandLogo(size: 38),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'ToyStorage',
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: FontWeight.w800),
            ),
          ),
        ],
      ),
      actions: [
        IconButton(
          tooltip: 'Refresh',
          onPressed: loading ? null : refresh,
          icon: const Icon(Icons.refresh),
        ),
      ],
    ),
    bottomNavigationBar: NavigationBar(
      selectedIndex: nav,
      onDestinationSelected: (i) => setState(() => nav = i),
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.grid_view_rounded),
          label: 'Overview',
        ),
        NavigationDestination(
          icon: Icon(Icons.assignment_outlined),
          label: 'Tasks',
        ),
        NavigationDestination(
          icon: Icon(Icons.widgets_outlined),
          label: 'Operations',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          label: 'Account',
        ),
      ],
    ),
    body: SafeArea(
      child: RefreshIndicator(
        onRefresh: refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          children: [
            if (widget.demo)
              Container(
                padding: const EdgeInsets.all(10),
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFEBC6),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Text(
                  'DEMO • Sample data; no changes to live inventory.',
                ),
              ),
            if (nav == 0) ...[
              Text(
                'Hello, ${widget.user['name'] ?? 'there'}',
                style: const TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.w800,
                  color: ink,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'What needs your attention today?',
                style: TextStyle(color: muted),
              ),
              const SizedBox(height: 24),
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFF25D19), Color(0xFFD94F12)],
                  ),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'AVAILABLE TASKS',
                      style: TextStyle(
                        color: Colors.white70,
                        letterSpacing: 2,
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      loading
                          ? '…'
                          : errors.isNotEmpty
                          ? '—'
                          : '${data.expand((e) => e).length}',
                      style: const TextStyle(
                        fontSize: 48,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      errors.isNotEmpty
                          ? 'Some lists could not load. Pull down to retry.'
                          : 'Receiving • Putaway • Picking • Counts',
                      style: const TextStyle(
                        color: Colors.white70,
                        height: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 22),
              GridView.count(
                crossAxisCount: 2,
                childAspectRatio: 1.05,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                children: List.generate(
                  4,
                  (i) => Material(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(18),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(18),
                      onTap: () => setState(() {
                        selected = i;
                        nav = 1;
                      }),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Icon(moduleIcons[i], color: teal),
                            Text(
                              modules[i].title,
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            Text(
                              errors.containsKey(i)
                                  ? 'Could not load'
                                  : loading
                                  ? 'Loading…'
                                  : '${data[i].length} tasks',
                              style: const TextStyle(color: Colors.black54),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                ),
                onPressed: loading ? null : scanPurchaseOrder,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text('Scan PO to receive goods'),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) =>
                        PackingBookmarks(api: widget.api, demo: widget.demo),
                  ),
                ),
                icon: const Icon(Icons.inventory_2_outlined),
                label: const Text('Pack and print package code'),
              ),
              const SizedBox(height: 22),
              const Text(
                'Recent tasks',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 14),
              if (!loading &&
                  errors.isEmpty &&
                  data.every((list) => list.isEmpty))
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Text(
                    'No tasks assigned to you.',
                    style: TextStyle(color: muted),
                  ),
                ),
              for (int i = 0; i < 4; i++)
                ...data[i].take(2).map((r) => tile(i, r)),
            ],
            if (nav == 1) ...[
              const Text(
                'Warehouse tasks',
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 16),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: List.generate(
                    4,
                    (i) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(modules[i].title),
                        selected: selected == i,
                        onSelected: (_) => setState(() {
                          selected = i;
                          taskPage = 0;
                        }),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              if (selected == 0) ...[
                FilledButton.icon(
                  onPressed: loading ? null : scanPurchaseOrder,
                  icon: const Icon(Icons.qr_code_scanner),
                  label: const Text('Scan PO to receive goods'),
                ),
                const SizedBox(height: 12),
              ],
              TextField(
                onChanged: (v) => setState(() {
                  query = v;
                  taskPage = 0;
                }),
                decoration: const InputDecoration(
                  hintText: 'Search task code or supplier…',
                  prefixIcon: Icon(Icons.search),
                ),
              ),
              const SizedBox(height: 20),
              if (errors.containsKey(selected))
                Text(
                  errors[selected]!,
                  style: const TextStyle(color: Colors.red),
                ),
              if (!loading &&
                  !errors.containsKey(selected) &&
                  data[selected].isEmpty)
                const Padding(
                  padding: EdgeInsets.all(30),
                  child: Text('No tasks in this list.'),
                ),
              ..._taskMatches
                  .skip(taskPage.clamp(0, _taskPages - 1) * 20)
                  .take(20)
                  .map((r) => tile(selected, r)),
              ListPagerBar(
                page: taskPage.clamp(0, _taskPages - 1),
                pages: _taskPages,
                total: _taskMatches.length,
                pageSize: 20,
                label: 'tasks',
                onPage: (p) => setState(() => taskPage = p),
              ),
            ],
            if (nav == 2) FeatureMenu(api: widget.api, demo: widget.demo),
            if (nav == 3) ...[
              const Icon(Icons.account_circle_outlined, size: 80, color: teal),
              const SizedBox(height: 16),
              Center(
                child: Text(
                  '${widget.user['name'] ?? 'Staff member'}',
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Center(child: Text('${widget.user['email'] ?? 'Preview mode'}')),
              const SizedBox(height: 32),
              const ListTile(
                leading: Icon(Icons.shield_outlined),
                title: Text('Warehouse Staff'),
                subtitle: Text(
                  'Complete your tasks and submit them for manager approval.',
                ),
              ),
              const SizedBox(height: 24),
              OutlinedButton(
                onPressed: () async {
                  try {
                    if (!widget.demo) await widget.api.logout();
                  } catch (e) {
                    if (mounted) message(context, e);
                    return;
                  }
                  if (context.mounted) Navigator.pop(context);
                },
                child: const Text('Sign out'),
              ),
            ],
            if (loading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: CircularProgressIndicator(),
                ),
              ),
          ],
        ),
      ),
    ),
  );
  Future<void> packingLookup() async {
    final values = await fields(
      context,
      'Continue packing',
      {'transferId': 'Transfer ID (number)'},
      numeric: {'transferId'},
    );
    if (values == null || !mounted) return;
    await Navigator.push(
      context,
      MaterialPageRoute<void>(
        builder: (_) => PackingPage(
          api: widget.api,
          demo: widget.demo,
          transferId: int.parse(values['transferId']!),
        ),
      ),
    );
  }
}

List<Json> demoRows(int i) => [
  {
    modules[i].idKey: 101 + i,
    modules[i].codeKey: [
      'GR-2026-0042',
      'PA-2026-0018',
      'ST-2026-0021',
      'SC-2026-0009',
    ][i],
    'status': [
      'CONFIRMED',
      'AVAILABLE',
      'PENDING_SOURCE_CONFIRMATION',
      'PLANNED',
    ][i],
    'supplierName': 'Toy World supplier',
    'warehouseName': 'Central warehouse',
    'toWarehouseName': 'District 1 store',
    'items': <Json>[
      {
        'itemId': 1,
        'productId': 11,
        'productCode': 'TOY-001',
        'productName': 'Creative building set',
        'barcode': '8938500010011',
        'expectedQuantity': 24,
        'approvedQuantity': 24,
        'remainingQuantity': 24,
        'putawayQuantity': 0,
        'pickedQuantity': 0,
        'locationCode': 'A-01-02',
        'toLocationCode': 'B-02-01',
        'inspected': false,
      },
      {
        'itemId': 2,
        'productId': 12,
        'productCode': 'TOY-002',
        'productName': 'Sports toy car',
        'barcode': '8938500010028',
        'expectedQuantity': 12,
        'approvedQuantity': 12,
        'remainingQuantity': 12,
        'putawayQuantity': 0,
        'pickedQuantity': 0,
        'locationCode': 'A-01-03',
        'toLocationCode': 'B-02-02',
        'inspected': false,
      },
    ],
  },
];
Future<Map<String, String>?> fields(
  BuildContext context,
  String title,
  Map<String, String> labels, {
  Set<String> numeric = const {},
  Set<String> optional = const {},
  Map<String, String> initial = const {},
  String? helpText,
}) async {
  final controllers = {
    for (final k in labels.keys)
      k: TextEditingController(text: initial[k] ?? ''),
  };
  final key = GlobalKey<FormState>();
  final result = await showDialog<Map<String, String>>(
    context: context,
    builder: (c) => AlertDialog(
      title: Text(title),
      content: SingleChildScrollView(
        child: Form(
          key: key,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (helpText != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: Text(helpText, style: const TextStyle(height: 1.5)),
                ),
              ...labels.entries
                  .map(
                    (e) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: TextFormField(
                        controller: controllers[e.key],
                        keyboardType: numeric.contains(e.key)
                            ? TextInputType.number
                            : TextInputType.text,
                        decoration: InputDecoration(
                          labelText: e.value,
                          suffixIcon:
                              [
                                'productBarcode',
                                'locationCode',
                                'packageBarcode',
                              ].contains(e.key)
                              ? IconButton(
                                  icon: const Icon(Icons.qr_code_scanner),
                                  onPressed: () async {
                                    final scanned =
                                        await Navigator.push<String>(
                                          c,
                                          MaterialPageRoute(
                                            builder: (_) => const ScanPage(),
                                          ),
                                        );
                                    if (scanned != null && c.mounted)
                                      controllers[e.key]!.text = scanned;
                                  },
                                )
                              : null,
                        ),
                        validator: (v) {
                          if (optional.contains(e.key) &&
                              (v ?? '').trim().isEmpty)
                            return null;
                          if ((v ?? '').trim().isEmpty)
                            return 'Enter the required information';
                          if (numeric.contains(e.key) &&
                              (int.tryParse(v!) == null || int.parse(v) < 0))
                            return 'Enter a nonnegative whole number';
                          return null;
                        },
                      ),
                    ),
                  )
                  .toList(),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(c),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            if (key.currentState!.validate())
              Navigator.pop(c, {
                for (final e in controllers.entries) e.key: e.value.text.trim(),
              });
          },
          child: const Text('Confirm'),
        ),
      ],
    ),
  );
  // Dialog route can still animate out; defer controller disposal until then.
  Future.delayed(const Duration(seconds: 1), () {
    for (final c in controllers.values) c.dispose();
  });
  return result;
}

class TaskPage extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  final int module;
  final Json initial;

  /// Start the task as soon as it opens (scan PO -> inspect, inspect -> putaway).
  final bool autoStart;
  const TaskPage({
    super.key,
    required this.api,
    required this.demo,
    required this.module,
    required this.initial,
    this.autoStart = false,
  });
  @override
  State<TaskPage> createState() => _TaskState();
}

class _TaskState extends State<TaskPage> {
  late Json task;
  /// Picking: lines the staff skipped for now (e.g. shelf empty); shown again at the end.
  final Set<dynamic> skippedPicks = {};
  bool busy = false;
  String? error;
  Module get m => modules[widget.module];
  dynamic get id => task[m.idKey];
  List<Json> get items => (task['items'] as List? ?? [])
      .map((x) => Map<String, dynamic>.from(x as Map))
      .toList();
  @override
  void initState() {
    super.initState();
    task = widget.initial;
    if (!widget.demo) {
      load().then((_) {
        if (mounted && widget.autoStart && canStart && error == null) start();
      });
    } else if (widget.autoStart && canStart) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) start();
      });
    }
  }

  Future<void> start() => action(
    'PATCH',
    widget.module == 0 ? '/start-receiving' : '/start',
    demo: () => task['status'] = [
      'RECEIVING',
      'IN_PROGRESS',
      'PICKING',
      'COUNTING',
    ][widget.module],
  );

  /// Location the staff has to walk to for this line (putaway: assigned or suggested shelf).
  String? targetLocation(Json item) {
    final value = widget.module == 1
        ? (item['toLocationCode'] ?? item['suggestedLocationCode'])
        : item['locationCode'];
    final text = '${value ?? ''}'.trim();
    return text.isEmpty ? null : text;
  }

  String targetPlace(Json item) {
    final zone = widget.module == 1
        ? (item['toLocationCode'] != null ? item['zone'] : item['suggestedZone'])
        : item['zone'];
    final shelf = widget.module == 1
        ? (item['toLocationCode'] != null ? item['shelf'] : item['suggestedShelf'])
        : item['shelf'];
    return [
      if ('${zone ?? ''}'.isNotEmpty) 'Zone $zone',
      if ('${shelf ?? ''}'.isNotEmpty) 'Shelf $shelf',
    ].join(' · ');
  }

  /// Lines in walking order: grouped by location so the staff does not go back and forth.
  List<Json> get routeItems {
    final list = items;
    if (widget.module == 0) {
      list.sort((a, b) =>
          (a['inspected'] == true ? 1 : 0) - (b['inspected'] == true ? 1 : 0));
    }
    if (widget.module == 1 || widget.module == 2) {
      list.sort((a, b) {
        final doneA = (a['remainingQuantity'] as num? ?? 0) <= 0 ? 1 : 0;
        final doneB = (b['remainingQuantity'] as num? ?? 0) <= 0 ? 1 : 0;
        if (doneA != doneB) return doneA - doneB;
        return (targetLocation(a) ?? '~').compareTo(targetLocation(b) ?? '~');
      });
    }
    return list;
  }

  Json? get nextStop {
    if (widget.module != 1 && widget.module != 2) return null;
    for (final item in routeItems) {
      if ((item['remainingQuantity'] as num? ?? 0) > 0) return item;
    }
    return null;
  }

  /// After inspection the same staff puts the goods on the shelf: open that putaway task.
  Future<void> continueToPutaway() async {
    if (widget.demo) return;
    try {
      final tasks = (await widget.api.call('GET', modules[1].listPath) as List)
          .map((x) => Map<String, dynamic>.from(x as Map))
          .toList();
      final next = tasks
          .where((t) => t['goodsReceiptId'] == id)
          .firstOrNull;
      if (!mounted) return;
      if (next == null) {
        message(
          context,
          'Inspection finished. No goods to put away (all damaged or missing).',
        );
        return;
      }
      message(context, 'Inspection finished. Now put the goods on the shelves.');
      await Navigator.pushReplacement(
        context,
        MaterialPageRoute<void>(
          builder: (_) => TaskPage(
            api: widget.api,
            demo: widget.demo,
            module: 1,
            initial: next,
            autoStart: true,
          ),
        ),
      );
    } catch (e) {
      if (mounted) message(context, e);
    }
  }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      final r = await widget.api.call('GET', m.detail(id));
      if (mounted)
        setState(() {
          task = Map<String, dynamic>.from(r);
          error = null;
        });
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> action(
    String method,
    String suffix, {
    Json? body,
    VoidCallback? demo,
  }) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      if (widget.demo) {
        demo?.call();
      } else {
        final response = await widget.api.call(
          method,
          '${m.base}/$id$suffix',
          body,
        );
        task = applyTaskResponse(widget.module, task, response);
      }
      if (widget.module == 2 && task['status'] == 'PACKING') {
        try {
          await widget.api.rememberPacking(id as int);
        } catch (_) {}
      }
      if (mounted) {
        setState(() => error = null);
        message(context, 'Task updated.');
      }
    } catch (e) {
      if (mounted) {
        setState(() => error = e.toString());
        message(context, e);
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  bool get active => [
    ['RECEIVING'],
    ['IN_PROGRESS'],
    ['PICKING'],
    ['IN_PROGRESS', 'COUNTING', 'RECOUNTING'],
  ][widget.module].contains(task['status']);
  bool get canStart => [
    ['CONFIRMED', 'RECEIVING'],
    ['AVAILABLE'],
    // 2026-11-13: Business submit -> CREATED; the warehouse can start picking it
    ['CREATED', 'PENDING_SOURCE_CONFIRMATION'],
    ['PLANNED'],
  ][widget.module].contains(task['status']);
  bool get done => taskComplete(widget.module, task);
  Future<void> editItem(Json item, {String? scannedBarcode}) async {
    if (widget.module == 0) {
      final v = await fields(
        context,
        'Inspect ${item['productCode'] ?? ''}',
        {
          'actualQuantity': 'Received quantity',
          'damagedQuantity': 'Damaged',
          'packageCode': 'Package code (optional)',
          'notes': 'Notes (optional)',
        },
        numeric: {'actualQuantity', 'damagedQuantity'},
        optional: {'packageCode', 'notes'},
        initial: {
          'actualQuantity':
              '${item['actualQuantity'] ?? item['expectedQuantity'] ?? ''}',
          'damagedQuantity': '${item['damagedQuantity'] ?? 0}',
          'notes': '${item['notes'] ?? ''}',
          'packageCode': '${item['packageCode'] ?? ''}',
        },
      );
      if (v == null || !mounted) return;
      final actual = int.parse(v['actualQuantity']!),
          damaged = int.parse(v['damagedQuantity']!);
      if (damaged > actual) {
        message(context, 'Damaged quantity cannot exceed received quantity.');
        return;
      }
      if ((actual != item['expectedQuantity'] || damaged > 0) &&
          v['notes']!.isEmpty) {
        message(context, 'Add notes for shortages, overages or damage.');
        return;
      }
      final payload = <String, dynamic>{
        'productId': item['productId'],
        'actualQuantity': actual,
        'damagedQuantity': damaged,
        'packageCode': v['packageCode'],
        'notes': v['notes'],
      };
      await action(
        'PUT',
        '/inspections',
        body: {
          'items': [payload],
        },
        demo: () {
          final target = (task['items'] as List).firstWhere(
            (x) => x['productId'] == item['productId'],
          );
          target.addAll({...payload, 'inspected': true});
        },
      );
    } else {
      final v = await fields(
        context,
        widget.module == 3 ? 'Record count' : 'Scan and confirm',
        {
          'productBarcode': 'Product barcode',
          'locationCode': 'Location code',
          'quantity': widget.module == 3
              ? 'Counted quantity'
              : 'Processed quantity',
        },
        numeric: {'quantity'},
        initial: {
          if (scannedBarcode != null) 'productBarcode': scannedBarcode,
          if (widget.module != 3 && targetLocation(item) != null)
            'locationCode': targetLocation(item)!,
          if (widget.module != 3)
            'quantity': '${item['remainingQuantity'] ?? ''}',
        },
        helpText: widget.module == 1
            ? item['toLocationCode'] == null
                  ? item['suggestedLocationCode'] != null
                        ? 'Go to ${item['suggestedLocationCode']}${targetPlace(item).isEmpty ? '' : ' (${targetPlace(item)})'} — ${item['suggestedReason'] ?? 'suggested shelf'}. If it is full, scan another NORMAL shelf.'
                        : 'Scan an active NORMAL shelf in this warehouse. The first entry assigns it as the destination shelf.'
                  : 'Continue putaway on shelf ${item['toLocationCode']}. Do not change the shelf after recording stock.'
            : null,
      );
      if (v == null || !mounted) return;
      final qty = int.parse(v['quantity']!);
      if (widget.module != 3 && qty < 1) {
        message(context, 'Quantity must be greater than zero.');
        return;
      }
      final scanError = validateScannedItem(
        widget.module,
        item,
        v['productBarcode']!,
        v['locationCode']!,
        qty,
      );
      if (scanError != null) {
        message(context, scanError);
        return;
      }
      final payload = <String, dynamic>{
        'productBarcode': v['productBarcode'],
        'locationCode': v['locationCode'],
        'quantity': qty,
      };
      await action(
        widget.module == 3 ? 'PUT' : 'PATCH',
        widget.module == 3 ? '/items' : '/items/${item['itemId']}',
        body: payload,
        demo: () {
          final target = (task['items'] as List).firstWhere(
            (x) => x['itemId'] == item['itemId'],
          );
          if (widget.module == 3) {
            target[countField(task)] = qty;
          } else {
            if (widget.module == 1)
              target['toLocationCode'] = v['locationCode'];
            target['remainingQuantity'] -= qty;
            target[widget.module == 1 ? 'putawayQuantity' : 'pickedQuantity'] +=
                qty;
          }
        },
      );
    }
  }

  Future<void> scan() async {
    final code = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScanPage()),
    );
    if (code == null || !mounted) return;
    final found = items
        .where((x) => x['barcode'] == code || x['productCode'] == code)
        .toList();
    if (found.isEmpty) {
      message(context, 'Code $code is not in this task.');
      return;
    }
    final selected = await chooseItemLocation(context, found);
    if (selected != null && mounted) {
      await editItem(
        selected,
        scannedBarcode: code == selected['barcode'] ? code : null,
      );
    }
  }

  // =====================================================================
  // 2026-11-13 simplified flow: ONE "Scan" button per task.
  //   Receiving: scan product -> received / damaged / notes
  //   Picking:   scan shelf -> scan product -> quantity (default 1)
  //   Putaway:   scan product -> scan shelf -> quantity (default 1)
  // Quantities are only shown after the right code was scanned.
  // Stock count keeps the previous flow (scan()).
  // =====================================================================

  Future<String?> scanCode(String prompt) => Navigator.push<String>(
    context,
    MaterialPageRoute(builder: (_) => ScanPage(prompt: prompt)),
  );

  static String _norm(dynamic value) => '${value ?? ''}'.trim().toUpperCase();

  static bool _sameProduct(Json item, String code) =>
      _norm(item['barcode']) == _norm(code) ||
      _norm(item['productCode']) == _norm(code);

  static int _remaining(Json item) =>
      (item['remainingQuantity'] as num? ?? 0).toInt();

  String _productName(Json item) =>
      '${item['productName'] ?? item['productCode'] ?? 'Product'}';

  Future<void> scanFlow() async {
    switch (widget.module) {
      case 0:
        return receiveFlow();
      case 1:
        return putawayFlow();
      case 2:
        return pickFlow();
      default:
        return scan();
    }
  }

  Future<void> receiveFlow() async {
    final code = await scanCode('Scan the product barcode');
    if (code == null || !mounted) return;
    final item = items.where((x) => _sameProduct(x, code)).firstOrNull;
    if (item == null) {
      message(context, 'Code $code is not on this receipt.');
      return;
    }
    final v = await askInspection(context, item);
    if (v == null || !mounted) return;
    final payload = <String, dynamic>{
      'productId': item['productId'],
      'actualQuantity': v.actual,
      'damagedQuantity': v.damaged,
      'packageCode': item['packageCode'],
      'notes': v.notes,
    };
    await action(
      'PUT',
      '/inspections',
      body: {
        'items': [payload],
      },
      demo: () {
        final target = (task['items'] as List).firstWhere(
          (x) => x['productId'] == item['productId'],
        );
        target.addAll({...payload, 'inspected': true});
      },
    );
  }

  /// Picking goes one line at a time in walking order. Skipped lines come back last.
  Json? get pickCurrent {
    final open = routeItems.where((x) => _remaining(x) > 0).toList();
    if (open.isEmpty) return null;
    return open.where((x) => !skippedPicks.contains(x['itemId'])).firstOrNull ??
        open.first;
  }

  int get pickOpenCount => items.where((x) => _remaining(x) > 0).length;

  void skipPick() {
    final current = pickCurrent;
    if (current == null) return;
    setState(() {
      if (skippedPicks.length >= pickOpenCount - 1) skippedPicks.clear();
      skippedPicks.add(current['itemId']);
    });
  }

  Future<void> pickFlow() async {
    final item = pickCurrent;
    if (item == null) return;
    final code = await scanCode('Scan the barcode of ${_productName(item)}');
    if (code == null || !mounted) return;
    if (!_sameProduct(item, code)) {
      message(
        context,
        'This is not ${_productName(item)}. Check shelf ${item['locationCode']} and scan again.',
      );
      return;
    }
    final qty = await askQuantity(
      context,
      title: 'Pick ${_productName(item)}',
      info: ['Shelf: ${item['locationCode']}', 'To pick: ${_remaining(item)}'],
      max: _remaining(item),
      initial: _remaining(item),
    );
    if (qty == null || !mounted) return;
    await submitLine(item, '${item['locationCode']}', qty);
    if (mounted) setState(() => skippedPicks.remove(item['itemId']));
  }

  Widget pickStep() {
    final item = pickCurrent;
    final total = items.length;
    if (item == null) {
      return const Card(
        elevation: 0,
        color: Color(0xFFE8F5E9),
        child: Padding(
          padding: EdgeInsets.all(20),
          child: Row(
            children: [
              Icon(Icons.check_circle, color: Colors.green, size: 32),
              SizedBox(width: 12),
              Expanded(
                child: Text(
                  'All products picked. Tap Finish picking.',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
        ),
      );
    }
    final doneCount = total - pickOpenCount;
    final place = targetPlace(item);
    return Card(
      elevation: 0,
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Product ${doneCount + 1} of $total',
              style: const TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                const Icon(Icons.location_on_outlined, color: teal),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    '${item['locationCode'] ?? 'No location'}${place.isEmpty ? '' : ' · $place'}',
                    style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: teal,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              _productName(item),
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            Text(
              '${item['productCode'] ?? ''} • ${item['barcode'] ?? ''}',
              style: const TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 12),
            Text(
              'To pick: ${_remaining(item)}'
              '${(item['pickedQuantity'] as num? ?? 0) > 0 ? '  (already picked ${item['pickedQuantity']})' : ''}',
              style: const TextStyle(fontSize: 17),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(56),
                textStyle: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              onPressed: busy || error != null ? null : pickFlow,
              icon: const Icon(Icons.qr_code_scanner),
              label: const Text('Scan product'),
            ),
            if (pickOpenCount > 1)
              TextButton(
                onPressed: busy ? null : skipPick,
                child: const Text('Skip for now'),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> putawayFlow() async {
    final code = await scanCode('Scan the product barcode');
    if (code == null || !mounted) return;
    final candidates = items
        .where((x) => _sameProduct(x, code) && _remaining(x) > 0)
        .toList();
    if (candidates.isEmpty) {
      message(context, 'Code $code has nothing left to put away in this task.');
      return;
    }
    final item = candidates.length == 1
        ? candidates.first
        : await chooseItemLocation(context, candidates);
    if (item == null || !mounted) return;
    final assigned = '${item['toLocationCode'] ?? ''}'.trim();
    final suggested = '${item['suggestedLocationCode'] ?? ''}'.trim();
    final shelf = await scanCode(
      assigned.isNotEmpty
          ? 'Scan shelf $assigned'
          : suggested.isNotEmpty
          ? 'Scan the shelf (suggested: $suggested)'
          : 'Scan a storage shelf',
    );
    if (shelf == null || !mounted) return;
    if (assigned.isNotEmpty && _norm(shelf) != _norm(assigned)) {
      message(context, 'This product goes to shelf $assigned. Scan that shelf.');
      return;
    }
    final qty = await askQuantity(
      context,
      title: 'Put away ${_productName(item)}',
      info: [
        'Shelf: ${assigned.isNotEmpty ? assigned : shelf.trim().toUpperCase()}',
        'Still to put away: ${_remaining(item)}',
      ],
      max: _remaining(item),
    );
    if (qty == null || !mounted) return;
    await submitLine(
      item,
      assigned.isNotEmpty ? assigned : shelf.trim().toUpperCase(),
      qty,
    );
  }

  /// Picking / putaway: one scanned quantity is added to the line on the server.
  Future<void> submitLine(Json item, String location, int qty) async {
    final payload = <String, dynamic>{
      'productBarcode': item['barcode'],
      'locationCode': location,
      'quantity': qty,
    };
    await action(
      'PATCH',
      '/items/${item['itemId']}',
      body: payload,
      demo: () {
        final target = (task['items'] as List).firstWhere(
          (x) => x['itemId'] == item['itemId'],
        );
        if (widget.module == 1) target['toLocationCode'] = location;
        target['remainingQuantity'] -= qty;
        target[widget.module == 1 ? 'putawayQuantity' : 'pickedQuantity'] += qty;
      },
    );
  }

  /// Receiving finished: put away now (same staff) or later from the Putaway list.
  Future<void> askPutawayNow() async {
    final now = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Put the goods on the shelves now?'),
        content: const Text(
          'The inspection is finished. Good items are in the receiving area. '
          'You can put them away now, or later from Putaway on the home screen.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: const Text('Later'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('Put away now'),
          ),
        ],
      ),
    );
    if (!mounted) return;
    if (now == true) {
      await continueToPutaway();
    } else {
      message(context, 'Inspection finished. The putaway task is waiting in Putaway.');
    }
  }

  Future<void> complete() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(
          const [
            'Finish inspection?',
            'Finish putaway?',
            'Finish picking?',
            'Submit stock count?',
          ][widget.module],
        ),
        content: Text(
          const [
            'Good items stay in the receiving area until they are put away. '
                'Damaged items go to the quarantine area. Shortages, overages and damage are reported automatically.',
            'The goods on the shelves become available stock.',
            'The transfer moves to packing.',
            'The counted quantities are sent to the manager for review.',
          ][widget.module],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: const Text('Review'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('Complete'),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final before = task['status'];
    await action(
      'PATCH',
      [
        '/finish-inspection',
        '/complete',
        '/complete',
        '/submit',
      ][widget.module],
      demo: () => task['status'] = [
        'INSPECTED',
        'COMPLETED',
        'PACKING',
        'PENDING_CONFIRMATION',
      ][widget.module],
    );
    if (widget.module == 0 && mounted && error == null && task['status'] != before) {
      await askPutawayNow();
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(m.title),
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
          '${task[m.codeKey]}',
          style: const TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w800,
            color: ink,
          ),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          children: [
            Chip(
              label: Text(statusLabel(task['status'])),
              backgroundColor: brandSoft,
            ),
            if (widget.demo) const Chip(label: Text('DEMO')),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          '${task['supplierName'] ?? task['toWarehouseName'] ?? task['warehouseName'] ?? ''}',
        ),
        if (task['purchaseOrderCode'] != null)
          Text('Purchase order: ${task['purchaseOrderCode']}'),
        if (task['scheduledDate'] != null)
          Text('Count scheduled: ${task['scheduledDate']}'),
        if (task['assignedToName'] != null)
          Text('Assigned to: ${task['assignedToName']}'),
        const SizedBox(height: 22),
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red)),
        if (busy) const LinearProgressIndicator(),
        if (canStart)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: FilledButton.icon(
              onPressed: busy ? null : start,
              icon: const Icon(Icons.play_arrow),
              label: const Text('Accept and start'),
            ),
          ),
        if (active && widget.module == 2) ...[pickStep(), const SizedBox(height: 18)],
        if (active && widget.module != 2)
          FilledButton.icon(
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            onPressed: busy || error != null ? null : scanFlow,
            icon: const Icon(Icons.qr_code_scanner),
            label: Text(
              const [
                'Scan product',
                'Scan product',
                'Scan product',
                'Scan product code',
              ][widget.module],
            ),
          ),
        const SizedBox(height: 18),
        if (active && widget.module == 1 && nextStop != null) _NextStopCard(
          title: 'Put away next',
          location: targetLocation(nextStop!) ??
              (widget.module == 1 ? 'Scan any NORMAL shelf' : 'No location'),
          place: targetPlace(nextStop!),
          reason: widget.module == 1 && nextStop!['toLocationCode'] == null
              ? nextStop!['suggestedReason']?.toString()
              : null,
          product:
              '${nextStop!['productName'] ?? nextStop!['productCode'] ?? ''}',
          quantity: '${nextStop!['remainingQuantity'] ?? ''}',
          onConfirm: null, // info only: the Scan button does the work
        ),
        if (!(active && widget.module == 2)) ...[
        Text(
          '${items.length} products',
          style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 12),
        ],
        // Picking in progress: only the current step is shown (no full list).
        for (final item in active && widget.module == 2 ? const <Json>[] : routeItems)
          Card(
            elevation: 0,
            color: Colors.white,
            margin: const EdgeInsets.only(bottom: 14),
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          '${item['productName'] ?? item['productCode'] ?? 'Products'}',
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      if (itemComplete(widget.module, task, item))
                        const Icon(Icons.check_circle, color: Colors.green),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${item['productCode'] ?? ''} • ${item['barcode'] ?? ''}',
                    style: const TextStyle(color: Colors.black54),
                  ),
                  const Divider(height: 28),
                  if (widget.module == 0)
                    Text(
                      'Expected: ${item['expectedQuantity'] ?? '—'}   •   Received: ${item['actualQuantity'] ?? '—'}\nDamaged: ${item['damagedQuantity'] ?? 0}   •   ${item['inspected'] == true ? 'Inspected' : 'Not inspected'}',
                      style: const TextStyle(height: 1.8),
                    ),
                  if (widget.module == 1 || widget.module == 2) ...[
                    Row(
                      children: [
                        const Icon(
                          Icons.location_on_outlined,
                          size: 18,
                          color: teal,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            widget.module == 1
                                ? item['toLocationCode'] != null
                                      ? '${item['toLocationCode']}'
                                      : item['suggestedLocationCode'] != null
                                      ? 'Suggested: ${item['suggestedLocationCode']}${targetPlace(item).isEmpty ? '' : ' · ${targetPlace(item)}'}'
                                      : 'No shelf assigned — scan during putaway'
                                : '${item['locationCode'] ?? 'No location'}${targetPlace(item).isEmpty ? '' : ' · ${targetPlace(item)}'}',
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              color: teal,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'Remaining: ${item['remainingQuantity'] ?? '—'}  •  ${widget.module == 1 ? 'Put away' : 'Picked'}: ${item[widget.module == 1 ? 'putawayQuantity' : 'pickedQuantity'] ?? 0}',
                    ),
                  ],
                  if (widget.module == 3)
                    Text(
                      'Location: ${item['locationCode'] ?? '—'}\nFirst count: ${item['firstCountQuantity'] ?? 'Not recorded'}${item['secondCountQuantity'] != null ? '\nRecount: ${item['secondCountQuantity']}' : ''}${item['finalQuantity'] != null ? '\nResult: ${item['finalQuantity']}' : ''}',
                      style: const TextStyle(height: 1.8),
                    ),
                  if (active && widget.module == 3)
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton.icon(
                        onPressed: busy || error != null
                            ? null
                            : () => editItem(item),
                        icon: const Icon(Icons.edit_outlined, size: 18),
                        label: const Text('Record count'),
                      ),
                    ),
                ],
              ),
            ),
          ),
        if (active)
          FilledButton(
            onPressed: busy || error != null || !done ? null : complete,
            child: Text(
              widget.module == 3
                  ? 'Submit stock count'
                  : widget.module == 2
                  ? 'Finish picking'
                  : 'Complete ${m.title.toLowerCase()}',
            ),
          ),
        if (active && !done && widget.module != 2)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: Text(
              'Scan every product to finish. If stock is missing or the shelf is wrong, ask a manager.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
          ),
        if (widget.module == 2 && task['status'] == 'PACKING')
          FilledButton.icon(
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute<void>(
                builder: (_) => PackingPage(
                  api: widget.api,
                  demo: widget.demo,
                  transferId: id,
                ),
              ),
            ),
            icon: const Icon(Icons.inventory_2_outlined),
            label: const Text('Move to packing'),
          ),
      ],
    ),
  );
}

/// Quantity after a successful scan. Starts at [initial] (default 1), can go down to 0,
/// never above [max].
/// 0 cannot be confirmed (nothing to record) — the user cancels instead.
Future<int?> askQuantity(
  BuildContext context, {
  required String title,
  required List<String> info,
  required int max,
  int initial = 1,
}) {
  final start = initial > max ? max : (initial < 0 ? 0 : initial);
  final ctrl = TextEditingController(text: '$start');
  return showDialog<int>(
    context: context,
    builder: (c) => StatefulBuilder(
      builder: (c, setLocal) {
        final value = int.tryParse(ctrl.text.trim()) ?? 0;
        final valid = value >= 1 && value <= max;
        return AlertDialog(
          title: Text(title),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final line in info) Text(line),
              const SizedBox(height: 16),
              _QtyStepper(
                controller: ctrl,
                min: 0,
                max: max,
                onChanged: () => setLocal(() {}),
              ),
              if (!valid)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    value > max ? 'Maximum is $max.' : 'Enter at least 1.',
                    style: TextStyle(color: Theme.of(c).colorScheme.error),
                  ),
                ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(c),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: valid ? () => Navigator.pop(c, value) : null,
              child: const Text('Confirm'),
            ),
          ],
        );
      },
    ),
  );
}

class InspectionValue {
  final int actual;
  final int damaged;
  final String notes;
  const InspectionValue(this.actual, this.damaged, this.notes);
}

/// Receiving: shown only after the product barcode matched a receipt line.
/// Saving again overwrites the previous values for this product.
Future<InspectionValue?> askInspection(BuildContext context, Json item) {
  final expected = (item['expectedQuantity'] as num? ?? 0).toInt();
  final inspected = item['inspected'] == true;
  final actualCtrl = TextEditingController(
    text: inspected ? '${item['actualQuantity'] ?? 1}' : '1',
  );
  final damagedCtrl = TextEditingController(
    text: inspected ? '${item['damagedQuantity'] ?? 0}' : '0',
  );
  final notesCtrl = TextEditingController(
    text: inspected ? '${item['notes'] ?? ''}' : '',
  );
  return showDialog<InspectionValue>(
    context: context,
    builder: (c) => StatefulBuilder(
      builder: (c, setLocal) {
        final actual = int.tryParse(actualCtrl.text.trim()) ?? -1;
        final damaged = int.tryParse(damagedCtrl.text.trim()) ?? -1;
        final needsNotes = actual != expected || damaged > 0;
        String? problem;
        if (actual < 0 || damaged < 0) {
          problem = 'Enter a number.';
        } else if (damaged > actual) {
          problem = 'Damaged cannot be more than received.';
        } else if (needsNotes && notesCtrl.text.trim().isEmpty) {
          problem = 'Add a note: quantity differs or goods are damaged.';
        }
        return AlertDialog(
          title: Text('${item['productName'] ?? item['productCode'] ?? 'Product'}'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Expected: $expected',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 14),
                const Text('Received'),
                _QtyStepper(
                  controller: actualCtrl,
                  min: 0,
                  onChanged: () => setLocal(() {}),
                ),
                const SizedBox(height: 12),
                const Text('Damaged (included in received)'),
                _QtyStepper(
                  controller: damagedCtrl,
                  min: 0,
                  onChanged: () => setLocal(() {}),
                ),
                if (needsNotes) ...[
                  const SizedBox(height: 12),
                  TextField(
                    controller: notesCtrl,
                    onChanged: (_) => setLocal(() {}),
                    decoration: const InputDecoration(
                      labelText: 'Note (required)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
                if (problem != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      problem,
                      style: TextStyle(color: Theme.of(c).colorScheme.error),
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(c),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: problem == null
                  ? () => Navigator.pop(
                      c,
                      InspectionValue(
                        actual,
                        damaged,
                        needsNotes ? notesCtrl.text.trim() : '',
                      ),
                    )
                  : null,
              child: const Text('Save'),
            ),
          ],
        );
      },
    ),
  );
}

/// [-]  [ 12 ]  [+]
class _QtyStepper extends StatelessWidget {
  final TextEditingController controller;
  final int min;
  final int? max;
  final VoidCallback onChanged;
  const _QtyStepper({
    required this.controller,
    required this.min,
    this.max,
    required this.onChanged,
  });

  void _set(int value) {
    var v = value < min ? min : value;
    if (max != null && v > max!) v = max!;
    controller.text = '$v';
    onChanged();
  }

  @override
  Widget build(BuildContext context) {
    final value = int.tryParse(controller.text.trim()) ?? 0;
    return Row(
      children: [
        IconButton.filledTonal(
          onPressed: value > min ? () => _set(value - 1) : null,
          icon: const Icon(Icons.remove),
        ),
        Expanded(
          child: TextField(
            controller: controller,
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w700),
            onChanged: (_) => onChanged(),
          ),
        ),
        IconButton.filledTonal(
          onPressed: max == null || value < max! ? () => _set(value + 1) : null,
          icon: const Icon(Icons.add),
        ),
      ],
    );
  }
}

class ScanPage extends StatefulWidget {
  /// What to scan, e.g. "Scan the shelf code" (shown under the camera).
  final String? prompt;
  const ScanPage({super.key, this.prompt});
  @override
  State<ScanPage> createState() => _ScanState();
}

class _ScanState extends State<ScanPage> {
  final controller = MobileScannerController();
  bool returned = false;
  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  void finish(String value) {
    if (returned || !mounted) return;
    returned = true;
    Navigator.pop(context, value);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Scan barcode / QR'),
      actions: [
        IconButton(
          onPressed: () async {
            try {
              await controller.toggleTorch();
            } catch (e) {
              if (context.mounted)
                message(context, 'This device does not support a flash.');
            }
          },
          icon: const Icon(Icons.flash_on),
        ),
      ],
    ),
    body: Column(
      children: [
        Expanded(
          child: MobileScanner(
            controller: controller,
            onDetect: (capture) {
              for (final barcode in capture.barcodes) {
                final value = barcode.rawValue;
                if (value != null) {
                  finish(value);
                  break;
                }
              }
            },
            errorBuilder: (context, error) => const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'Could not open the camera. Check camera permission or enter the code manually.',
                ),
              ),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: Text(
            widget.prompt ?? 'Place the barcode or QR code in the camera frame.',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: OutlinedButton.icon(
            onPressed: () async {
              try {
                await controller.stop();
              } catch (_) {}
              if (!context.mounted) return;
              final v = await fields(context, 'Enter code manually', {
                'code': 'Barcode / QR',
              });
              if (v != null) {
                finish(v['code']!);
              } else if (mounted) {
                try {
                  await controller.start();
                } catch (_) {
                  if (context.mounted)
                    message(
                      context,
                      'Could not open the camera. Enter the code manually.',
                    );
                }
              }
            },
            icon: const Icon(Icons.keyboard),
            label: const Text('Enter code manually'),
          ),
        ),
      ],
    ),
  );
}

class PackingPage extends StatefulWidget {
  final WarehouseApi api;
  final bool demo;
  final int transferId;
  const PackingPage({
    super.key,
    required this.api,
    required this.demo,
    required this.transferId,
  });
  @override
  State<PackingPage> createState() => _PackingState();
}

class _PackingState extends State<PackingPage> {
  Json? dispatch;
  Json summary = {};
  List<Json> packages = [];
  bool busy = false;
  String? error;
  String get base => '/api/warehouse/staff/packing';
  String get preview =>
      '/api/warehouse/staff/packing-submissions/transfers/${widget.transferId}';
  @override
  void initState() {
    super.initState();
    if (widget.demo) {
      summary = widget.api.demoPacking.putIfAbsent(
        widget.transferId,
        () => {
          'transferCode': 'ST-DEMO-${widget.transferId}',
          'status': 'PACKING',
          'totalPickedQuantity': 36,
          'totalPackedQuantity': 0,
          'packages': <Json>[],
        },
      );
      packages = (summary['packages'] as List).cast<Json>();
    } else {
      load();
    }
  }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      final r = await widget.api.call('GET', preview);
      summary = Map<String, dynamic>.from(r);
      try {
        await widget.api.rememberPacking(widget.transferId);
      } catch (_) {}
      packages = (summary['packages'] as List? ?? [])
          .map((x) => Map<String, dynamic>.from(x as Map))
          .toList();
      error = null;
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> run(Future<void> Function() work) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await work();
      if (!widget.demo) {
        await load();
      }
      if (mounted)
        message(
          context,
          error == null ? 'Packing updated.' : 'The action was submitted, but its new status could not be loaded. Refresh before continuing.',
        );
    } catch (e) {
      if (mounted) {
        setState(() => error = e.toString());
        message(context, e);
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  int get packed => widget.demo
      ? packages.fold<int>(0, (a, p) => a + (p['totalQuantity'] as int? ?? 0))
      : (summary['totalPackedQuantity'] as int? ?? 0);
  bool get ready => packingReady({
    ...summary,
    'packages': packages,
    'totalPackedQuantity': packed,
  });
  bool get canCreate =>
      summary['completedAt'] == null &&
      (summary['totalPickedQuantity'] as num? ?? 0) > packed &&
      packages.every((p) => (p['totalQuantity'] as num? ?? 0) > 0);
  Future<void> create() async => run(() async {
    if (!canCreate)
      throw Exception(
        'Use an existing empty package or check quantities before creating another.',
      );
    if (widget.demo) {
      packages.add({
        'packageId': packages.length + 1,
        'packageCode': 'PK-DEMO-${packages.length + 1}',
        'packageStatus': 'PACKING',
        'totalQuantity': 0,
        'items': <Json>[],
      });
    } else {
      await widget.api.call(
        'POST',
        '$base/transfers/${widget.transferId}/packages',
      );
    }
  });
  Future<void> add(Json p) async {
    final code = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScanPage()),
    );
    if (code == null || !mounted) return;
    final v = await fields(
      context,
      'Add product to package',
      {'productBarcode': 'Barcode', 'quantity': 'Quantity'},
      numeric: {'quantity'},
      initial: {'productBarcode': code, 'quantity': '1'},
    );
    if (v == null || !mounted) return;
    final qty = int.parse(v['quantity']!);
    if (qty < 1) {
      message(context, 'Quantity must be greater than zero.');
      return;
    }
    await run(() async {
      if (widget.demo) {
        final samples = demoRows(2).first['items'] as List;
        final match = samples
            .where((x) => x['barcode'] == v['productBarcode'])
            .toList();
        if (match.isEmpty)
          throw Exception('This product is not in the demo task.');
        final already = packages
            .expand((p) => (p['items'] as List? ?? []))
            .where((x) => x['barcode'] == v['productBarcode'])
            .fold<int>(0, (a, x) => a + (x['quantity'] as int));
        if (qty + already > (match.first['approvedQuantity'] as int))
          throw Exception('Quantity exceeds picked stock.');
        p['totalQuantity'] += qty;
        (p['items'] as List).add({
          'barcode': v['productBarcode'],
          'quantity': qty,
        });
      } else {
        await widget.api.call('POST', '$base/packages/${p['packageId']}/scan', {
          'productBarcode': v['productBarcode'],
          'quantity': qty,
        });
      }
    });
  }

  Future<void> seal(Json p) async {
    final v = await fields(context, 'Seal package', {
      'sealNumber': 'Seal number',
    });
    if (v == null || !mounted) return;
    await run(() async {
      if (widget.demo) {
        if (p['totalQuantity'] == 0)
          throw Exception('Cannot seal an empty package.');
        p['sealNumber'] = v['sealNumber'];
        p['packageStatus'] = 'PACKED';
      } else {
        await widget.api.call(
          'PATCH',
          '$base/packages/${p['packageId']}/seal',
          {'sealNumber': v['sealNumber']},
        );
      }
    });
  }

  Future<void> submit() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Submit for manager approval?'),
        content: Text(
          'Packed $packed products in ${packages.length} packages. After submitting, the task becomes PACKED.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('Submit results'),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    await run(() async {
      if (widget.demo) {
        summary['status'] = 'PACKED';
      } else {
        if (summary['completedAt'] == null) {
          await widget.api.call(
            'PATCH',
            '$base/transfers/${widget.transferId}/complete',
          );
          summary['completedAt'] = 'completed';
        }
        final r = await widget.api.call('POST', '$preview/submit');
        if (r is Map) dispatch = Map<String, dynamic>.from(r);
      }
    });
    final d = dispatch;
    if (!mounted || d == null) return;
    if (d['shipmentCode'] == null) {
      // 2026-11-13: staff only submits; the warehouse manager reviews packing
      // and that creates the trip.
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Packing submitted. Waiting for the warehouse manager to review it.'),
        ),
      );
      return;
    }
    await showDialog<void>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Ready to ship'),
        content: Text(
          d['driverName'] != null
              ? 'Trip ${d['shipmentCode']} was sent to driver ${d['driverName']}. '
                    'Hand over the packages when the driver arrives.'
              : 'Trip ${d['shipmentCode']} is waiting for a free driver. '
                    'The system keeps trying and assigns the nearest one automatically.',
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(c),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> inspect(Json p) async {
    try {
      final r = widget.demo
          ? p
          : await widget.api.call('GET', '$base/packages/${p['packageId']}');
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (c) => AlertDialog(
          title: Text('${p['packageCode']}'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Sealed: ${r['sealNumber'] ?? 'Not sealed'}'),
                const SizedBox(height: 14),
                for (final x in (r['items'] as List? ?? []))
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Text(
                      '${x['productName'] ?? x['barcode'] ?? x['productCode'] ?? 'Products'} • ${x['quantity'] ?? '—'}',
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(c),
              child: const Text('Close'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) message(context, e);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Packing'),
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
          '${summary['transferCode'] ?? 'Task #${widget.transferId}'}',
          style: const TextStyle(fontSize: 27, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        Text(statusLabel(summary['status'])),
        if (widget.demo)
          const Text(
            'DEMO • Product codes: 8938500010011 (24), 8938500010028 (12)',
          ),
        const SizedBox(height: 24),
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: ink,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'PACKING PROGRESS',
                style: TextStyle(color: Colors.white70, letterSpacing: 2),
              ),
              const SizedBox(height: 12),
              Text(
                '$packed / ${summary['totalPickedQuantity'] ?? '—'} products',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '${packages.length} packages',
                style: const TextStyle(color: Colors.white70),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        if (busy) const LinearProgressIndicator(),
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red)),
        for (final p in packages)
          Card(
            elevation: 0,
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.inventory_2_outlined, color: teal),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          '${p['packageCode']}',
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 18,
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: 'Print QR label',
                        onPressed: busy
                            ? null
                            : () async {
                                try {
                                  await printPackage(p);
                                } catch (e) {
                                  if (context.mounted) message(context, e);
                                }
                              },
                        icon: const Icon(Icons.print_outlined),
                      ),
                      IconButton(
                        onPressed: busy ? null : () => inspect(p),
                        icon: const Icon(Icons.info_outline),
                      ),
                    ],
                  ),
                  Text(
                    '${p['totalQuantity'] ?? 0} products • ${p['packageStatus'] == 'PACKED' ? 'Sealed' : 'Packing'}',
                  ),
                  if (p['sealNumber'] != null) Text('Seal: ${p['sealNumber']}'),
                  if (error == null &&
                      summary['completedAt'] == null &&
                      summary['status'] == 'PACKING' &&
                      p['packageStatus'] != 'PACKED')
                    Wrap(
                      spacing: 8,
                      children: [
                        TextButton.icon(
                          onPressed: busy ? null : () => add(p),
                          icon: const Icon(Icons.qr_code_scanner),
                          label: const Text('Add product'),
                        ),
                        TextButton.icon(
                          onPressed: busy ? null : () => seal(p),
                          icon: const Icon(Icons.lock_outline),
                          label: const Text('Sealed'),
                        ),
                      ],
                    ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 16),
        if (summary['status'] == 'PACKING') ...[
          OutlinedButton.icon(
            onPressed: busy || error != null || !canCreate ? null : create,
            icon: const Icon(Icons.add),
            label: const Text('Create package'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: busy || error != null || !ready ? null : submit,
            child: const Text('Submit for manager approval'),
          ),
          const SizedBox(height: 12),
          const Text(
            'Pack all quantities and seal every package before submitting.',
            textAlign: TextAlign.center,
          ),
        ],
      ],
    ),
  );
}


/// Big "go here next" card for putaway and picking.
class _NextStopCard extends StatelessWidget {
  final String title, location, place, product, quantity;
  final String? reason;
  final VoidCallback? onConfirm;
  const _NextStopCard({
    required this.title,
    required this.location,
    required this.place,
    required this.product,
    required this.quantity,
    required this.onConfirm,
    this.reason,
  });

  @override
  Widget build(BuildContext context) => Card(
    elevation: 0,
    color: brandSoft,
    margin: const EdgeInsets.only(bottom: 18),
    child: Padding(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title.toUpperCase(),
            style: const TextStyle(
              letterSpacing: 1.5,
              fontSize: 12,
              color: Colors.black54,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.place_rounded, color: teal, size: 30),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  location,
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    color: teal,
                  ),
                ),
              ),
            ],
          ),
          if (place.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(left: 38, top: 2),
              child: Text(place, style: const TextStyle(fontSize: 16)),
            ),
          if (reason != null)
            Padding(
              padding: const EdgeInsets.only(left: 38, top: 2),
              child: Text(
                reason!,
                style: const TextStyle(color: Colors.black54),
              ),
            ),
          const Divider(height: 24),
          Text(
            '$product  ×  $quantity',
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
          if (onConfirm != null) ...[
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: onConfirm,
              icon: const Icon(Icons.qr_code_scanner),
              label: const Text('Arrived — scan & confirm'),
            ),
          ],
        ],
      ),
    ),
  );
}
