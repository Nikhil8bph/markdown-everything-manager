#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir/frontend"
npm run build
cd "$root_dir"
mkdir -p backend/src/main/resources/static
cp -R frontend/dist/app/browser/. backend/src/main/resources/static/
printf 'Packaged Angular assets into backend/src/main/resources/static\n'
