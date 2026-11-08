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
import '../../../../core/widgets/info_banner.dart';
import '../../../../core/widgets/password_field.dart';
import '../providers/auth_notifier.dart';
import '../widgets/auth_header.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();

  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  void _clearError() {
    if (_error != null) setState(() => _error = null);
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      await ref.read(authProvider.notifier).login(
            _emailCtrl.text,
            _passwordCtrl.text,
          );
      // No navigation here: stage → go_router redirect picks the next screen
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _showForgotPassword() {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Forgot password?'),
        content: const Text(
          'Please contact ${AppStrings.supportContact}. '
          'You will receive a temporary password and will be asked to '
          'set a new one after signing in.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // e.g. "Your session has expired" set by AuthNotifier
    final notice = ref.watch(authProvider.select((s) => s.message));

    return Scaffold(
      body: SafeArea(
        top: false,
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: constraints.maxHeight),
              child: IntrinsicHeight(
                child: Column(
                  children: [
                    const AuthHeader(),
                    const SizedBox(height: 16),
                    const Text(
                      AppStrings.appName,
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textDark,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(AppStrings.tagline, style: AppTextStyles.subtitle),
                    const SizedBox(height: 28),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 24),
                      child: _buildForm(notice),
                    ),
                    const Spacer(),
                    const Padding(
                      padding: EdgeInsets.only(top: 24, bottom: 16),
                      child: Text(AppStrings.versionLabel,
                          style: AppTextStyles.caption),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildForm(String? notice) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (notice != null && _error == null) ...[
            InfoBanner(message: notice),
            const SizedBox(height: 16),
          ],
          const Text('Sign in', style: AppTextStyles.screenTitle),
          const SizedBox(height: 6),
          const Text('Enter your account details to continue.',
              style: AppTextStyles.subtitle),
          const SizedBox(height: 24),
          const FieldLabel('Email'),
          TextFormField(
            controller: _emailCtrl,
            enabled: !_loading,
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.next,
            autofillHints: const [AutofillHints.email],
            validator: Validators.email,
            onChanged: (_) => _clearError(),
            decoration: const InputDecoration(
              hintText: 'you@example.com',
              prefixIcon: Icon(Icons.mail_outline_rounded),
            ),
          ),
          const SizedBox(height: 18),
          const FieldLabel('Password'),
          PasswordField(
            controller: _passwordCtrl,
            hintText: 'Your password',
            enabled: !_loading,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.password],
            validator: Validators.loginPassword,
            onChanged: (_) => _clearError(),
            onFieldSubmitted: (_) => _submit(),
          ),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: _showForgotPassword,
              child: const Text(
                'Forgot password?',
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
              ),
            ),
          ),
          if (_error != null) ...[
            ErrorBanner(message: _error!),
            const SizedBox(height: 12),
          ],
          const SizedBox(height: 8),
          ElevatedButton(
            onPressed: _loading ? null : _submit,
            child: _loading
                ? const ButtonSpinner()
                : const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Continue'),
                      SizedBox(width: 8),
                      Icon(Icons.arrow_forward_rounded, size: 20),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}
