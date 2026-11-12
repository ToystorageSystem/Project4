import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';

void configureTransport(Dio dio, CookieJar jar) {
  dio.interceptors.add(CookieManager(jar));
}
