import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio_web_adapter/dio_web_adapter.dart';

void configureTransport(Dio dio, CookieJar jar) {
  dio.httpClientAdapter = BrowserHttpClientAdapter(withCredentials: true);
}
