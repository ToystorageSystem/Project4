#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v flutter >/dev/null || { echo 'Cần Flutter SDK trong PATH.'; exit 1; }
if [ ! -f web/index.html ]; then
 backup_dir=$(mktemp -d)
 cp -R lib "$backup_dir/lib"
 cp -R test "$backup_dir/test"
 cp pubspec.yaml analysis_options.yaml "$backup_dir/"
 restore_sources() {
  cp -R "$backup_dir/lib/." lib/
  cp -R "$backup_dir/test/." test/
  cp "$backup_dir/pubspec.yaml" "$backup_dir/analysis_options.yaml" .
 }
 trap restore_sources EXIT
 flutter create --no-pub --platforms=web --project-name toystorage_staff .
 restore_sources
 trap - EXIT
 if [ -f test/widget_test.dart ]; then rm test/widget_test.dart; fi
fi
flutter pub get
flutter run -d chrome --web-port=5174
