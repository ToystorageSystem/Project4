import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'api.dart';
import 'app_config.dart';
import 'brand.dart';
import 'main.dart' show HomePage;

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
    final role = '${raw ?? ''}'
        .trim()
        .toUpperCase()
        .replaceAll('-', '_');

    return staffRoles.contains(role);
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
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Column(
              children: [
                const BrandLogo(size: 76),
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
                  'Kết nối công việc. Vận hành hiệu quả.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: muted, height: 1.5),
                ),
                const SizedBox(height: 28),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(24),
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
  Widget build(BuildContext context) =>
      text == null
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
      setState(() => error = 'Nhập email và mật khẩu.');
      return;
    }
    if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email.text.trim())) {
      setState(() => error = 'Email chưa đúng định dạng.');
      return;
    }
    if (AppConfig.apiBaseUrl.isEmpty) {
      setState(
        () =>
            error =
                'Ứng dụng chưa được cấu hình máy chủ. Vui lòng liên hệ quản trị viên.',
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
        setState(
          () => error = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.',
        );
      }
    };
    try {
      final user = await api.login(email.text.trim(), password.text);
      if (user['id'] == null)
        throw Exception('Máy chủ chưa trả về thông tin tài khoản hợp lệ.');
      api.accountScope = '${user['id']}';
      if (!mounted) return;
      TextInput.finishAutofillContext();
      password.clear();
      await Navigator.push(
        context,
        MaterialPageRoute<void>(
          builder:
              (_) =>
                  isWarehouseStaff(user)
                      ? HomePage(api: api, demo: false, user: user)
                      : RoleWorkspace(api: api, user: user),
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
            'Chào mừng trở lại',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: ink,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Đăng nhập để tiếp tục công việc của bạn.',
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
              labelText: 'Mật khẩu',
              prefixIcon: const Icon(Icons.lock_outline),
              suffixIcon: IconButton(
                tooltip: hidden ? 'Hiện mật khẩu' : 'Ẩn mật khẩu',
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
              onPressed:
                  busy
                      ? null
                      : () => Navigator.push(
                        context,
                        MaterialPageRoute<void>(
                          builder:
                              (_) => ForgotPasswordPage(
                                initialEmail: email.text.trim(),
                              ),
                        ),
                      ),
              child: const Text('Quên mật khẩu?'),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : enter,
              child:
                  busy
                      ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                      : const Text('Đăng nhập'),
            ),
          ),
        ],
      ),
    ),
  );
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
      setState(() => error = 'Vui lòng nhập email hợp lệ.');
      return;
    }
    if (sent &&
        (token.text.trim().isEmpty ||
            password.text.length < 12 ||
            password.text.length > 64 ||
            password.text != confirm.text)) {
      setState(
        () =>
            error =
                'Nhập mã trong email, mật khẩu từ 12–64 ký tự và xác nhận trùng khớp.',
      );
      return;
    }
    if (AppConfig.apiBaseUrl.isEmpty) {
      setState(() => error = 'Ứng dụng chưa được cấu hình máy chủ.');
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
              ? 'Mật khẩu đã cập nhật'
              : sent
              ? 'Đặt mật khẩu mới'
              : 'Quên mật khẩu',
          style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 10),
        Text(
          done
              ? 'Bạn có thể đăng nhập bằng mật khẩu mới.'
              : sent
              ? 'Nếu tài khoản tồn tại và đang hoạt động, mã khôi phục sẽ được gửi đến email. Sao chép toàn bộ mã vào ô dưới đây. Mã có hiệu lực 15 phút.'
              : 'Nhập email tài khoản để nhận mã khôi phục mật khẩu.',
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
                labelText: 'Mã khôi phục trong email',
                prefixIcon: Icon(Icons.key_outlined),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: password,
              enabled: !busy,
              obscureText: hidden,
              decoration: InputDecoration(
                labelText: 'Mật khẩu mới',
                helperText: 'Từ 12–64 ký tự',
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
                labelText: 'Xác nhận mật khẩu mới',
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
                    ? 'Đang xử lý…'
                    : sent
                    ? 'Cập nhật mật khẩu'
                    : 'Gửi mã khôi phục',
              ),
            ),
          ),
          if (sent)
            Center(
              child: TextButton(
                onPressed:
                    busy
                        ? null
                        : () => setState(() {
                          sent = false;
                          token.clear();
                          error = null;
                        }),
                child: const Text('Đổi email hoặc gửi lại mã'),
              ),
            ),
        ],
        Center(
          child: TextButton(
            onPressed: busy ? null : () => Navigator.pop(context),
            child: const Text('Quay lại đăng nhập'),
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
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Toy Storage System')),
    body: Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const BrandLogo(size: 72),
            const SizedBox(height: 24),
            Text(
              'Xin chào, ${user['name'] ?? 'bạn'}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            const Text(
              'Bạn đã đăng nhập thành công. Không gian làm việc cho vai trò của bạn sẽ được bổ sung trong phiên bản tiếp theo.',
              textAlign: TextAlign.center,
              style: TextStyle(color: muted, height: 1.6),
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
                        content: Text('Chưa đăng xuất được. Vui lòng thử lại.'),
                      ),
                    );
                }
              },
              child: const Text('Đăng xuất'),
            ),
          ],
        ),
      ),
    ),
  );
}
