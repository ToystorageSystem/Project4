import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/field_label.dart';
import '../../../../core/widgets/otp_input.dart';
import '../providers/auth_notifier.dart';
import '../widgets/lost_authenticator_dialog.dart';
import '../widgets/step_indicator.dart';

class OtpScreen extends ConsumerStatefulWidget {
  const OtpScreen({super.key});

  @override
  ConsumerState<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends ConsumerState<OtpScreen> {
  final _codeCtrl = TextEditingController();
  final _focusNode = FocusNode();
  final _recoveryCtrl = TextEditingController();
  bool _useRecoveryCode = false; // phone lost → type a saved recovery code

  Timer? _timer;
  int _secondsLeft = _calcSecondsLeft();
  bool _loading = false;
  String? _error;

  /// TOTP changes every 30s, counted from 1970-01-01 (same as Google Authenticator)
  static int _calcSecondsLeft() {
    final nowSeconds = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    return 30 - (nowSeconds % 30);
  }

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      setState(() => _secondsLeft = _calcSecondsLeft());
    });
  }

  @override
  void dispose() {
    _timer?.cancel(); // stop timer, otherwise setState after dispose → crash
    _codeCtrl.dispose();
    _focusNode.dispose();
    _recoveryCtrl.dispose();
    super.dispose();
  }

  // ---------- Actions ----------

  Future<void> _verify() async {
    final code = _useRecoveryCode ? _recoveryCtrl.text.trim() : _codeCtrl.text;
    if (_loading) return;
    if (_useRecoveryCode ? code.isEmpty : code.length != OtpInput.length)
      return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final notifier = ref.read(authProvider.notifier);
      if (_useRecoveryCode) {
        await notifier.recoverWithCode(code);
      } else {
        await notifier.verifyOtp(code);
      }
      // Success → AUTHENTICATED → router shows Home
    } on ApiException catch (e) {
      // 401 = pending sign-in expired → notifier already went back to Login
      // (message is shown there)
      if (e.isUnauthorized || !mounted) return;
      setState(() => _error = e.message); // 400 wrong code
      if (!_useRecoveryCode) {
        _codeCtrl.clear();
        _focusNode.requestFocus();
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _toggleRecovery() {
    setState(() {
      _useRecoveryCode = !_useRecoveryCode;
      _error = null;
    });
  }

  /// Give up this sign-in (also ends the pending session on the server)
  void _goBack() => ref.read(authProvider.notifier).logout();

  void _showContactIt() {
    showLostAuthenticatorDialog(
      context,
      email: ref.read(authProvider).user?.email,
    );
  }

  static String _initialsFromEmail(String email) {
    final name = email.split('@').first.replaceAll(RegExp(r'[^a-zA-Z]'), '');
    if (name.isEmpty) return '?';
    return name.substring(0, min(2, name.length)).toUpperCase();
  }

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final email = ref.watch(authProvider.select((s) => s.user?.email)) ?? '';

    // Android back button → go back to Login instead of closing the app
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && !_loading) _goBack();
      },
      child: Scaffold(
        body: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) => SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: IntrinsicHeight(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Column(
                      children: [
                        const SizedBox(height: 12),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: _buildBackButton(),
                        ),
                        const SizedBox(height: 12),
                        const _ShieldBadge(),
                        const SizedBox(height: 24),
                        const StepIndicator(
                          current: 2,
                          total: 2,
                          alignment: MainAxisAlignment.center,
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Two-step verification',
                          textAlign: TextAlign.center,
                          style: AppTextStyles.screenTitle,
                        ),
                        const SizedBox(height: 8),
                        const Text.rich(
                          TextSpan(
                            text: 'Enter the 6-digit code from your\n',
                            children: [
                              TextSpan(
                                text: 'Google Authenticator',
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textDark,
                                ),
                              ),
                              TextSpan(text: ' app.'),
                            ],
                          ),
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 16,
                            height: 1.5,
                            color: AppColors.textGrey,
                          ),
                        ),
                        const SizedBox(height: 16),
                        _buildEmailChip(email),
                        const SizedBox(height: 28),
                        if (_useRecoveryCode)
                          _buildRecoveryInput()
                        else ...[
                          OtpInput(
                            controller: _codeCtrl,
                            focusNode: _focusNode,
                            enabled: !_loading,
                            hasError: _error != null,
                            onChanged: (_) => setState(() => _error = null),
                            onCompleted: (_) => _verify(),
                          ),
                          const SizedBox(height: 16),
                          _buildCountdown(),
                        ],
                        const SizedBox(height: 8),
                        TextButton(
                          onPressed: _loading ? null : _toggleRecovery,
                          child: Text(
                            _useRecoveryCode
                                ? 'Use the code from Google Authenticator'
                                : 'Use a recovery code',
                            style: const TextStyle(fontWeight: FontWeight.w600),
                          ),
                        ),
                        if (_error != null) ...[
                          const SizedBox(height: 16),
                          ErrorBanner(message: _error!),
                        ],
                        const SizedBox(height: 24),
                        _buildVerifyButton(),
                        const SizedBox(height: 16),
                        _buildContactIt(),
                        const Spacer(),
                        const Padding(
                          padding: EdgeInsets.only(top: 24, bottom: 16),
                          child: Text(
                            AppStrings.versionLabel,
                            style: AppTextStyles.caption,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBackButton() {
    return IconButton(
      onPressed: _loading ? null : _goBack,
      icon: const Icon(Icons.chevron_left_rounded, size: 28),
      style: IconButton.styleFrom(
        fixedSize: const Size(52, 52),
        backgroundColor: Colors.white,
        foregroundColor: AppColors.textDark,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: AppColors.border),
        ),
      ),
    );
  }

  Widget _buildEmailChip(String email) {
    return Container(
      padding: const EdgeInsets.fromLTRB(6, 6, 14, 6),
      decoration: BoxDecoration(
        color: AppColors.fieldFill,
        borderRadius: BorderRadius.circular(30),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircleAvatar(
            radius: 15,
            backgroundColor: AppColors.primary,
            child: Text(
              _initialsFromEmail(email),
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              email,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: AppColors.textDark,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCountdown() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        SizedBox(
          width: 18,
          height: 18,
          child: CircularProgressIndicator(
            value: _secondsLeft / 30, // 1.0 → 0.0 as time runs out
            strokeWidth: 2.5,
            color: AppColors.primary,
            backgroundColor: AppColors.border,
          ),
        ),
        const SizedBox(width: 8),
        const Text(
          'New code in ',
          style: TextStyle(fontSize: 15, color: AppColors.textGrey),
        ),
        Text(
          '${_secondsLeft}s',
          style: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: AppColors.textDark,
          ),
        ),
      ],
    );
  }

  Widget _buildRecoveryInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const FieldLabel('Recovery code'),
        TextField(
          controller: _recoveryCtrl,
          enabled: !_loading,
          autofocus: true,
          textInputAction: TextInputAction.done,
          onChanged: (_) => setState(() => _error = null),
          onSubmitted: (_) => _verify(),
          decoration: const InputDecoration(
            hintText: 'One of the codes you saved',
            prefixIcon: Icon(Icons.key_rounded),
          ),
        ),
      ],
    );
  }

  Widget _buildVerifyButton() {
    final complete = _useRecoveryCode
        ? _recoveryCtrl.text.trim().isNotEmpty
        : _codeCtrl.text.length == OtpInput.length;
    return ElevatedButton(
      onPressed: (_loading || !complete) ? null : _verify,
      child: _loading ? const ButtonSpinner() : const Text('Verify & sign in'),
    );
  }

  Widget _buildContactIt() {
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        const Text(
          'Lost access to your authenticator? ',
          style: TextStyle(fontSize: 14, color: AppColors.textGrey),
        ),
        GestureDetector(
          onTap: _showContactIt,
          child: const Text(
            'Contact IT',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: AppColors.primary,
            ),
          ),
        ),
      ],
    );
  }
}

