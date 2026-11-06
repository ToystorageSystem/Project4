import 'package:dio/dio.dart';
import 'package:dio_web_adapter/dio_web_adapter.dart';

class SessionTransport {
  SessionTransport(Dio dio) {
    dio.httpClientAdapter = BrowserHttpClientAdapter(withCredentials: true);
  }
  // HttpOnly cookies are managed by the browser; POST /logout invalidates the server session.
  Future<void> clear() async {}
}
