import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';

/// Cream curved header + white logo box (Login screen)
class AuthHeader extends StatelessWidget {
  const AuthHeader({super.key});

  @override
  Widget build(BuildContext context) {
    // Height of the status bar (time, battery...) so content is not hidden
    final topPad = MediaQuery.paddingOf(context).top;

    return SizedBox(
      height: 190 + topPad,
      child: Stack(
        alignment: Alignment.topCenter,
        children: [
          ClipPath(
            clipper: _CurveClipper(),
            child: Container(
              height: 150 + topPad,
              color: AppColors.primarySoft,
            ),
          ),
          Positioned(top: 80 + topPad, child: const AppLogoBox()),
        ],
      ),
    );
  }
}

/// White rounded box with the app icon (Login header, Splash)
class AppLogoBox extends StatelessWidget {
  const AppLogoBox({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 104,
      height: 104,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.18),
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: const Icon(
        Icons.inventory_2_rounded,
        size: 52,
        color: AppColors.primary,
      ),
    );
  }
}

/// Cuts the bottom of the cream box into a downward curve
class _CurveClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    return Path()
      ..lineTo(0, size.height * 0.55)
      ..quadraticBezierTo(
        size.width / 2,
        size.height * 1.15, // control point pulls curve down
        size.width,
        size.height * 0.55,
      )
      ..lineTo(size.width, 0)
      ..close();
  }

  @override
  bool shouldReclip(covariant CustomClipper<Path> oldClipper) => false;
}
