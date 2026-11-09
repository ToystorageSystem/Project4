import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'api.dart';
import 'app_config.dart';
import 'brand.dart';
import 'main.dart' show HomePage;
import 'driver.dart' show DriverHomePage;

bool isWarehouseStaff(Json user) {
  final values = user['authorities'];
  if (values is! List) return false;

  const staffRoles = {
    'WAREHOUSE_STAFF',
    'ROLE_WAREHOUSE_STAFF',
    'ROLE_WH_STAFF',
    'ROLE_ROLE_WH_STAFF',
  };

  return values.any((value) {
    final raw = value is Map ? value['authority'] : value;
    final role = '${raw ?? ''}'.trim().toUpperCase().replaceAll('-', '_');

    return staffRoles.contains(role);
  });
}

bool isDriver(Json user) {
  final values = user['authorities'];
  if (values is! List) return false;
  const driverRoles = {
    'DRIVER',
    'ROLE_DRIVER',
    'ROLE_ROLE_DRIVER',
    'DELIVERY_STAFF',
    'ROLE_DELIVERY_STAFF',
    'ROLE_ROLE_DELIVERY_STAFF',
  };
  return values.any((value) {
    final raw = value is Map ? value['authority'] : value;
    final role = '${raw ?? ''}'.trim().toUpperCase().replaceAll('-', '_');
    return driverRoles.contains(role);
  });
}

class AuthShell extends StatelessWidget {
  final Widget child;
  const AuthShell({super.key, required this.child});
  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: const Color(0xFFFFF8F4),
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: EdgeInsets.fromLTRB(
            16,
            20,
            16,
            20 + MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Column(
              children: [
                const BrandLogo(size: 64),
                const SizedBox(height: 20),
                const Text(
                  'Toy Storage System',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w800,
                    color: ink,
                    letterSpacing: -.7,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Your work, all in one place.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: muted, height: 1.5),
                ),
                const SizedBox(height: 20),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0DF25D19),
                        blurRadius: 30,
                        offset: Offset(0, 12),
                      ),
                    ],
                  ),
                  child: child,
                ),
                const SizedBox(height: 24),
                const Text(
                  'TOY STORAGE SYSTEM',
                  style: TextStyle(
                    color: muted,
                    fontSize: 11,
                    letterSpacing: 2,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

class InlineError extends StatelessWidget {
  final String? text;
  const InlineError(this.text, {super.key});
  @override
  Widget build(BuildContext context) => text == null
      ? const SizedBox.shrink()
      : Container(
          width: double.infinity,
          margin: const EdgeInsets.only(bottom: 18),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: const Color(0xFFFEF2F2),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text(
            text!,
            style: const TextStyle(color: Color(0xFFB91C1C), height: 1.5),
          ),
        );
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginState();
}

class _LoginState extends State<LoginPage> {
  final email = TextEditingController(), password = TextEditingController();
  bool busy = false, hidden = true;
  String? error;
  @override
  void dispose() {
    email.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> enter() async {
    if (busy) return;
    if (email.text.trim().isEmpty || password.text.isEmpty) {
      setState(() => error = 'Enter your email and password.');
      return;
    }
    if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email.text.trim())) {
      setState(() => error = 'Enter a valid email address.');
      return;
    }
    if (AppConfig.apiBaseUrl.isEmpty) {
      setState(
        () =>
            error = 'The server is not configured. Contact your administrator.',
      );
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    final api = WarehouseApi(AppConfig.apiBaseUrl);
    api.onUnauthorized = () {
      if (mounted) {
        Navigator.of(context).popUntil((route) => route.isFirst);
        setState(() => error = 'Your session has expired. Sign in again.');
      }
    };
    try {
      final user = await api.login(email.text.trim(), password.text);
      if (user['id'] == null)
        throw Exception('The server did not return a valid account.');
      if (user['mustChangePassword'] is! bool ||
          user['requiresTwoFactor'] is! bool ||
          user['requiresTwoFactorSetup'] is! bool) {
        try {
          await api.logout();
        } catch (_) {
          await api.session?.clear();
        }
        throw Exception(
          'The server needs the account security update. Contact your administrator.',
        );
      }
      api.accountScope = '${user['id']}';
      if (!mounted) return;
      TextInput.finishAutofillContext();
      password.clear();
      await Navigator.push(
        context,
        MaterialPageRoute<void>(
          builder: (_) => SecurityCheckpoint(api: api, user: user),
        ),
      );
    } catch (e) {
      if (mounted)
        setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => AuthShell(
    child: AutofillGroup(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Welcome back',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: ink,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Sign in to continue your work.',
            style: TextStyle(color: muted, height: 1.5),
          ),
          const SizedBox(height: 24),
          InlineError(error),
          TextField(
            controller: email,
            enabled: !busy,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.username],
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(
              labelText: 'Email',
              hintText: 'name@company.com',
              prefixIcon: Icon(Icons.mail_outline),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: password,
            enabled: !busy,
            obscureText: hidden,
            autofillHints: const [AutofillHints.password],
            onSubmitted: (_) => enter(),
            decoration: InputDecoration(
              labelText: 'Password',
              prefixIcon: const Icon(Icons.lock_outline),
              suffixIcon: IconButton(
                tooltip: hidden ? 'Show password' : 'Hide password',
                onPressed: () => setState(() => hidden = !hidden),
                icon: Icon(
                  hidden
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                ),
              ),
            ),
          ),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: busy
                  ? null
                  : () => Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                        builder: (_) =>
                            ForgotPasswordPage(initialEmail: email.text.trim()),
                      ),
                    ),
              child: const Text('Forgot password?'),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : enter,
              child: busy
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Sign in'),
            ),
          ),
        ],
      ),
    ),
  );
}

