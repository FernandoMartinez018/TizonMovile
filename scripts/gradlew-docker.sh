#!/usr/bin/env bash
# Ejecuta Gradle dentro de un contenedor con el Android SDK (no requiere instalarlo en el equipo).
# Uso: scripts/gradlew-docker.sh :composeApp:assembleDebug :composeApp:testDebugUnitTest
set -euo pipefail
cd "$(dirname "$0")/.."
src="$(pwd -W 2>/dev/null || pwd)"   # pwd -W da la ruta Windows en Git Bash
MSYS_NO_PATHCONV=1 exec docker run --rm \
  -v "$src:/work" -v tizon-gradle-cache:/root/.gradle -w /work \
  -e TIZON_KEYSTORE_PATH -e TIZON_KEYSTORE_PASSWORD -e TIZON_KEY_ALIAS -e TIZON_KEY_PASSWORD \
  ghcr.io/cirruslabs/android-sdk:36 ./gradlew --no-daemon "$@"
