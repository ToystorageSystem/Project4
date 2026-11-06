#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Pass a device name/ID as argument 1 if your simulator has another name.
flutter run -d "${1:-iPhone 17 Pro}" --dart-define="API_BASE_URL=${API_BASE_URL:-http://localhost:8080}"
