import 'package:shared_preferences/shared_preferences.dart';
import 'package:dio/dio.dart';

import 'session_native.dart' if (dart.library.html) 'session_web.dart';

typedef Json = Map<String, dynamic>;

class WarehouseApi {
  SessionTransport? session;
  final Future<void> Function()? sharedLogout;
  String accountScope = 'anonymous';
  void Function()? onUnauthorized;
  bool _expired = false;
  final Map<int, Json> demoPacking = {};
  String get bookmarkKey => 'packing:${dio.options.baseUrl}:$accountScope';
  Future<List<int>> packingIds() async {
    final prefs = await SharedPreferences.getInstance();
    return (prefs.getStringList(bookmarkKey) ?? [])
        .map(int.tryParse)
        .whereType<int>()
        .toList();
  }

  Future<void> rememberPacking(int id) async {
    final ids = await packingIds();
    if (!ids.contains(id)) ids.insert(0, id);
    await (await SharedPreferences.getInstance()).setStringList(
      bookmarkKey,
      ids.take(100).map((i) => '$i').toList(),
    );
  }

  Future<void> forgetPacking(int id) async {
    final ids = await packingIds();
    ids.remove(id);
    await (await SharedPreferences.getInstance()).setStringList(
      bookmarkKey,
      ids.map((i) => '$i').toList(),
    );
  }

  late final Dio dio;
  WarehouseApi(String baseUrl, {Dio? client, this.sharedLogout}) {
    if (client != null) {
      dio = client;
      return;
    }
    dio = Dio(
      BaseOptions(
        baseUrl: baseUrl.replaceFirst(RegExp(r'/$'), ''),
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 25),
      ),
    );
    session = SessionTransport(dio);
  }
  Future<dynamic> call(String method, String path, [dynamic body]) async {
    try {
      return (await dio.request<dynamic>(
        path,
        data: body,
        options: Options(method: method),
      )).data;
    } on DioException catch (e) {
      final code = e.response?.statusCode;
      final accountRequest = path.startsWith('/api/auth/');
      final recovery =
          path == '/api/auth/forgot-password' ||
          path == '/api/auth/reset-password';
      if (recovery && [401, 403, 404, 405].contains(code))
        throw Exception(
          'Password recovery is not available on the server. Contact your administrator.',
        );
      if (code == 401) {
        if (path == '/api/auth/login')
          throw Exception(
            'Incorrect email or password, or the account is inactive.',
          );
        if (path == '/api/auth/2fa/verify')
          throw Exception('Invalid or expired verification code. Try again.');
        if (!_expired) {
          _expired = true;
          onUnauthorized?.call();
        }
        throw Exception('Your session has expired. Sign in again.');
      }
      if (code == 403)
        throw Exception(
          'You do not have permission, or another staff member is handling this task.',
        );
      if (e.response == null) {
        if (accountRequest)
          throw Exception(
            'Could not reach the sign-in service. Check your connection and try again.',
          );
        if (method != 'GET')
          throw Exception(
            'Connection lost while submitting. Reload the task to check the result before trying again.',
          );
        throw Exception(
          'Could not connect to the server. Check your network and retry.',
        );
      }
      if (code != null && code >= 500) {
        if (accountRequest)
          throw Exception(
            'Account service error ($code). Please try again or contact your administrator.',
          );
        throw Exception(
          'Server error ($code). Reload the task; if it persists, ask a manager to check the backend. Verify the result before submitting quantities again.',
        );
      }
      final data = e.response?.data;
      throw Exception(
        data is Map
            ? (data['message'] ??
                  data['error'] ??
                  'Could not complete the action.')
            : 'Could not connect to the server, or the server returned an error (${e.response?.statusCode ?? 'Network'}).',
      );
    }
  }

  Future<Json> login(String email, String password) async =>
      Map<String, dynamic>.from(
        await call('POST', '/api/auth/login', {
          'email': email,
          'password': password,
        }),
      );
  Future<void> logout() async {
    if (sharedLogout != null) return sharedLogout!();
    try {
      await call('POST', '/api/auth/logout');
    } finally {
      await session?.clear();
    }
  }
}

class Module {
  final String title, idKey, codeKey, listPath, base;
  const Module(this.title, this.idKey, this.codeKey, this.listPath, this.base);
  String detail(dynamic id) =>
      '$base/$id${idKey == 'receiptId' ? '/receiving-detail' : ''}';
}

const modules = [
  Module(
    'Receiving',
    'receiptId',
    'receiptCode',
    '/api/warehouse/goods-receipts/receiving',
    '/api/warehouse/goods-receipts',
  ),
  Module(
    'Putaway',
    'taskId',
    'taskCode',
    '/api/warehouse/putaway/my-tasks',
    '/api/warehouse/putaway',
  ),
  Module(
    'Picking',
    'transferId',
    'transferCode',
    '/api/warehouse/picking/transfers',
    '/api/warehouse/picking/transfers',
  ),
  Module(
    'Stock count',
    'stockCountId',
    'countCode',
    '/api/warehouse/staff/stock-counts',
    '/api/warehouse/staff/stock-counts',
  ),
];
String statusLabel(dynamic s) =>
    const {
      'SHIPPED': 'Returning to warehouse',
      'INSPECTING': 'Inspecting return',
      'RECEIVED': 'Received',
      'OPEN': 'Pending',
      'INVESTIGATING': 'In progress',
      'RESOLVED': 'Resolved',
      'CONFIRMED': 'Awaiting inspection',
      'PENDING_CONFIRMATION': 'Awaiting manager approval',
      'AVAILABLE': 'Available',
      'IN_PROGRESS': 'In progress',
      'PENDING_SOURCE_CONFIRMATION': 'Awaiting picking',
      'RECEIVING': 'Inspecting',
      'INSPECTED': 'Awaiting manager approval',
      'PICKING': 'Picking',
      'PACKING': 'Packing',
      'PACKED': 'Submitted for approval',
      'COMPLETED': 'Completed',
      'CREATED': 'Created',
      'PLANNED': 'Planned',
      'ARRIVED': 'At warehouse',
      'SUBMITTED': 'Submitted for approval',
      'COUNTING': 'Counting',
      'RECOUNTING': 'Recounting',
      'SEALED': 'Sealed',
    }[s] ??
    (s?.toString() ?? 'No status');
