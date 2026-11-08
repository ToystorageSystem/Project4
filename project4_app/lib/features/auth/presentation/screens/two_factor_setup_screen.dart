import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/otp_input.dart';
import '../../domain/entities/two_factor_setup.dart';
import '../providers/auth_notifier.dart';
import '../providers/two_factor_setup_provider.dart';
import '../widgets/recovery_codes_dialog.dart';

/// Stage SETUP_2FA: every account must enable Google Authenticator.
/// No Back button – only Sign out.
class TwoFactorSetupScreen extends ConsumerStatefulWidget {
  const TwoFactorSetupScreen({super.key});

  @override
  ConsumerState<TwoFactorSetupScreen> createState() =>
      _TwoFactorSetupScreenState();
}

class _TwoFactorSetupScreenState extends ConsumerState<TwoFactorSetupScreen> {
  final _codeCtrl = TextEditingController();
  final _focusNode = FocusNode();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _codeCtrl.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  // ---------- Actions ----------

  Future<void> _confirm() async {
    final code = _codeCtrl.text;
    if (code.length != OtpInput.length || _loading) return;

    FocusScope.of(context).unfocus();
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final notifier = ref.read(authProvider.notifier);
      final session = await notifier.confirmTwoFactor(code);

      // Show recovery codes BEFORE leaving this screen (shown only once)
      if (session.recoveryCodes.isNotEmpty && mounted) {
        await showRecoveryCodesDialog(context, session.recoveryCodes);
      }
      notifier.applySession(session); // → router goes to Home
    } on ApiException catch (e) {
      if (e.isUnauthorized || !mounted) return; // expired → Login
      setState(() => _error = e.message); // 400 wrong code
      _codeCtrl.clear();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _copySecret(String secret) async {
    await Clipboard.setData(ClipboardData(text: secret));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Setup key copied')),
    );
  }

  void _signOut() => ref.read(authProvider.notifier).logout();

  /// "JBSWY3DPEHPK3PXP" → "JBSW Y3DP EHPK 3PXP"
  static String _groupSecret(String s) =>
      s.replaceAllMapped(RegExp(r'.{4}'), (m) => '${m[0]} ').trim();

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final setupAsync = ref.watch(twoFactorSetupProvider);

    return PopScope(
      canPop: false, // Android back button does nothing
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Set up two-step verification'),
          backgroundColor: Colors.white,
          surfaceTintColor: Colors.transparent,
          actions: [
            TextButton.icon(
              onPressed: _loading ? null : _signOut,
              icon: const Icon(Icons.logout_rounded, size: 20),
              label: const Text('Sign out',
                  style: TextStyle(fontWeight: FontWeight.w600)),
            ),
            const SizedBox(width: 4),
          ],
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
            child: setupAsync.when(
              loading: () => const Padding(
                padding: EdgeInsets.only(top: 160),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (e, _) => _buildLoadError(e),
              data: _buildContent,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLoadError(Object e) {
    final message = e is ApiException ? e.message : 'Could not load setup data.';
    return Padding(
      padding: const EdgeInsets.only(top: 80),
      child: Column(
        children: [
          ErrorBanner(message: message),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () => ref.invalidate(twoFactorSetupProvider),
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Try again'),
          ),
        ],
      ),
    );
  }

  Widget _buildContent(TwoFactorSetup setup) {
    final complete = _codeCtrl.text.length == OtpInput.length;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const InfoBanner(
          message: 'Your organization requires two-step verification '
              'for every account. Set it up now to continue.',
        ),
        const SizedBox(height: 24),
        const _StepTile(
          number: 1,
          title: 'Install Google Authenticator',
          description: 'Download it from Google Play or the App Store.',
        ),
        const SizedBox(height: 20),
        const _StepTile(
          number: 2,
          title: 'Scan this QR code',
          description: 'In the app, tap + then "Scan a QR code".',
        ),
        const SizedBox(height: 16),
        Center(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: AppColors.border),
            ),
            child: QrImageView(
              data: setup.otpAuthUri,
              version: QrVersions.auto,
              size: 200,
              backgroundColor: Colors.white,
            ),
          ),
        ),
        const SizedBox(height: 16),
        if (setup.secret.isNotEmpty) ...[
          const Text(
            "Can't scan? Enter this setup key manually:",
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: AppColors.textGrey),
          ),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.only(left: 16),
            decoration: BoxDecoration(
              color: AppColors.fieldFill,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              children: [
                Expanded(
                  child: SelectableText(
                    _groupSecret(setup.secret),
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 1.2,
                      color: AppColors.textDark,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'Copy',
                  onPressed: () => _copySecret(setup.secret),
                  icon: const Icon(Icons.copy_rounded, color: AppColors.primary),
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),
        ],
        const _StepTile(
          number: 3,
          title: 'Enter the 6-digit code',
          description: 'Type the code shown in the app to finish setup.',
        ),
        const SizedBox(height: 16),
        OtpInput(
          controller: _codeCtrl,
          focusNode: _focusNode,
          autofocus: false, // don't cover the QR with the keyboard
          enabled: !_loading,
          hasError: _error != null,
          onChanged: (_) => setState(() => _error = null),
          onCompleted: (_) => _confirm(),
        ),
        if (_error != null) ...[
          const SizedBox(height: 16),
          ErrorBanner(message: _error!),
        ],
        const SizedBox(height: 24),
        ElevatedButton(
          onPressed: (_loading || !complete) ? null : _confirm,
          child: _loading
              ? const ButtonSpinner()
              : const Text('Turn on two-step verification'),
        ),
      ],
    );
  }
}

class _StepTile extends StatelessWidget {
  final int number;
  final String title;
  final String description;

  const _StepTile({
    required this.number,
    required this.title,
    required this.description,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          radius: 14,
          backgroundColor: AppColors.primarySoft,
          child: Text(
            '$number',
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: AppColors.primary,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: AppTextStyles.cardTitle),
              const SizedBox(height: 2),
              Text(
                description,
                style: const TextStyle(fontSize: 14, color: AppColors.textGrey),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
