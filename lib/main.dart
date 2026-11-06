import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'api.dart';
import 'staff_features.dart';
import 'brand.dart';
import 'auth.dart';
import 'workflow.dart';
import 'staff_lookup.dart';
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

void message(BuildContext context, Object error) =>
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(error.toString().replaceFirst('Exception: ', ''))),
    );
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
  int nav = 0, selected = 0;
  bool loading = true;
  String query = '';
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
          next[i] =
              widget.demo
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

  Future<void> open(int i, Json row) async {
    await Navigator.push(
      context,
      MaterialPageRoute<void>(
        builder:
            (_) => TaskPage(
              api: widget.api,
              demo: widget.demo,
              module: i,
              initial: row,
            ),
      ),
    );
    if (mounted) refresh();
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
          Text('ToyStorage', style: TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
      actions: [
        IconButton(
          tooltip: 'Làm mới',
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
          label: 'Tổng quan',
        ),
        NavigationDestination(
          icon: Icon(Icons.assignment_outlined),
          label: 'Công việc',
        ),
        NavigationDestination(
          icon: Icon(Icons.widgets_outlined),
          label: 'Nghiệp vụ',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          label: 'Tài khoản',
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
                  'DEMO • Dữ liệu mẫu, không ghi vào kho thật.',
                ),
              ),
            if (nav == 0) ...[
              Text(
                'Xin chào, ${widget.user['name'] ?? 'bạn'}',
                style: const TextStyle(
                  fontSize: 25,
                  fontWeight: FontWeight.w800,
                  color: ink,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Hôm nay bạn cần xử lý những gì?',
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
                      'CÔNG VIỆC HIỆN CÓ',
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
                          ? 'Một số danh sách chưa tải được. Kéo xuống để thử lại.'
                          : 'Phiếu nhập • Cất hàng • Soạn hàng • Kiểm kê',
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
                      onTap:
                          () => setState(() {
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
                                  ? 'Không tải được'
                                  : loading
                                  ? 'Đang tải…'
                                  : '${data[i].length} công việc',
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
              OutlinedButton.icon(
                onPressed:
                    () => Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                        builder:
                            (_) => PackingBookmarks(
                              api: widget.api,
                              demo: widget.demo,
                            ),
                      ),
                    ),
                icon: const Icon(Icons.inventory_2_outlined),
                label: const Text('Đóng gói & in mã kiện'),
              ),
              const SizedBox(height: 22),
              const Text(
                'Công việc gần đây',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 14),
              if (!loading &&
                  errors.isEmpty &&
                  data.every((list) => list.isEmpty))
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Text(
                    'Bạn chưa có công việc được giao.',
                    style: TextStyle(color: muted),
                  ),
                ),
              for (int i = 0; i < 4; i++)
                ...data[i].take(2).map((r) => tile(i, r)),
            ],
            if (nav == 1) ...[
              const Text(
                'Công việc trong kho',
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
                        onSelected: (_) => setState(() => selected = i),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                onChanged: (v) => setState(() => query = v.toLowerCase()),
                decoration: const InputDecoration(
                  hintText: 'Tìm mã phiếu, nhà cung cấp…',
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
                  child: Text('Chưa có công việc trong danh sách này.'),
                ),
              ...data[selected]
                  .where((r) => r.toString().toLowerCase().contains(query))
                  .map((r) => tile(selected, r)),
            ],
            if (nav == 2) FeatureMenu(api: widget.api, demo: widget.demo),
            if (nav == 3) ...[
              const Icon(Icons.account_circle_outlined, size: 80, color: teal),
              const SizedBox(height: 16),
              Center(
                child: Text(
                  '${widget.user['name'] ?? 'Nhân viên'}',
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Center(
                child: Text('${widget.user['email'] ?? 'Chế độ trải nghiệm'}'),
              ),
              const SizedBox(height: 32),
              const ListTile(
                leading: Icon(Icons.shield_outlined),
                title: Text('Warehouse Staff'),
                subtitle: Text('Thực hiện công việc và gửi quản lý xác nhận.'),
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
                child: const Text('Đăng xuất'),
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
      'Tiếp tục đóng gói',
      {'transferId': 'ID phiếu chuyển (số)'},
      numeric: {'transferId'},
    );
    if (values == null || !mounted) return;
    await Navigator.push(
      context,
      MaterialPageRoute<void>(
        builder:
            (_) => PackingPage(
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
    modules[i].codeKey:
        ['GR-2026-0042', 'PA-2026-0018', 'ST-2026-0021', 'SC-2026-0009'][i],
    'status':
        ['CONFIRMED', 'AVAILABLE', 'PENDING_SOURCE_CONFIRMATION', 'PLANNED'][i],
    'supplierName': 'Nhà cung cấp Toy World',
    'warehouseName': 'Kho trung tâm',
    'toWarehouseName': 'Cửa hàng Quận 1',
    'items': <Json>[
      {
        'itemId': 1,
        'productId': 11,
        'productCode': 'TOY-001',
        'productName': 'Bộ xếp hình sáng tạo',
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
        'productName': 'Xe mô hình thể thao',
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
}) async {
  final controllers = {
    for (final k in labels.keys)
      k: TextEditingController(text: initial[k] ?? ''),
  };
  final key = GlobalKey<FormState>();
  final result = await showDialog<Map<String, String>>(
    context: context,
    builder:
        (c) => AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: Form(
              key: key,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children:
                    labels.entries
                        .map(
                          (e) => Padding(
                            padding: const EdgeInsets.only(bottom: 14),
                            child: TextFormField(
                              controller: controllers[e.key],
                              keyboardType:
                                  numeric.contains(e.key)
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
                                          icon: const Icon(
                                            Icons.qr_code_scanner,
                                          ),
                                          onPressed: () async {
                                            final scanned =
                                                await Navigator.push<String>(
                                                  c,
                                                  MaterialPageRoute(
                                                    builder:
                                                        (_) => const ScanPage(),
                                                  ),
                                                );
                                            if (scanned != null && c.mounted)
                                              controllers[e.key]!.text =
                                                  scanned;
                                          },
                                        )
                                        : null,
                              ),
                              validator: (v) {
                                if (optional.contains(e.key) &&
                                    (v ?? '').trim().isEmpty)
                                  return null;
                                if ((v ?? '').trim().isEmpty)
                                  return 'Vui lòng nhập thông tin';
                                if (numeric.contains(e.key) &&
                                    (int.tryParse(v!) == null ||
                                        int.parse(v) < 0))
                                  return 'Nhập số nguyên không âm';
                                return null;
                              },
                            ),
                          ),
                        )
                        .toList(),
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(c),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () {
                if (key.currentState!.validate())
                  Navigator.pop(c, {
                    for (final e in controllers.entries)
                      e.key: e.value.text.trim(),
                  });
              },
              child: const Text('Xác nhận'),
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
  const TaskPage({
    super.key,
    required this.api,
    required this.demo,
    required this.module,
    required this.initial,
  });
  @override
  State<TaskPage> createState() => _TaskState();
}

class _TaskState extends State<TaskPage> {
  late Json task;
  bool busy = false;
  String? error;
  Module get m => modules[widget.module];
  dynamic get id => task[m.idKey];
  List<Json> get items =>
      (task['items'] as List? ?? [])
          .map((x) => Map<String, dynamic>.from(x as Map))
          .toList();
  @override
  void initState() {
    super.initState();
    task = widget.initial;
    if (!widget.demo) load();
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
        message(context, 'Đã cập nhật công việc.');
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
    ['PENDING_SOURCE_CONFIRMATION'],
    ['PLANNED'],
  ][widget.module].contains(task['status']);
  bool get done => taskComplete(widget.module, task);
  Future<void> editItem(Json item) async {
    if (widget.module == 0) {
      final v = await fields(
        context,
        'Kiểm nhận ${item['productCode'] ?? ''}',
        {
          'actualQuantity': 'Số lượng thực nhận',
          'damagedQuantity': 'Trong đó hư hỏng',
          'packageCode': 'Mã kiện (nếu có)',
          'notes': 'Ghi chú (nếu có)',
        },
        numeric: {'actualQuantity', 'damagedQuantity'},
        optional: {'packageCode', 'notes'},
        initial: {
          'actualQuantity': '${item['actualQuantity'] ?? ''}',
          'damagedQuantity': '${item['damagedQuantity'] ?? 0}',
          'notes': '${item['notes'] ?? ''}',
          'packageCode': '${item['packageCode'] ?? ''}',
        },
      );
      if (v == null || !mounted) return;
      final actual = int.parse(v['actualQuantity']!),
          damaged = int.parse(v['damagedQuantity']!);
      if (damaged > actual) {
        message(context, 'Hàng hỏng không thể lớn hơn thực nhận.');
        return;
      }
      if ((actual != item['expectedQuantity'] || damaged > 0) &&
          v['notes']!.isEmpty) {
        message(context, 'Nhập ghi chú khi thiếu, thừa hoặc có hàng hỏng.');
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
        widget.module == 3 ? 'Ghi nhận số đếm' : 'Quét và xác nhận',
        {
          'productBarcode': 'Mã vạch sản phẩm',
          'locationCode': 'Mã vị trí',
          'quantity':
              widget.module == 3 ? 'Số lượng đếm được' : 'Số lượng thực hiện',
        },
        numeric: {'quantity'},
      );
      if (v == null || !mounted) return;
      final qty = int.parse(v['quantity']!);
      if (widget.module != 3 && qty < 1) {
        message(context, 'Số lượng phải lớn hơn 0.');
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
    final found =
        items
            .where((x) => x['barcode'] == code || x['productCode'] == code)
            .toList();
    if (found.isEmpty) {
      message(context, 'Mã $code không có trong phiếu này.');
      return;
    }
    final selected = await chooseItemLocation(context, found);
    if (selected != null && mounted) await editItem(selected);
  }

  Future<void> complete() async {
    final ok = await showDialog<bool>(
      context: context,
      builder:
          (c) => AlertDialog(
            title: const Text('Hoàn tất công việc?'),
            content: const Text(
              'Kiểm tra lại số lượng trước khi gửi. Kết quả được chuyển sang bước tiếp theo của quy trình.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(c, false),
                child: const Text('Kiểm tra lại'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(c, true),
                child: const Text('Hoàn tất'),
              ),
            ],
          ),
    );
    if (ok != true || !mounted) return;
    await action(
      'PATCH',
      ['/finish-inspection', '/complete', '/complete', '/submit'][widget
          .module],
      demo:
          () =>
              task['status'] =
                  [
                    'INSPECTED',
                    'COMPLETED',
                    'PACKING',
                    'PENDING_CONFIRMATION',
                  ][widget.module],
    );
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
          Text('Đơn mua: ${task['purchaseOrderCode']}'),
        if (task['scheduledDate'] != null)
          Text('Lịch kiểm kê: ${task['scheduledDate']}'),
        if (task['assignedToName'] != null)
          Text('Người thực hiện: ${task['assignedToName']}'),
        const SizedBox(height: 22),
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red)),
        if (busy) const LinearProgressIndicator(),
        if (canStart)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: FilledButton.icon(
              onPressed:
                  busy
                      ? null
                      : () => action(
                        'PATCH',
                        widget.module == 0 ? '/start-receiving' : '/start',
                        demo:
                            () =>
                                task['status'] =
                                    [
                                      'RECEIVING',
                                      'IN_PROGRESS',
                                      'PICKING',
                                      'COUNTING',
                                    ][widget.module],
                      ),
              icon: const Icon(Icons.play_arrow),
              label: const Text('Nhận việc và bắt đầu'),
            ),
          ),
        if (active)
          OutlinedButton.icon(
            onPressed: busy || error != null ? null : scan,
            icon: const Icon(Icons.qr_code_scanner),
            label: const Text('Quét mã sản phẩm'),
          ),
        const SizedBox(height: 18),
        if (active && widget.module != 3)
          TextButton.icon(
            onPressed:
                busy
                    ? null
                    : () => Navigator.push(
                      context,
                      MaterialPageRoute<Json>(
                        builder:
                            (_) => ReportForm(
                              api: widget.api,
                              demo: widget.demo,
                              shortage: false,
                            ),
                      ),
                    ),
            icon: const Icon(Icons.broken_image_outlined),
            label: const Text('Báo cáo hàng hỏng'),
          ),
        Text(
          '${items.length} sản phẩm',
          style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 12),
        for (final item in items)
          Card(
            elevation: 0,
            color: Colors.white,
            margin: const EdgeInsets.only(bottom: 14),
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${item['productName'] ?? item['productCode'] ?? 'Sản phẩm'}',
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${item['productCode'] ?? ''} • ${item['barcode'] ?? ''}',
                    style: const TextStyle(color: Colors.black54),
                  ),
                  const Divider(height: 28),
                  if (widget.module == 0 &&
                      active &&
                      item['actualQuantity'] != null &&
                      (item['actualQuantity'] as num) <
                          (item['expectedQuantity'] as num))
                    TextButton.icon(
                      onPressed:
                          busy
                              ? null
                              : () => Navigator.push(
                                context,
                                MaterialPageRoute<Json>(
                                  builder:
                                      (_) => ReportForm(
                                        api: widget.api,
                                        demo: widget.demo,
                                        shortage: true,
                                        receipt: task,
                                        item: item,
                                      ),
                                ),
                              ),
                      icon: const Icon(Icons.report_outlined),
                      label: const Text('Báo thiếu & đính kèm ảnh'),
                    ),
                  if (widget.module == 0)
                    Text(
                      'Dự kiến: ${item['expectedQuantity'] ?? '—'}   •   Thực nhận: ${item['actualQuantity'] ?? '—'}\nHỏng: ${item['damagedQuantity'] ?? 0}   •   ${item['inspected'] == true ? 'Đã kiểm' : 'Chưa kiểm'}',
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
                        Text(
                          '${widget.module == 1 ? item['toLocationCode'] : item['locationCode']}',
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            color: teal,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'Còn lại: ${item['remainingQuantity'] ?? '—'}  •  ${widget.module == 1 ? 'Đã cất' : 'Đã soạn'}: ${item[widget.module == 1 ? 'putawayQuantity' : 'pickedQuantity'] ?? 0}',
                    ),
                  ],
                  if (widget.module == 3)
                    Text(
                      'Vị trí: ${item['locationCode'] ?? '—'}\nĐếm lần đầu: ${item['firstCountQuantity'] ?? 'Chưa ghi nhận'}${item['secondCountQuantity'] != null ? '\nĐếm lại: ${item['secondCountQuantity']}' : ''}${item['finalQuantity'] != null ? '\nKết quả: ${item['finalQuantity']}' : ''}',
                      style: const TextStyle(height: 1.8),
                    ),
                  if (active)
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton.icon(
                        onPressed:
                            busy || error != null ? null : () => editItem(item),
                        icon: const Icon(Icons.edit_outlined, size: 18),
                        label: const Text('Ghi nhận'),
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
                  ? 'Gửi kết quả kiểm kê'
                  : 'Hoàn tất ${m.title.toLowerCase()}',
            ),
          ),
        if (active && !done)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: Text(
              'Ghi nhận đủ các sản phẩm để hoàn tất. Nếu thiếu hàng khi soạn hoặc vị trí sai, hãy liên hệ quản lý điều chỉnh phiếu.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
          ),
        if (widget.module == 2 && task['status'] == 'PACKING')
          FilledButton.icon(
            onPressed:
                () => Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder:
                        (_) => PackingPage(
                          api: widget.api,
                          demo: widget.demo,
                          transferId: id,
                        ),
                  ),
                ),
            icon: const Icon(Icons.inventory_2_outlined),
            label: const Text('Chuyển sang đóng gói'),
          ),
      ],
    ),
  );
}

class ScanPage extends StatefulWidget {
  const ScanPage({super.key});
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
      title: const Text('Quét mã sản phẩm'),
      actions: [
        IconButton(
          onPressed: () async {
            try {
              await controller.toggleTorch();
            } catch (e) {
              if (context.mounted)
                message(context, 'Thiết bị không hỗ trợ đèn flash.');
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
            errorBuilder:
                (context, error) => const Center(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'Không mở được camera. Kiểm tra quyền camera hoặc nhập mã thủ công.',
                    ),
                  ),
                ),
          ),
        ),
        const Padding(
          padding: EdgeInsets.all(20),
          child: Text('Đưa mã vạch hoặc QR vào vùng camera.'),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: OutlinedButton.icon(
            onPressed: () async {
              try {
                await controller.stop();
              } catch (_) {}
              if (!context.mounted) return;
              final v = await fields(context, 'Nhập mã thủ công', {
                'code': 'Mã sản phẩm',
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
                      'Không mở được camera. Bạn có thể nhập mã thủ công.',
                    );
                }
              }
            },
            icon: const Icon(Icons.keyboard),
            label: const Text('Nhập mã thủ công'),
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
      packages =
          (summary['packages'] as List? ?? [])
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
          error == null
              ? 'Đã cập nhật đóng gói.'
              : 'Thao tác đã gửi. Chưa tải được trạng thái mới; hãy tải lại trước khi thao tác tiếp.',
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

  int get packed =>
      widget.demo
          ? packages.fold<int>(
            0,
            (a, p) => a + (p['totalQuantity'] as int? ?? 0),
          )
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
        'Dùng kiện rỗng hiện có hoặc kiểm tra số lượng trước khi tạo thêm kiện.',
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
      'Thêm sản phẩm vào kiện',
      {'productBarcode': 'Mã vạch', 'quantity': 'Số lượng'},
      numeric: {'quantity'},
      initial: {'productBarcode': code, 'quantity': '1'},
    );
    if (v == null || !mounted) return;
    final qty = int.parse(v['quantity']!);
    if (qty < 1) {
      message(context, 'Số lượng phải lớn hơn 0.');
      return;
    }
    await run(() async {
      if (widget.demo) {
        final samples = demoRows(2).first['items'] as List;
        final match =
            samples.where((x) => x['barcode'] == v['productBarcode']).toList();
        if (match.isEmpty) throw Exception('Sản phẩm không thuộc phiếu demo.');
        final already = packages
            .expand((p) => (p['items'] as List? ?? []))
            .where((x) => x['barcode'] == v['productBarcode'])
            .fold<int>(0, (a, x) => a + (x['quantity'] as int));
        if (qty + already > (match.first['approvedQuantity'] as int))
          throw Exception('Số lượng vượt số đã soạn.');
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
    final v = await fields(context, 'Niêm phong kiện', {
      'sealNumber': 'Số niêm phong',
    });
    if (v == null || !mounted) return;
    await run(() async {
      if (widget.demo) {
        if (p['totalQuantity'] == 0)
          throw Exception('Không thể niêm phong kiện rỗng.');
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
      builder:
          (c) => AlertDialog(
            title: const Text('Gửi quản lý xác nhận?'),
            content: Text(
              'Đã đóng $packed sản phẩm trong ${packages.length} kiện. Sau khi gửi, phiếu chuyển sang PACKED.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(c, false),
                child: const Text('Hủy'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(c, true),
                child: const Text('Gửi kết quả'),
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
        await widget.api.call('POST', '$preview/submit');
      }
    });
  }

  Future<void> inspect(Json p) async {
    try {
      final r =
          widget.demo
              ? p
              : await widget.api.call(
                'GET',
                '$base/packages/${p['packageId']}',
              );
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder:
            (c) => AlertDialog(
              title: Text('${p['packageCode']}'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Niêm phong: ${r['sealNumber'] ?? 'Chưa niêm phong'}'),
                    const SizedBox(height: 14),
                    for (final x in (r['items'] as List? ?? []))
                      Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: Text(
                          '${x['productName'] ?? x['barcode'] ?? x['productCode'] ?? 'Sản phẩm'} • ${x['quantity'] ?? '—'}',
                        ),
                      ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(c),
                  child: const Text('Đóng'),
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
      title: const Text('Đóng gói'),
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
          '${summary['transferCode'] ?? 'Phiếu #${widget.transferId}'}',
          style: const TextStyle(fontSize: 27, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        Text(statusLabel(summary['status'])),
        if (widget.demo)
          const Text(
            'DEMO • Mã sản phẩm: 8938500010011 (24), 8938500010028 (12)',
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
                'TIẾN ĐỘ ĐÓNG GÓI',
                style: TextStyle(color: Colors.white70, letterSpacing: 2),
              ),
              const SizedBox(height: 12),
              Text(
                '$packed / ${summary['totalPickedQuantity'] ?? '—'} sản phẩm',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '${packages.length} kiện hàng',
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
                        tooltip: 'In nhãn QR',
                        onPressed:
                            busy
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
                    '${p['totalQuantity'] ?? 0} sản phẩm • ${p['packageStatus'] == 'PACKED' ? 'Đã niêm phong' : 'Đang đóng gói'}',
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
                          label: const Text('Thêm sản phẩm'),
                        ),
                        TextButton.icon(
                          onPressed: busy ? null : () => seal(p),
                          icon: const Icon(Icons.lock_outline),
                          label: const Text('Niêm phong'),
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
            label: const Text('Tạo kiện mới'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: busy || error != null || !ready ? null : submit,
            child: const Text('Gửi quản lý xác nhận'),
          ),
          const SizedBox(height: 12),
          const Text(
            'Đóng đủ số lượng và niêm phong tất cả kiện trước khi gửi.',
            textAlign: TextAlign.center,
          ),
        ],
      ],
    ),
  );
}
