import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/providers/auth_notifier.dart';
import '../../features/auth/presentation/providers/auth_state.dart';
import '../../features/auth/presentation/screens/change_password_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/otp_screen.dart';
import '../../features/auth/presentation/screens/splash_screen.dart';
import '../../features/auth/presentation/screens/two_factor_setup_screen.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import '../../features/receiving/domain/entities/receiving_result.dart';
import '../../features/receiving/presentation/screens/manifest_detail_screen.dart';
import '../../features/receiving/presentation/screens/manifest_list_screen.dart';
import '../../features/receiving/presentation/screens/package_check_screen.dart';
import '../../features/receiving/presentation/screens/receiving_result_screen.dart';
import '../../features/sales/domain/entities/sale_detail.dart';
import '../../features/sales/presentation/screens/sale_detail_screen.dart';
import '../../features/sales/presentation/screens/sale_history_screen.dart';
import '../../features/sales/presentation/screens/sale_screen.dart';
import '../../features/shelves/presentation/screens/low_shelf_screen.dart';
import '../../features/shelves/presentation/screens/shelf_detail_screen.dart';
import '../../features/shelves/presentation/screens/shelf_list_screen.dart';
import '../../features/store/presentation/screens/product_detail_screen.dart';
import '../../features/store/presentation/screens/product_search_screen.dart';

class AppRoutes {
  // ----- Top-level pages (each one replaces the whole screen stack) -----
  static const splash = '/splash';
  static const login = '/login';
  static const otp = '/otp'; // stage VERIFY_2FA
  static const forceChangePassword = '/force-change-password'; // CHANGE_PASSWORD
  static const forceTwoFactor = '/force-two-factor'; // SETUP_2FA
  static const home = '/home';

  // ----- Child pages of /home (pushed on top of Home, Back returns) -----
  static const changePassword = '/home/change-password';

  // Store – product lookup
  static const storePrefix = '/home/store';
  static const storeProducts = '/home/store/products';
  static String storeProductDetail(int id) => '/home/store/products/$id';

  // Store – shelves
  static const storeShelves = '/home/store/shelves';
  static String storeShelfDetail(int id) => '/home/store/shelves/$id';
  static const lowShelf = '/home/store/low-shelf';

  // Store – sales
  static const sales = '/home/store/sales'; // new sale (cart)
  static const saleHistory = '/home/store/sales/history';
  static String saleDetail(int id) => '/home/store/sales/$id';

  // Store – receiving goods
  static const receiving = '/home/receiving';
  static String manifest(String code) => '$receiving/${Uri.encodeComponent(code)}';
  static String packageCheck(String manifestCode, String packageCode) =>
      '${manifest(manifestCode)}/packages/${Uri.encodeComponent(packageCode)}';
  static String receivingResult(String manifestCode) =>
      '${manifest(manifestCode)}/result';

  /// Pages that only make sense BEFORE the user is fully signed in
  static const _authFlowPages = {
    splash,
    login,
    otp,
    forceChangePassword,
    forceTwoFactor,
  };
}

