#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v flutter >/dev/null || { echo 'Cài Flutter SDK và thêm flutter vào PATH trước.'; exit 1; }
command -v python3 >/dev/null || { echo 'Cần Python 3 để cấu hình quyền nền tảng.'; exit 1; }
# Preserve authored sources if flutter create scaffolds defaults.
backup_dir=$(mktemp -d)
cp -R lib "$backup_dir/lib"
cp -R test "$backup_dir/test"
cp pubspec.yaml "$backup_dir/pubspec.yaml"
cp analysis_options.yaml "$backup_dir/analysis_options.yaml"
restore_sources() {
 cp -R "$backup_dir/lib/." lib/
 cp -R "$backup_dir/test/." test/
 cp "$backup_dir/pubspec.yaml" pubspec.yaml
 cp "$backup_dir/analysis_options.yaml" analysis_options.yaml
}
trap restore_sources EXIT
flutter create --no-pub --platforms=android,ios,web --org com.toystorage --project-name toystorage_staff .
restore_sources
trap - EXIT
# flutter create adds its example test; keep only the supplied tests.
if [ -f test/widget_test.dart ]; then rm test/widget_test.dart; fi
python3 scripts/configure_platforms.py
flutter pub get
dart format lib test
flutter analyze
flutter test
printf '\nSẵn sàng. Chạy: flutter run\n'
