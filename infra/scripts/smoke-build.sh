#!/usr/bin/env bash
set -euo pipefail

./gradlew :shared:allTests :app:assembleDebug
(
  cd webApp
  npm ci
  npm test
  npm run build
)