final routerProvider = Provider<GoRouter>((ref) {
  // A Listenable that "rings a bell" every time auth state changes
  final authChanges = ValueNotifier<AuthState>(ref.read(authProvider));
  ref.listen<AuthState>(authProvider, (_, next) => authChanges.value = next);
  ref.onDispose(authChanges.dispose);

  final router = GoRouter(
    initialLocation: AppRoutes.splash,
    debugLogDiagnostics: kDebugMode,
    refreshListenable: authChanges,
    redirect: (context, state) =>
        _redirect(ref.read(authProvider), state.matchedLocation),
    routes: [
      GoRoute(path: AppRoutes.splash, builder: (c, s) => const SplashScreen()),
      GoRoute(path: AppRoutes.login, builder: (c, s) => const LoginScreen()),
      GoRoute(path: AppRoutes.otp, builder: (c, s) => const OtpScreen()),

      // ----- FORCED pages (sign-in stages): no Back, only Sign out -----
      GoRoute(
        path: AppRoutes.forceChangePassword,
        builder: (c, s) => const ChangePasswordScreen(forced: true),
      ),
      GoRoute(
        path: AppRoutes.forceTwoFactor,
        builder: (c, s) => const TwoFactorSetupScreen(),
      ),

      // ----- Home + child pages (child paths: NO leading "/") -----
      GoRoute(
        path: AppRoutes.home,
        builder: (c, s) => const HomeScreen(),
        routes: [
          GoRoute(
            path: 'change-password',
            builder: (c, s) => const ChangePasswordScreen(),
          ),
          GoRoute(
            path: 'store/products',
            builder: (c, s) => const ProductSearchScreen(),
            routes: [
              GoRoute(
                path: ':id',
                builder: (c, s) => ProductDetailScreen(
                  productId: int.tryParse(s.pathParameters['id'] ?? '') ?? 0,
                ),
              ),
            ],
          ),
          GoRoute(
            path: 'store/shelves',
            builder: (c, s) => const ShelfListScreen(),
            routes: [
              GoRoute(
                path: ':id',
                builder: (c, s) => ShelfDetailScreen(
                  shelfId: int.tryParse(s.pathParameters['id'] ?? '') ?? 0,
                ),
              ),
            ],
          ),
          GoRoute(
            path: 'store/low-shelf',
            builder: (c, s) => const LowShelfScreen(),
          ),
          GoRoute(
            path: 'store/sales',
            builder: (c, s) => const SaleScreen(),
            routes: [
              // 'history' BEFORE ':id' so it is not read as an id
              GoRoute(
                path: 'history',
                builder: (c, s) => const SaleHistoryScreen(),
              ),
              GoRoute(
                path: ':id',
                builder: (c, s) => SaleDetailScreen(
                  saleId: int.tryParse(s.pathParameters['id'] ?? '') ?? 0,
                  // passed by context.push(..., extra: sale) after checkout
                  initialSale: s.extra is SaleDetail ? s.extra as SaleDetail : null,
                ),
              ),
            ],
          ),
          GoRoute(
            path: 'receiving',
            builder: (c, s) => const ManifestListScreen(),
            routes: [
              GoRoute(
                path: ':code',
                builder: (c, s) =>
                    ManifestDetailScreen(manifestCode: s.pathParameters['code']!),
                routes: [
                  GoRoute(
                    path: 'packages/:packageCode',
                    builder: (c, s) => PackageCheckScreen(
                      manifestCode: s.pathParameters['code']!,
                      packageCode: s.pathParameters['packageCode']!,
                    ),
                  ),
                  GoRoute(
                    path: 'result',
                    builder: (c, s) => ReceivingResultScreen(
                      manifestCode: s.pathParameters['code']!,
                      // passed by context.push(..., extra: result)
                      initialResult: s.extra is ReceivingResult
                          ? s.extra as ReceivingResult
                          : null,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );

  ref.onDispose(router.dispose);
  return router;
});

/// Returns the page the user MUST be on, or null = "stay where you are".
/// Order matters – follows the backend sign-in stage.
String? _redirect(AuthState auth, String location) {
  String? only(String page) => location == page ? null : page;

  switch (auth.status) {
    case AuthStatus.unknown:
      return only(AppRoutes.splash);
    case AuthStatus.unauthenticated:
      return only(AppRoutes.login);
    case AuthStatus.changePassword:
      return only(AppRoutes.forceChangePassword);
    case AuthStatus.setupTwoFactor:
      return only(AppRoutes.forceTwoFactor);
    case AuthStatus.verifyTwoFactor:
      return only(AppRoutes.otp);
    case AuthStatus.authenticated:
      if (AppRoutes._authFlowPages.contains(location)) return AppRoutes.home;

      // Store pages: only ROLE-STORE-STAFF / ROLE-STORE-MANAGER
      // (UI guard only – backend also checks role + user's store)
      final isStorePage = location.startsWith(AppRoutes.storePrefix) ||
          location.startsWith(AppRoutes.receiving);
      if (isStorePage && !auth.isStoreUser) return AppRoutes.home;

      return null;
  }
}
