import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/validators.dart';
import '../../../../core/widgets/button_spinner.dart';
import '../../../../core/widgets/error_banner.dart';
import '../../../../core/widgets/field_label.dart';
import '../../../../core/widgets/password_field.dart';
import '../providers/auth_notifier.dart';

class ChangePasswordScreen extends ConsumerStatefulWidget {
  /// true  = first login, user MUST change password (no back button)
  /// false = user opened this screen from Home
  final bool forced;

  const ChangePasswordScreen({super.key, this.forced = false});

  @override
  ConsumerState<ChangePasswordScreen> createState() =>
      _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentCtrl = TextEditingController();
  final _newCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();

  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _currentCtrl.dispose();
    _newCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  // ---------- Live rules (shown as checklist) ----------

  bool get _lengthOk => Validators.isValidPasswordLength(_newCtrl.text);

  bool get _differentOk =>
      _newCtrl.text.isNotEmpty && _newCtrl.text != _currentCtrl.text;

  bool get _matchOk =>
      _confirmCtrl.text.isNotEmpty && _confirmCtrl.text == _newCtrl.text;

  bool get _canSubmit =>
      !_loading &&
      _currentCtrl.text.isNotEmpty &&
      _lengthOk &&
      _differentOk &&
      _matchOk;

  // Any typing → redraw checklist + hide old API error
  void _onChanged(String _) => setState(() => _error = null);

  // ---------- Actions ----------

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    // Capture before await (screen may be replaced/popped afterwards)
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final stillSignedIn = await ref
          .read(authProvider.notifier)
          .changePassword(_currentCtrl.text, _newCtrl.text);

      messenger.showSnackBar(const SnackBar(content: Text('Password changed')));

      // forced: the new stage (SETUP_2FA / AUTHENTICATED...) → router moves on
      // voluntary: go back to Home if the session is still alive
      if (!widget.forced && stillSignedIn && navigator.canPop()) {
        navigator.pop();
      }
    } on ApiException catch (e) {
      if (e.isUnauthorized) return; // session expired → Login shows message
      if (mounted)
        setState(() => _error = e.message); // wrong current password...
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  /// forced screen: give up and go back to Login
  void _signOut() => ref.read(authProvider.notifier).logout();

  // ---------- UI ----------

  @override
  Widget build(BuildContext context) {
    final email = ref.watch(authProvider.select((s) => s.user?.email)) ?? '';

    return PopScope(
      canPop: !widget.forced, // forced: Android back button does nothing
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
                        _buildTopBar(),
                        const SizedBox(height: 8),
                        const _KeyBadge(),
                        const SizedBox(height: 20),
                        Text(
                          widget.forced
                              ? 'Set a new password'
                              : 'Change password',
                          textAlign: TextAlign.center,
                          style: AppTextStyles.screenTitle,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          widget.forced
                              ? 'Your account is using a temporary password.\n'
                                    'Please set a new one to continue.'
                              : 'Enter your current password and choose a new one.',
                          textAlign: TextAlign.center,
                          style: AppTextStyles.subtitle,
                        ),
                        if (email.isNotEmpty) ...[
                          const SizedBox(height: 8),
                          Text(
                            email,
                            style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textDark,
                            ),
                          ),
                        ],
                        const SizedBox(height: 28),
                        _buildForm(),
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

  Widget _buildTopBar() {
    if (widget.forced) {
      // No way back – only sign out
      return Align(
        alignment: Alignment.centerRight,
        child: TextButton.icon(
          onPressed: _loading ? null : _signOut,
          icon: const Icon(Icons.logout_rounded, size: 20),
          label: const Text(
            'Sign out',
            style: TextStyle(fontWeight: FontWeight.w600),
          ),
        ),
      );
    }
    return Align(
      alignment: Alignment.centerLeft,
      child: IconButton(
        onPressed: _loading ? null : () => Navigator.of(context).maybePop(),
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
      ),
    );
  }

  Widget _buildForm() {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          FieldLabel(widget.forced ? 'Temporary password' : 'Current password'),
          PasswordField(
            controller: _currentCtrl,
            hintText: widget.forced
                ? 'Password from administrator'
                : 'Your current password',
            enabled: !_loading,
            autofillHints: const [AutofillHints.password],
            validator: (v) => Validators.required(
              v,
              message: 'Please enter your current password',
            ),
            onChanged: _onChanged,
          ),
          const SizedBox(height: 18),

          const FieldLabel('New password'),
          PasswordField(
            controller: _newCtrl,
            hintText: 'At least ${Validators.passwordMinLength} characters',
            enabled: !_loading,
            autofillHints: const [AutofillHints.newPassword],
            validator: (v) =>
                Validators.newPassword(v, currentPassword: _currentCtrl.text),
            onChanged: _onChanged,
          ),
          const SizedBox(height: 18),

          const FieldLabel('Confirm new password'),
          PasswordField(
            controller: _confirmCtrl,
            hintText: 'Re-enter new password',
            enabled: !_loading,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.newPassword],
            validator: (v) => Validators.confirmPassword(v, _newCtrl.text),
            onChanged: _onChanged,
            onFieldSubmitted: (_) {
              if (_canSubmit) _submit();
            },
          ),
          const SizedBox(height: 16),

          // ----- Live checklist -----
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.fieldFill,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _RuleRow(
                  ok: _lengthOk,
                  text:
                      '${Validators.passwordMinLength}–'
                      '${Validators.passwordMaxLength} characters',
                ),
                const SizedBox(height: 8),
                _RuleRow(
                  ok: _differentOk,
                  text: 'Different from current password',
                ),
                const SizedBox(height: 8),
                _RuleRow(ok: _matchOk, text: 'Both new passwords match'),
              ],
            ),
          ),

          if (_error != null) ...[
            const SizedBox(height: 16),
            ErrorBanner(message: _error!),
          ],
          const SizedBox(height: 24),

          ElevatedButton(
            onPressed: _canSubmit ? _submit : null,
            child: _loading
                ? const ButtonSpinner()
                : const Text('Update password'),
          ),
        ],
      ),
    );
  }
}

// ---------- Small widgets ----------

class _RuleRow extends StatelessWidget {
  final bool ok;
  final String text;

  const _RuleRow({required this.ok, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 200),
          child: Icon(
            ok ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
            key: ValueKey(ok), // tells AnimatedSwitcher the icon changed
            size: 18,
            color: ok ? AppColors.success : AppColors.textGrey,
          ),
        ),
        const SizedBox(width: 8),
        Text(
          text,
          style: TextStyle(
            fontSize: 14,
            color: ok ? AppColors.textDark : AppColors.textGrey,
          ),
        ),
      ],
    );
  }
}

class _KeyBadge extends StatelessWidget {
  const _KeyBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 96,
      height: 96,
      decoration: const BoxDecoration(
        color: AppColors.primarySoft,
        shape: BoxShape.circle,
      ),
      child: const Icon(
        Icons.lock_reset_rounded,
        size: 44,
        color: AppColors.primary,
      ),
    );
  }
}
