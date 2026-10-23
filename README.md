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

## Configuración
| Propiedad | Dónde | Descripción |
|---|---|---|
| `tizon.apiBaseUrl` | `-Ptizon.apiBaseUrl=...` o `local.properties` | URL base de la API, terminada en `/api/`. En *debug*, por defecto `http://10.0.2.2:8080/api/` (la API del PC visto desde el emulador). En *release* es **obligatoria y debe ser `https://`**: si falta, la compilación falla. |

- *Release* no permite tráfico HTTP (`network_security_config`). *Debug* solo permite HTTP hacia `10.0.2.2` y `localhost`.
- Los logs HTTP solo existen en *debug*, en nivel `INFO` y con el header `Authorization` oculto.

## Compilar y probar
```bash
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
# o, sin Android SDK local:
scripts/gradlew-docker.sh :composeApp:assembleDebug :composeApp:testDebugUnitTest
```
El APK queda en `composeApp/build/outputs/apk/debug/`.