/// The server blocks protected APIs until password rotation and TOTP setup
/// are complete. The app also prevents entering a role workspace early.
class SecurityCheckpoint extends StatefulWidget {
  final WarehouseApi api;
  final Json user;
  const SecurityCheckpoint({super.key, required this.api, required this.user});
  @override
  State<SecurityCheckpoint> createState() => _SecurityCheckpointState();
}

class _SecurityCheckpointState extends State<SecurityCheckpoint> {
  final code = TextEditingController();
  final current = TextEditingController();
  final next = TextEditingController();
  final confirm = TextEditingController();
  bool busy = false;
  String? error, secret;
  late bool verify = widget.user['requiresTwoFactor'] == true;
  late bool change = widget.user['mustChangePassword'] == true;
  late bool enroll = widget.user['requiresTwoFactorSetup'] == true;

  @override
  void initState() {
    super.initState();
    if (!verify && !change && enroll) loadSetup();
    if (!verify && !change && !enroll) {
      WidgetsBinding.instance.addPostFrameCallback((_) => openWorkspace());
    }
  }

  @override
  void dispose() {
    code.dispose();
    current.dispose();
    next.dispose();
    confirm.dispose();
    super.dispose();
  }

  Future<void> loadSetup() async {
    if (busy || secret != null) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final response =
          await widget.api.call('GET', '/api/auth/2fa/setup') as Map;
      if (!mounted) return;
      setState(() => secret = '${response['secret'] ?? ''}');
      if (secret!.isEmpty)
        throw Exception('The server did not provide a setup key.');
    } catch (e) {
      if (mounted)
        setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> submit() async {
    if (busy) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      if (verify) {
        final result = await widget.api.call('POST', '/api/auth/2fa/verify', {
          'code': code.text.trim(),
        }) as Map;
        if (!mounted) return;
        setState(() {
          verify = false;
          change = result['mustChangePassword'] == true;
          enroll = result['requiresTwoFactorSetup'] == true;
          code.clear();
        });
      } else if (change) {
        if (next.text.length < 12 ||
            next.text.length > 72 ||
            next.text != confirm.text) {
          throw Exception(
            'Use a new password of 6–72 characters and confirm it.',
          );
        }
        await widget.api.call('POST', '/api/auth/change-password', {
          'currentPassword': current.text,
          'newPassword': next.text,
        });
        if (!mounted) return;
        current.clear();
        next.clear();
        confirm.clear();
        setState(() => change = false);
      } else if (enroll) {
        await widget.api.call('POST', '/api/auth/2fa/setup/confirm', {
          'code': code.text.trim(),
        });
        if (!mounted) return;
        setState(() {
          enroll = false;
          code.clear();
        });
      }
      if (!mounted) return;
      if (!verify && !change && !enroll) openWorkspace();
    } catch (e) {
      if (mounted)
        setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) {
        setState(() => busy = false);
        if (!verify && !change && enroll && secret == null) loadSetup();
      }
    }
  }

  void openWorkspace() {
    if (!mounted) return;
    Navigator.pushReplacement(
      context,
      MaterialPageRoute<void>(
        builder: (_) => isWarehouseStaff(widget.user)
            ? HomePage(api: widget.api, demo: false, user: widget.user)
            : isDriver(widget.user)
            ? DriverHomePage(api: widget.api, user: widget.user)
            : RoleWorkspace(api: widget.api, user: widget.user),
      ),
    );
  }

  Future<void> cancel() async {
    try {
      await widget.api.logout();
    } catch (_) {
      await widget.api.session?.clear();
    }
    if (mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final title = verify
        ? 'Verify your identity'
        : change
        ? 'Change your temporary password'
        : 'Set up two-step verification';
    return PopScope(
      canPop: false,
      child: AuthShell(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              verify
                  ? 'Enter the six-digit code from your authenticator app.'
                  : change
                  ? 'Your administrator requires a new password before you continue.'
                  : 'Add this account to an authenticator app using the setup key, then enter its six-digit code. Keep this key private.',
            ),
            const SizedBox(height: 20),
            InlineError(error),
            if (change) ...[
              TextField(
                controller: current,
                enabled: !busy,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Current password',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: next,
                enabled: !busy,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'New password (6–72 characters)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: confirm,
                enabled: !busy,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Confirm new password',
                ),
              ),
            ] else ...[
              if (enroll) ...[
                SelectableText(
                  secret ??
                      (busy ? 'Loading setup key…' : 'Setup key unavailable'),
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
              ],
              TextField(
                controller: code,
                enabled: !busy,
                keyboardType: TextInputType.number,
                maxLength: 6,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: const InputDecoration(labelText: 'Six-digit code'),
              ),
            ],
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: busy || (enroll && secret == null) ? null : submit,
                child: Text(
                  busy
                      ? 'Please wait…'
                      : change
                      ? 'Change password'
                      : 'Continue',
                ),
              ),
            ),
            if (enroll && secret == null)
              TextButton(
                onPressed: busy ? null : loadSetup,
                child: const Text('Retry setup'),
              ),
            TextButton(
              onPressed: busy ? null : cancel,
              child: const Text('Sign out'),
            ),
          ],
        ),
      ),
    );
  }
}

