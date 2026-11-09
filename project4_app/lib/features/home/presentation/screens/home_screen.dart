import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../auth/presentation/providers/auth_notifier.dart';
import '../../../shelves/presentation/providers/low_shelf_provider.dart';
import '../widgets/home_header.dart';
import '../widgets/info_card.dart';
import '../widgets/menu_tile.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  Future<void> _refresh(BuildContext context, WidgetRef ref) async {
    try {
      ref.invalidate(lowShelfProvider); // restock badge
      await ref.read(authProvider.notifier).refreshStatus();
    } on ApiException catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  Future<void> _confirmLogout(BuildContext context, WidgetRef ref) async {
    final ok = await showConfirmDialog(
      context,
      title: 'Sign out?',
      message: 'You will need to sign in again to use the app.',
      confirmLabel: 'Sign out',
    );
    if (ok) await ref.read(authProvider.notifier).logout();
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider.select((s) => s.user));

    // During logout, user becomes null for a moment before redirect runs
    if (user == null) return const Scaffold();

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      body: RefreshIndicator(
        onRefresh: () => _refresh(context, ref),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            HomeHeader(user: user, onLogout: () => _confirmLogout(context, ref)),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  InfoCard(user: user),

                  // ----- Store (store staff / manager only) -----
                  const SizedBox(height: 28),
                  const SectionHeader('Store'),
                  const SizedBox(height: 10),
                  if (user.isStoreUser) ...[
                    MenuTile(
                      icon: Icons.manage_search_rounded,
                      title: 'Product lookup',
                      subtitle: 'Search, scan barcode, move stock',
                      onTap: () => context.push(AppRoutes.storeProducts),
                    ),
                    const SizedBox(height: 10),
                    MenuTile(
                      icon: Icons.point_of_sale_rounded,
                      title: 'New sale',
                      subtitle: 'Scan products, checkout, receipt',
                      onTap: () => context.push(AppRoutes.sales),
                    ),
                    const SizedBox(height: 10),
                    MenuTile(
                      icon: Icons.receipt_long_outlined,
                      title: 'Sales history',
                      subtitle: 'Receipts of this store',
                      onTap: () => context.push(AppRoutes.saleHistory),
                    ),
                    const SizedBox(height: 10),
                    MenuTile(
                      icon: Icons.shelves,
                      title: 'Shelves',
                      subtitle: 'Products on each shelf',
                      onTap: () => context.push(AppRoutes.storeShelves),
                    ),
                    const SizedBox(height: 10),
                    MenuTile(
                      icon: Icons.inventory_outlined,
                      title: 'Restock shelves',
                      subtitle: 'Shelves at or below the minimum',
                      trailing: const _LowShelfBadge(),
                      onTap: () => context.push(AppRoutes.lowShelf),
                    ),
                    const SizedBox(height: 10),
                    MenuTile(
                      icon: Icons.local_shipping_outlined,
                      title: 'Receive goods',
                      subtitle: user.isStoreManager
                          ? 'Scan packages, count items, confirm into stock'
                          : 'Scan packages and count items',
                      onTap: () => context.push(AppRoutes.receiving),
                    ),
                  ] else
                    const InfoBanner(
                      message: 'Your account has no store role. '
                          'Ask your administrator for ROLE-STORE-STAFF '
                          'or ROLE-STORE-MANAGER.',
                    ),

                  // ----- Security -----
                  const SizedBox(height: 28),
                  const SectionHeader('Security'),
                  const SizedBox(height: 10),
                  MenuTile(
                    icon: Icons.lock_reset_rounded,
                    title: 'Change password',
                    subtitle: 'Update your sign-in password',
                    onTap: () => context.push(AppRoutes.changePassword),
                  ),

                  const SizedBox(height: 32),
                  const Center(
                    child: Text(AppStrings.versionLabel,
                        style: AppTextStyles.caption),
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

/// Red count of products to restock (nothing when 0 or on error)
class _LowShelfBadge extends ConsumerWidget {
  const _LowShelfBadge();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final count = ref.watch(lowShelfProvider).value?.length ?? 0;
    if (count == 0) {
      return const Icon(Icons.chevron_right_rounded, color: AppColors.textGrey);
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.error,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        '$count',
        style: const TextStyle(
            color: Colors.white, fontWeight: FontWeight.w800, fontSize: 13),
      ),
    );
  }
}
