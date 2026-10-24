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

## Arquitectura
| Capa | Contenido |
|---|---|
| `data/api` | `TizonApiClient` (Ktor 3, contrato v2, `X-Request-Id` por petición) y `AppError` |
| `data/auth` | Token cifrado con EncryptedSharedPreferences (`EncryptedTokenStore`) y `SessionManager`: inicio automático si el token sigue vigente; cualquier 401 cierra la sesión y lleva al login con "Tu sesión expiró" |
| `data/location` | `AndroidLocationProvider` con `LocationManager` (GPS y red, sin Play Services): última posición si tiene menos de 2 min o una lectura nueva con espera máxima de 10 s |
| `data/repository` | Repositorios por funcionalidad (auth, escaneo, alertas, administración) |
| `ui/*` | Pantallas sin estado + ViewModels de AndroidX (Koin `koinViewModel`); Navigation Compose con rutas tipadas |
| `work` | `AlertsWorker` (WorkManager cada 15 min con sesión iniciada) y notificación local del canal "Alertas de cultivo" |

Flujo de escaneo: la sesión se crea al pulsar "Iniciar escaneo"; CameraX entrega un frame solo cuando el
ViewModel lo pide (uno a la vez), rotado según `rotationDegrees`, escalado a 640 px y en JPEG 80; el siguiente se
pide `frame_interval_ms` después de la respuesta. 502/504 y la falta de red se reintentan con espera de 2, 4 y 8 s.
Las cajas se mapean a la vista previa con `PreviewTransform` (FILL_CENTER).

Permisos: cámara (obligatoria; si se rechaza, la app ofrece abrir los ajustes sin cerrarse), ubicación y
notificaciones (opcionales). La sesión cifrada no se respalda (`allowBackup=false`).

## Sistema visual y previews
- Tema propio en `ui/theme/`: paleta verde y tierra (verde hoja, arena, ocre, musgo, terracota y óxido), claro y
  oscuro, todo texto ≥ WCAG AA. No se usan colores fijos fuera del tema. Los textos están en `res/values/strings.xml`.
- Cada pantalla tiene `@Preview` por estado (`@TizonPreviews`: claro, oscuro y fuente al 200 %). Para regenerar
  las capturas PNG sin emulador (Roborazzi + Robolectric):
  ```bash
  scripts/gradlew-docker.sh :composeApp:recordRoborazziDebug   # → composeApp/build/outputs/roborazzi/
  ```
  La copia para revisión está en `../docs/ux/` (índice en `docs/ux/README.md`).

## CI/CD (GitHub Actions)
`.github/workflows/ci.yml` corre en cada push y PR a `dev`, `qa` y `main`: APK debug (artefacto) + pruebas unitarias y gitleaks.
En cada push a `main` compila la APK *release* y la publica como artefacto con su `SHA256SUMS.txt`.
Configuración en GitHub (*Settings → Secrets and variables → Actions*):
- Variable `TIZON_API_BASE_URL` (p. ej. `https://<PUBLIC_HOST>/api/`). Sin ella, el job de release no corre.
- Secretos `TIZON_KEYSTORE_BASE64` (`base64 -w0 tizon-release.jks`), `TIZON_KEYSTORE_PASSWORD`, `TIZON_KEY_ALIAS` y `TIZON_KEY_PASSWORD`.

