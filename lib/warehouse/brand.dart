import 'package:flutter/material.dart';

const brand = Color(0xFFF25D19);
const brandSoft = Color(0xFFFFF1E9);
const ink = Color(0xFF0F172A);
const muted = Color(0xFF64748B);
const canvas = Color(0xFFF8FAFC);

class BrandLogo extends StatelessWidget {
  final double size;
  const BrandLogo({super.key, this.size = 52});
  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    padding: EdgeInsets.all(size * .10),
    decoration: BoxDecoration(
      color: brandSoft,
      borderRadius: BorderRadius.circular(size * .26),
    ),
    child: Image.asset('assets/logo.png', semanticLabel: 'Toy Storage System'),
  );
}

ThemeData appTheme() => ThemeData(
  useMaterial3: true,
  scaffoldBackgroundColor: canvas,
  colorScheme: ColorScheme.fromSeed(seedColor: brand).copyWith(
    primary: brand,
    onPrimary: Colors.white,
    surface: Colors.white,
    onSurface: ink,
  ),
  appBarTheme: const AppBarTheme(
    backgroundColor: canvas,
    foregroundColor: ink,
    centerTitle: false,
    scrolledUnderElevation: 0,
  ),
  dividerTheme: const DividerThemeData(color: Color(0xFFE2E8F0), thickness: 1),
  inputDecorationTheme: InputDecorationTheme(
    filled: true,
    fillColor: canvas,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: Color(0xFFE2E8F0)),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: Color(0xFFE2E8F0)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: brand, width: 1.5),
    ),
  ),
  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      minimumSize: const Size(0, 52),
      textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
  outlinedButtonTheme: OutlinedButtonThemeData(
    style: OutlinedButton.styleFrom(
      minimumSize: const Size(0, 48),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
  navigationBarTheme: const NavigationBarThemeData(
    backgroundColor: Colors.white,
    indicatorColor: brandSoft,
  ),
  chipTheme: ChipThemeData(
    side: const BorderSide(color: Color(0xFFE2E8F0)),
    selectedColor: brandSoft,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
  ),
);
