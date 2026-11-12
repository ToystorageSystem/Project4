# ToyStorage Unified App

Merged warehouse/driver features from `app-lam` and store features from `dat-app`, sharing one login, cookie jar, CSRF client and role router.

From this directory:

```bash
flutter pub get
flutter run --dart-define=API_BASE_URL=http://YOUR_BACKEND_IP:8080
```

Use a Flutter stable release with Dart >=3.12.2. On iOS, deployment target is 15.5 and CocoaPods is required. New plugin dependencies must be resolved with `flutter pub get`; a new pubspec.lock will be generated.

See the bundle's HUONG_DAN_CHAY.md for the matching backend module, vehicle setup, manager web patch, GPS foreground-only behavior and routing configuration. Use lib/main.dart as the entry point. The original warehouse screens under lib/warehouse are integrated through the common session bridge; do not start their legacy main.dart as a separate app.

Validation: Dart syntax formatting and dependency/manifest checks passed, plus standalone offline role/password contract checks. Full Flutter analyze/test/build and real-device GPS verification remain unrun because the Flutter tool was blocked by automatic approval review when it attempted a cloud metadata endpoint. Backend and web verification are documented separately in the bundle.
