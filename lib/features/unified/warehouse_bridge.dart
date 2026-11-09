import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_client.dart';
import '../../core/constants/api_constants.dart';
import '../auth/presentation/providers/auth_notifier.dart';
import '../fleet/gps_publisher.dart';
import '../../warehouse/api.dart';
import '../../warehouse/main.dart' show HomePage;
import '../../warehouse/driver.dart' show DriverHomePage;

final warehouseBridgeProvider = Provider<WarehouseApi>((ref) {
  final user = ref.watch(authProvider.select((state) => state.user));
  final api = WarehouseApi(
    ApiConstants.baseUrl,
    client: ref.read(dioProvider),
    sharedLogout: () async {
      await ref.read(gpsPublisherProvider).stop();
      await ref.read(authProvider.notifier).logout();
    },
  );
  api.accountScope = '${user?.id ?? 'anonymous'}';
  return api;
});

class WarehouseBridgeScreen extends ConsumerWidget {
  final bool driver;
  const WarehouseBridgeScreen({super.key, this.driver = false});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user;
    if (user == null) return const Scaffold();
    final json = <String, dynamic>{
      'id': user.id,
      'name': user.name,
      'email': user.email,
      'authorities': user.authorities,
    };
    final api = ref.watch(warehouseBridgeProvider);
    return driver
        ? DriverHomePage(api: api, user: json)
        : HomePage(api: api, demo: false, user: json);
  }
}
