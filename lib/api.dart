import 'package:shared_preferences/shared_preferences.dart';
import 'package:dio/dio.dart';
import 'session_native.dart' if (dart.library.html) 'session_web.dart';

typedef Json = Map<String, dynamic>;

class WarehouseApi {
  late final SessionTransport session;
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
  WarehouseApi(String baseUrl) {
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
      final recovery =
          path == '/api/auth/forgot-password' ||
          path == '/api/auth/reset-password';
      if (recovery && [401, 403, 404, 405].contains(code))
        throw Exception(
          'Chức năng khôi phục mật khẩu chưa được kích hoạt trên máy chủ. Vui lòng liên hệ quản trị viên.',
        );
      if (code == 401) {
        if (path == '/api/auth/login')
          throw Exception(
            'Email hoặc mật khẩu không đúng, hoặc tài khoản chưa được kích hoạt.',
          );
        if (!_expired) {
          _expired = true;
          onUnauthorized?.call();
        }
        throw Exception('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
      }
      if (code == 403)
        throw Exception(
          'Bạn không có quyền thực hiện thao tác hoặc công việc đang do nhân viên khác xử lý.',
        );
      if (e.response == null) {
        if (method != 'GET')
          throw Exception(
            'Mất kết nối khi gửi dữ liệu. Hãy tải lại phiếu để kiểm tra kết quả trước khi gửi lại.',
          );
        throw Exception(
          'Không kết nối được máy chủ. Kiểm tra mạng và thử tải lại.',
        );
      }
      final data = e.response?.data;
      throw Exception(
        data is Map
            ? (data['message'] ??
                data['error'] ??
                'Không thể thực hiện thao tác.')
            : 'Không kết nối được server hoặc server trả lỗi (${e.response?.statusCode ?? 'mạng'}).',
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
    try {
      await call('POST', '/api/auth/logout');
    } finally {
      await session.clear();
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
    'Nhập kho',
    'receiptId',
    'receiptCode',
    '/api/warehouse/goods-receipts/receiving',
    '/api/warehouse/goods-receipts',
  ),
  Module(
    'Cất hàng',
    'taskId',
    'taskCode',
    '/api/warehouse/putaway/my-tasks',
    '/api/warehouse/putaway',
  ),
  Module(
    'Soạn hàng',
    'transferId',
    'transferCode',
    '/api/warehouse/picking/transfers',
    '/api/warehouse/picking/transfers',
  ),
  Module(
    'Kiểm kê',
    'stockCountId',
    'countCode',
    '/api/warehouse/staff/stock-counts',
    '/api/warehouse/staff/stock-counts',
  ),
];
String statusLabel(dynamic s) =>
    const {
      'SHIPPED': 'Đang chuyển về kho',
      'INSPECTING': 'Đang kiểm hàng trả',
      'RECEIVED': 'Đã nhận',
      'OPEN': 'Chờ xử lý',
      'INVESTIGATING': 'Đang xử lý',
      'RESOLVED': 'Đã xử lý',
      'CONFIRMED': 'Chờ kiểm nhận',
      'PENDING_CONFIRMATION': 'Chờ quản lý xác nhận',
      'AVAILABLE': 'Chờ nhận việc',
      'IN_PROGRESS': 'Đang thực hiện',
      'PENDING_SOURCE_CONFIRMATION': 'Chờ soạn',
      'RECEIVING': 'Đang kiểm nhận',
      'INSPECTED': 'Chờ quản lý xác nhận',
      'PICKING': 'Đang soạn',
      'PACKING': 'Đang đóng gói',
      'PACKED': 'Đã gửi quản lý',
      'COMPLETED': 'Hoàn thành',
      'CREATED': 'Mới tạo',
      'PLANNED': 'Đã lên kế hoạch',
      'ARRIVED': 'Đã đến kho',
      'SUBMITTED': 'Đã gửi quản lý',
      'COUNTING': 'Đang kiểm kê',
      'RECOUNTING': 'Đang đếm lại',
      'SEALED': 'Đã niêm phong',
    }[s] ??
    (s?.toString() ?? 'Chưa có trạng thái');
