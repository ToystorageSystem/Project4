import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/utils/number_formatter.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/scan_input.dart';
import '../../../store/presentation/providers/store_providers.dart';
import '../../domain/entities/cart_item.dart';
import '../providers/cart_notifier.dart';
import '../widgets/cart_item_tile.dart';
import '../widgets/checkout_dialog.dart';
import '../widgets/product_picker_sheet.dart';
import '../widgets/quantity_dialog.dart';

/// "New sale": add products (scan / type barcode / search) → checkout → receipt
class SaleScreen extends ConsumerStatefulWidget {
  const SaleScreen({super.key});

  @override
  ConsumerState<SaleScreen> createState() => _SaleScreenState();
}

class _SaleScreenState extends ConsumerState<SaleScreen> {
  bool _lookingUp = false; // barcode lookup running
  bool _checkingOut = false;

  bool get _busy => _lookingUp || _checkingOut;

  CartNotifier get _cart => ref.read(cartProvider.notifier);

  void _toast(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ));
  }

  // ---------- Add products ----------

  Future<void> _addByBarcode(String barcode) async {
    if (_busy) return;
    setState(() => _lookingUp = true);
    try {
      final product =
          await ref.read(getProductByBarcodeUseCaseProvider)(barcode: barcode);
      if (!mounted) return;
      _toast(_cart.add(product));
    } on ApiException catch (e) {
      if (mounted) _toast(e.isNotFound ? 'No product with barcode $barcode' : e.message);
    } finally {
      if (mounted) setState(() => _lookingUp = false);
    }
  }

  Future<void> _openPicker() async {
    await showProductPickerSheet(
      context,
      onPick: _cart.add,
      quantityInCart: (id) => ref.read(cartProvider).quantityOf(id),
    );
  }

  Future<void> _editQuantity(CartItem item) async {
    final value = await showQuantityDialog(
      context,
      title: item.name,
      current: item.quantity,
      max: item.available,
    );
    if (value != null) _cart.setQuantity(item.productId, value);
  }

  void _remove(CartItem item) => _cart.remove(item.productId);

  Future<void> _clearCart() async {
    final ok = await showConfirmDialog(
      context,
      title: 'Clear cart?',
      message: 'All products will be removed from this sale.',
      confirmLabel: 'Clear',
      destructive: true,
    );
    if (ok) _cart.clear();
  }

  // ---------- Checkout ----------

  Future<void> _checkout(CartState cart) async {
    final confirm = await showCheckoutDialog(
      context,
      lineCount: cart.items.length,
      totalQuantity: cart.totalQuantity,
      totalAmount: cart.totalAmount,
    );
    if (confirm == null || !mounted) return;

    setState(() => _checkingOut = true);
    try {
      final sale = await _cart.checkout(note: confirm.note);
      if (!mounted) return;
      _toast('Sale ${sale.code} saved');
      context.push(AppRoutes.saleDetail(sale.id), extra: sale);
    } on ApiException catch (e) {
      // e.g. "Not enough stock: TEST-TOY-007 (available 2)" → cart is kept
      if (mounted) _toast(e.message);
    } finally {
      if (mounted) setState(() => _checkingOut = false);
    }
  }

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final cart = ref.watch(cartProvider);

    return PopScope(
      // leaving with a non-empty cart → ask first (the cart is not saved)
      canPop: cart.isEmpty,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        final leave = await showConfirmDialog(
          context,
          title: 'Discard this sale?',
          message: 'The cart has not been paid and will be lost.',
          confirmLabel: 'Discard',
          destructive: true,
        );
        if (leave && context.mounted) {
          _cart.clear();
          context.pop();
        }
      },
      child: Scaffold(
        backgroundColor: AppColors.fieldFill,
        appBar: AppBar(
          title: const Text('New sale'),
          backgroundColor: Colors.white,
          surfaceTintColor: Colors.transparent,
          actions: [
            if (!cart.isEmpty)
              IconButton(
                tooltip: 'Clear cart',
                onPressed: _busy ? null : _clearCart,
                icon: const Icon(Icons.remove_shopping_cart_outlined),
              ),
            IconButton(
              tooltip: 'Sales history',
              onPressed: () => context.push(AppRoutes.saleHistory),
              icon: const Icon(Icons.history_rounded),
            ),
          ],
          bottom: _busy
              ? const PreferredSize(
                  preferredSize: Size.fromHeight(3),
                  child: LinearProgressIndicator(minHeight: 3),
                )
              : null,
        ),
        body: Column(
          children: [
            Container(
              color: Colors.white,
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
              child: Row(
                children: [
                  Expanded(
                    child: ScanInput(
                      hintText: 'Scan or type barcode',
                      scanTitle: 'Scan product barcode',
                      enabled: !_busy,
                      onSubmitted: _addByBarcode,
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filledTonal(
                    tooltip: 'Search product',
                    onPressed: _busy ? null : _openPicker,
                    icon: const Icon(Icons.search_rounded),
                  ),
                ],
              ),
            ),
            Expanded(
              child: cart.isEmpty
                  ? const _EmptyCart()
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      itemCount: cart.items.length + 1, // +1 header
                      separatorBuilder: (context, index) => const SizedBox(height: 10),
                      itemBuilder: (context, i) {
                        if (i == 0) {
                          return Text(
                              '${cart.items.length} products · ${cart.totalQuantity} pcs',
                              style: AppTextStyles.caption);
                        }
                        final item = cart.items[i - 1];
                        return CartItemTile(
                          item: item,
                          enabled: !_busy,
                          onIncrease: () =>
                              _cart.setQuantity(item.productId, item.quantity + 1),
                          onDecrease: () =>
                              _cart.setQuantity(item.productId, item.quantity - 1),
                          onEditQuantity: () => _editQuantity(item),
                          onRemove: () => _remove(item),
                        );
                      },
                    ),
            ),
          ],
        ),
        bottomNavigationBar: cart.isEmpty
            ? null
            : _CheckoutBar(
                totalAmount: cart.totalAmount,
                loading: _checkingOut,
                onCheckout: _busy ? null : () => _checkout(cart),
              ),
      ),
    );
  }
}

class _EmptyCart extends StatelessWidget {
  const _EmptyCart();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.shopping_cart_outlined, size: 56, color: AppColors.textGrey),
            SizedBox(height: 12),
            Text('The cart is empty', style: AppTextStyles.cardTitle),
            SizedBox(height: 4),
            Text(
              'Scan a barcode, type it and press Enter, or tap the search button.',
              textAlign: TextAlign.center,
              style: AppTextStyles.caption,
            ),
          ],
        ),
      ),
    );
  }
}

class _CheckoutBar extends StatelessWidget {
  final double totalAmount;
  final bool loading;
  final VoidCallback? onCheckout;

  const _CheckoutBar({
    required this.totalAmount,
    required this.loading,
    required this.onCheckout,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Total', style: AppTextStyles.caption),
                    Text(formatMoney(totalAmount),
                        style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w800,
                            color: AppColors.primary)),
                  ],
                ),
              ),
              SizedBox(
                width: 170,
                child: ElevatedButton(
                  onPressed: onCheckout,
                  child: loading ? const ButtonSpinner() : const Text('Checkout'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