class ForgotPasswordPage extends StatefulWidget {
  final String initialEmail;
  const ForgotPasswordPage({super.key, this.initialEmail = ''});
  @override
  State<ForgotPasswordPage> createState() => _ForgotState();
}

class _ForgotState extends State<ForgotPasswordPage> {
  late final email = TextEditingController(text: widget.initialEmail);
  final token = TextEditingController(),
      password = TextEditingController(),
      confirm = TextEditingController();
  bool sent = false, done = false, busy = false, hidden = true;
  String? error;
  @override
  void dispose() {
    email.dispose();
    token.dispose();
    password.dispose();
    confirm.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (busy) return;
    final address = email.text.trim();
    if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(address)) {
      setState(() => error = 'Enter a valid email address.');
      return;
    }
    if (sent &&
        (token.text.trim().isEmpty ||
            password.text.length < 6 ||
            password.text.length > 72 ||
            password.text != confirm.text)) {
      setState(
        () => error = 'Enter the email code and a matching new password of 6–72 characters.',
      );
      return;
    }
    if (AppConfig.apiBaseUrl.isEmpty) {
      setState(() => error = 'The server is not configured.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    final api = WarehouseApi(AppConfig.apiBaseUrl);
    try {
      await api.call(
        'POST',
        sent ? '/api/auth/reset-password' : '/api/auth/forgot-password',
        sent
            ? {
                'email': address,
                'token': token.text.trim(),
                'newPassword': password.text,
              }
            : {'email': address},
      );
      if (mounted)
        setState(() {
          if (sent) {
            done = true;
            password.clear();
            confirm.clear();
            token.clear();
          } else {
            sent = true;
          }
        });
    } catch (e) {
      if (mounted)
        setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      api.dio.close();
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => AuthShell(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          done ? Icons.check_circle_outline : Icons.lock_reset_rounded,
          size: 36,
          color: brand,
        ),
        const SizedBox(height: 16),
        Text(
          done
              ? 'Password updated'
              : sent
              ? 'Set a new password'
              : 'Forgot password',
          style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 10),
        Text(
          done
              ? 'You can now sign in with your new password.'
              : sent
              ? 'If your account is active, a recovery code has been sent by email. Enter the full code below. It expires in 15 minutes.'
              : 'Enter your account email to receive a recovery code.',
          style: const TextStyle(color: muted, height: 1.6),
        ),
        const SizedBox(height: 24),
        InlineError(error),
        if (!done) ...[
          TextField(
            controller: email,
            enabled: !busy && !sent,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'Email',
              prefixIcon: Icon(Icons.mail_outline),
            ),
          ),
          if (sent) ...[
            const SizedBox(height: 16),
            TextField(
              controller: token,
              enabled: !busy,
              autocorrect: false,
              decoration: const InputDecoration(
                labelText: 'Email recovery code',
                prefixIcon: Icon(Icons.key_outlined),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: password,
              enabled: !busy,
              obscureText: hidden,
              decoration: InputDecoration(
                labelText: 'New password',
                helperText: '6–72 characters',
                suffixIcon: IconButton(
                  onPressed: () => setState(() => hidden = !hidden),
                  icon: Icon(
                    hidden
                        ? Icons.visibility_outlined
                        : Icons.visibility_off_outlined,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: confirm,
              enabled: !busy,
              obscureText: hidden,
              decoration: const InputDecoration(
                labelText: 'Confirm new password',
              ),
            ),
          ],
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : submit,
              child: Text(
                busy
                    ? 'Working…'
                    : sent
                    ? 'Update password'
                    : 'Send recovery code',
              ),
            ),
          ),
          if (sent)
            Center(
              child: TextButton(
                onPressed: busy
                    ? null
                    : () => setState(() {
                        sent = false;
                        token.clear();
                        error = null;
                      }),
                child: const Text('Change email or resend code'),
              ),
            ),
        ],
        Center(
          child: TextButton(
            onPressed: busy ? null : () => Navigator.pop(context),
            child: const Text('Back to sign in'),
          ),
        ),
      ],
    ),
  );
}

/// Other roles never enter staff screens or fetch warehouse staff data.
/// Register their actual home screens here as they are implemented.
class RoleWorkspace extends StatelessWidget {
  final WarehouseApi api;
  final Json user;
  const RoleWorkspace({super.key, required this.api, required this.user});
  String get assignedRoles {
    final authorities = user['authorities'];
    if (authorities is! List) return 'No role information';
    final roles = authorities
        .map((entry) => entry is Map ? '${entry['authority'] ?? ''}' : '$entry')
        .where((entry) => entry.toUpperCase().startsWith('ROLE_'))
        .toList();
    return roles.isEmpty ? 'No account role found' : roles.join(', ');
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Toy Storage System')),
    body: Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Padding(
          padding: EdgeInsets.zero,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const BrandLogo(size: 72),
              const SizedBox(height: 24),
              Text(
                'Hello, ${user['name'] ?? 'there'}',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'You are signed in. A workspace for your role is not available yet.',
                textAlign: TextAlign.center,
                style: TextStyle(color: muted, height: 1.6),
              ),
              const SizedBox(height: 12),
              Text(
                'Server role: $assignedRoles',
                textAlign: TextAlign.center,
                style: const TextStyle(color: muted, fontSize: 13),
              ),
              const SizedBox(height: 24),
              OutlinedButton(
                onPressed: () async {
                  try {
                    await api.logout();
                    if (context.mounted) Navigator.pop(context);
                  } catch (_) {
                    if (context.mounted)
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text(
                            'Could not sign out. Please try again.',
                          ),
                        ),
                      );
                  }
                },
                child: const Text('Sign out'),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
