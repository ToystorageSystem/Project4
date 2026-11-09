import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/router/app_router.dart';
import '../../../auth/presentation/providers/auth_notifier.dart';
import '../../../fleet/gps_publisher.dart';
import '../widgets/home_header.dart';
import '../widgets/menu_tile.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user;
    if (user == null) return const Scaffold();
    Widget tile(IconData icon, String title, String path) => Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: MenuTile(
        icon: icon,
        title: title,
        subtitle: '',
        onTap: () => context.push(path),
      ),
    );
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => ref.read(authProvider.notifier).refreshStatus(),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            HomeHeader(
              user: user,
              onLogout: () async {
                final ok = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: const Text('Sign out?'),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('Cancel'),
                      ),
                      FilledButton(
                        onPressed: () => Navigator.pop(context, true),
                        child: const Text('Sign out'),
                      ),
                    ],
                  ),
                );
                if (ok == true) {
                  await ref.read(gpsPublisherProvider).stop();
                  await ref.read(authProvider.notifier).logout();
                }
              },
            ),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (user.isWarehouseStaff)
                    tile(
                      Icons.warehouse_outlined,
                      'Warehouse tasks',
                      AppRoutes.warehouse,
                    ),
                  if (user.isDriver)
                    tile(
                      Icons.local_shipping_outlined,
                      'My deliveries',
                      AppRoutes.driver,
                    ),
                  if (user.isDriver || user.canManageFleet)
                    tile(
                      Icons.route_outlined,
                      'Vehicles & routes',
                      AppRoutes.fleet,
                    ),
                  if (user.isStoreUser) ...[
                    tile(
                      Icons.manage_search_rounded,
                      'Products & stock',
                      AppRoutes.storeProducts,
                    ),
                    tile(
                      Icons.point_of_sale_rounded,
                      'New sale',
                      AppRoutes.sales,
                    ),
                    tile(
                      Icons.receipt_long_outlined,
                      'Sales history',
                      AppRoutes.saleHistory,
                    ),
                    tile(Icons.shelves, 'Shelves', AppRoutes.storeShelves),
                    tile(
                      Icons.inventory_outlined,
                      'Restock shelves',
                      AppRoutes.lowShelf,
                    ),
                    tile(
                      Icons.move_to_inbox_outlined,
                      'Receive goods',
                      AppRoutes.receiving,
                    ),
                  ],
                  tile(
                    Icons.lock_reset_rounded,
                    'Change password',
                    AppRoutes.changePassword,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
