import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';

class SessionTransport {
  final jar = CookieJar();
  SessionTransport(Dio dio) {
    dio.interceptors.add(CookieManager(jar));
  }
  Future<void> clear() => jar.deleteAll();
}
