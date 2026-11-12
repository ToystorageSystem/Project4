import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/search_paged_list.dart';
import '../providers/manifest_list_provider.dart';
import '../widgets/manifest_card.dart';

/// Manifests (bảng kê) coming to my store
class ManifestListScreen extends ConsumerWidget {
  const ManifestListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final listAsync = ref.watch(manifestListProvider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Receive goods'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: listAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ErrorBanner(
                message: e is ApiException
                    ? e.message
                    : 'Could not load manifests.',
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () => ref.invalidate(manifestListProvider),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
        data: (manifests) => RefreshIndicator(
          onRefresh: () => ref.refresh(manifestListProvider.future),
          child: manifests.isEmpty
              ? ListView(
                  children: const [
                    SizedBox(height: 80),
                    Icon(
                      Icons.inbox_outlined,
                      size: 48,
                      color: AppColors.textGrey,
                    ),
                    SizedBox(height: 8),
                    Center(
                      child: Text(
                        'No incoming manifests',
                        style: TextStyle(color: AppColors.textGrey),
                      ),
                    ),
                  ],
                )
              : SearchPagedList(
                  items: manifests,
                  hint: 'Search manifest, shipment or warehouse…',
                  label: 'manifests',
                  searchValues: (m) => [
                    m.manifestCode,
                    m.shipmentCode,
                    m.fromWarehouseName,
                    m.receivingStatus,
                  ],
                  itemBuilder: (context, m) => ManifestCard(
                    manifest: m,
                    onTap: () =>
                        context.push(AppRoutes.manifest(m.manifestCode)),
                  ),
                ),
        ),
      ),
    );
  }
}
