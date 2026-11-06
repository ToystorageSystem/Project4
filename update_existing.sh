#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 scripts/configure_platforms.py
flutter pub get
dart format lib test
flutter analyze
flutter test
