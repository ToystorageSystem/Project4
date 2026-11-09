import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../store/domain/entities/store_product.dart';
import '../../../store/presentation/providers/store_providers.dart';

/// Search a product by name/code to add to the cart.
/// [onPick] is called for every tapped product; the sheet stays open so
/// several products can be added. [quantityInCart] shows "in cart: n".
Future<void> showProductPickerSheet(
  BuildContext context, {
  required String Function(StoreProduct product) onPick,
  required int Function(int productId) quantityInCart,
}) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
    ),
    builder: (_) => FractionallySizedBox(
      heightFactor: 0.9,
      child: _ProductPicker(onPick: onPick, quantityInCart: quantityInCart),
    ),
  );
}

class _ProductPicker extends ConsumerStatefulWidget {
  final String Function(StoreProduct product) onPick;
  final int Function(int productId) quantityInCart;

  const _ProductPicker({required this.onPick, required this.quantityInCart});

  @override
  ConsumerState<_ProductPicker> createState() => _ProductPickerState();
}

class _ProductPickerState extends ConsumerState<_ProductPicker> {
  final _ctrl = TextEditingController();
  Timer? _debounce;
  bool _loading = false;
  String? _error;
  List<StoreProduct> _items = const [];
  int _requestId = 0; // ignore answers of older searches
  String? _lastMessage;

  @override
  void initState() {
    super.initState();
    _search('');
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _ctrl.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () => _search(value));
  }

  Future<void> _search(String keyword) async {
    final id = ++_requestId;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await ref.read(searchStoreProductsUseCaseProvider)(
        keyword: keyword,
        size: 30,
      );
      if (!mounted || id != _requestId) return;
      setState(() => _items = page.content);
    } on ApiException catch (e) {
      if (!mounted || id != _requestId) return;
      setState(() => _error = e.message);
    } finally {
      if (mounted && id == _requestId) setState(() => _loading = false);
    }
  }

  void _pick(StoreProduct p) {
    final message = widget.onPick(p);
    setState(() => _lastMessage = message); // also refreshes "in cart"
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Add products',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: AppColors.textDark,
                  ),
                ),
              ),
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Done'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _ctrl,
            autofocus: true,
            textInputAction: TextInputAction.search,
            onChanged: _onChanged,
            onSubmitted: (v) {
              _debounce?.cancel();
              _search(v);
            },
            decoration: const InputDecoration(
              hintText: 'Product name or code',
              prefixIcon: Icon(Icons.search_rounded),
            ),
          ),
          if (_lastMessage != null) ...[
            const SizedBox(height: 8),
            Text(
              _lastMessage!,
              style: const TextStyle(color: AppColors.success, fontSize: 13),
            ),
          ],
          if (_loading) const LinearProgressIndicator(minHeight: 2),
          const SizedBox(height: 8),
          if (_error != null) ErrorBanner(message: _error!),
          Expanded(
            child: _items.isEmpty && !_loading
                ? const Center(
                    child: Text(
                      'No products found',
                      style: TextStyle(color: AppColors.textGrey),
                    ),
                  )
                : ListView.separated(
                    itemCount: _items.length,
                    separatorBuilder: (context, index) =>
                        const Divider(height: 1),
                    itemBuilder: (context, i) {
                      final p = _items[i];
                      final inCart = widget.quantityInCart(p.productId);
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        enabled: !p.isOutOfStock,
                        title: Text(
                          p.name,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        subtitle: Text(
                          '${p.productCode} · Stock ${p.totalQuantity}'
                          '${inCart > 0 ? ' · In cart $inCart' : ''}',
                          style: AppTextStyles.caption,
                        ),
                        trailing: Text(
                          formatMoney(p.sellingPrice),
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        onTap: () => _pick(p),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