// ---------- Shield icon with dashed circle ----------

class _ShieldBadge extends StatelessWidget {
  const _ShieldBadge();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 140,
      height: 140,
      child: CustomPaint(
        painter: _DashedCirclePainter(
          AppColors.primary.withValues(alpha: 0.45),
        ),
        child: Center(
          child: Container(
            width: 108,
            height: 108,
            decoration: const BoxDecoration(
              color: AppColors.primarySoft,
              shape: BoxShape.circle,
            ),
            child: const Stack(
              alignment: Alignment.center,
              children: [
                Icon(Icons.shield_outlined, size: 48, color: AppColors.primary),
                Padding(
                  padding: EdgeInsets.only(top: 2),
                  child: Icon(
                    Icons.lock_rounded,
                    size: 18,
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DashedCirclePainter extends CustomPainter {
  final Color color;

  _DashedCirclePainter(this.color);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2;

    const dashCount = 48;
    const sweep = 2 * pi / dashCount; // angle of one dash + one gap
    final rect = Rect.fromCircle(
      center: size.center(Offset.zero),
      radius: size.width / 2 - 1,
    );

    for (var i = 0; i < dashCount; i++) {
      canvas.drawArc(rect, i * sweep, sweep * 0.5, false, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _DashedCirclePainter old) => old.color != color;
}
