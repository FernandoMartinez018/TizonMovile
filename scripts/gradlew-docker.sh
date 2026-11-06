#!/usr/bin/env bash
# Ejecuta Gradle dentro de un contenedor con el Android SDK (no requiere instalarlo en el equipo).
# Uso: scripts/gradlew-docker.sh :composeApp:assembleDebug :composeApp:testDebugUnitTest
set -euo pipefail
winpath() { (cd "$1" && (pwd -W 2>/dev/null || pwd)); }   # pwd -W da la ruta Windows en Git Bash

# Release firmada: el keystore (fuera del repo) se monta en solo lectura y Gradle lo lee en /run/keystore.jks.
keystore=()
if [ -n "${TIZON_KEYSTORE_PATH:-}" ]; then
  [ -f "$TIZON_KEYSTORE_PATH" ] || { echo "No existe el keystore TIZON_KEYSTORE_PATH=$TIZON_KEYSTORE_PATH" >&2; exit 1; }
  keystore=(-v "$(winpath "$(dirname "$TIZON_KEYSTORE_PATH")")/$(basename "$TIZON_KEYSTORE_PATH"):/run/keystore.jks:ro")
  export TIZON_KEYSTORE_PATH=/run/keystore.jks
fi

cd "$(dirname "$0")/.."
src="$(winpath .)"
MSYS_NO_PATHCONV=1 exec docker run --rm \
  -v "$src:/work" -v tizon-gradle-cache:/root/.gradle -v tizon-m2-cache:/root/.m2 -w /work \
  ${keystore[@]+"${keystore[@]}"} \
  -e TIZON_KEYSTORE_PATH -e TIZON_KEYSTORE_PASSWORD -e TIZON_KEY_ALIAS -e TIZON_KEY_PASSWORD \
  ghcr.io/cirruslabs/android-sdk:36 ./gradlew --no-daemon "$@"
