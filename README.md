# TizonMovile

App Android (Kotlin + Jetpack Compose) del sistema Tizón Tardío. Escanea hojas de papa con la cámara,
envía los frames a la API (`TizonApiIntegration`) y muestra el diagnóstico en vivo.

## Decisiones de proyecto
- **Solo Android** (decisión D-01). Se retiró Kotlin Multiplatform (iOS, JVM, `:server`, `:shared`):
  el módulo `:composeApp` es un módulo Android estándar con Jetpack Compose. Así se usan directamente
  las librerías de AndroidX (Navigation, ViewModel, CameraX, WorkManager) sin capas `expect/actual`.
- Todas las versiones están en `gradle/libs.versions.toml` (RNF-MAN-06).
- Sin Google Play Services ni Firebase: GPS con `LocationManager`, mapas con osmdroid y notificaciones locales.

## Requisitos
- JDK 17 o superior y Android SDK (platform 36, build-tools 36.0.0), **o**
- Docker: `scripts/gradlew-docker.sh` compila dentro de `ghcr.io/cirruslabs/android-sdk:36`, sin instalar el SDK.

## Compilar y probar
```bash
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
# o, sin Android SDK local:
scripts/gradlew-docker.sh :composeApp:assembleDebug :composeApp:testDebugUnitTest
```
El APK queda en `composeApp/build/outputs/apk/debug/`.
