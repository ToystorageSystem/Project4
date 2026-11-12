import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/widgets/load_error_view.dart';
import '../../../../core/widgets/search_paged_list.dart';
import '../providers/sale_history_notifier.dart';
import '../widgets/sale_card.dart';

/// Sales of my store, newest first, infinite scroll
class SaleHistoryScreen extends ConsumerStatefulWidget {
  const SaleHistoryScreen({super.key});

  @override
  ConsumerState<SaleHistoryScreen> createState() => _SaleHistoryScreenState();
}

class _SaleHistoryScreenState extends ConsumerState<SaleHistoryScreen> {
  final _scrollCtrl = ScrollController();
  final _searchCtrl = TextEditingController();
  String _query = '';

  @override
  void initState() {
    super.initState();
    _scrollCtrl.addListener(() {
      if (_scrollCtrl.position.extentAfter < 300) {
        ref.read(saleHistoryProvider.notifier).loadMore();
      }
    });
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final historyAsync = ref.watch(saleHistoryProvider);

    return Scaffold(
      backgroundColor: AppColors.fieldFill,
      appBar: AppBar(
        title: const Text('Sales history'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: historyAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => LoadErrorView(
          error: e,
          fallbackMessage: 'Could not load sales.',
          onRetry: () => ref.invalidate(saleHistoryProvider),
        ),
        data: (data) => RefreshIndicator(
          onRefresh: () => ref.refresh(saleHistoryProvider.future),
          child: data.items.isEmpty
              ? ListView(
                  children: const [
                    SizedBox(height: 80),
                    Icon(
                      Icons.receipt_long_outlined,
                      size: 48,
                      color: AppColors.textGrey,
                    ),
                    SizedBox(height: 8),
                    Center(
                      child: Text(
                        'No sales yet',
                        style: TextStyle(color: AppColors.textGrey),
                      ),
                    ),
                  ],
                )
              : Builder(builder: (context) {
                  final sales = data.items
                      .where((s) => matchesSearch(_query, [
                            s.code,
                            s.createdByName,
                          ]))
                      .toList();
                  return ListView.separated(
                  controller: _scrollCtrl,
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                  itemCount: sales.length + 2, // header + footer
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: 10),
                  itemBuilder: (context, i) {
                    if (i == 0) {
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          ListSearchField(
                            controller: _searchCtrl,
                            hint: 'Search sale code or cashier…',
                            onChanged: (v) => setState(() => _query = v),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _query.trim().isEmpty
                                ? '${data.totalElements} sales'
                                : '${sales.length} of ${data.items.length} loaded sales match',
                            style: AppTextStyles.caption,
                          ),
                        ],
                      );
                    }
                    if (i == sales.length + 1) return _footer(data);
                    final sale = sales[i - 1];
                    return SaleCard(
                      sale: sale,
                      onTap: () => context.push(AppRoutes.saleDetail(sale.id)),
                    );
                  },
                );
                }),
        ),
      ),
    );
  }

  Widget _footer(SaleHistoryState data) {
    if (data.loadingMore) {
      return const Padding(
        padding: EdgeInsets.all(12),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (data.loadMoreError != null) {
      return Column(
        children: [
          Text(
            data.loadMoreError!,
            style: const TextStyle(color: AppColors.error),
          ),
          TextButton(
            onPressed: () => ref.read(saleHistoryProvider.notifier).loadMore(),
            child: const Text('Retry'),
          ),
        ],
      );
    }
    if (!data.hasMore) {
      return const Center(
        child: Text('End of list', style: AppTextStyles.caption),
      );
    }
    return const SizedBox(height: 24);
  }
}
