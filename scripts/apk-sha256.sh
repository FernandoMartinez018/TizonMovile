#!/usr/bin/env bash
# Verifica la firma de una APK y muestra el SHA-256 del certificado y del archivo (F5-MOB-01, 09-despliegue).
# Uso: scripts/apk-sha256.sh composeApp/build/outputs/apk/release/TizonMovile-1.1.0-release.apk
set -euo pipefail
cd "$(dirname "$0")/.."
apk="${1:?Uso: scripts/apk-sha256.sh <ruta de la APK dentro del repo>}"
[ -f "$apk" ] || { echo "No existe $apk (la ruta es relativa a la raíz de TizonMovile)" >&2; exit 1; }
src="$(pwd -W 2>/dev/null || pwd)"
MSYS_NO_PATHCONV=1 exec docker run --rm -v "$src:/work:ro" -w /work ghcr.io/cirruslabs/android-sdk:36 bash -c '
  set -euo pipefail
  apksigner="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/apksigner"
  "$apksigner" verify --print-certs "$1" | grep -E "certificate (DN|SHA-256 digest):"
  sha256sum "$1"' _ "$apk"
