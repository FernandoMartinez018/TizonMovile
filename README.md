# TizonMovile

App Android (Kotlin + Jetpack Compose) del sistema Tizón Tardío. Escanea hojas de papa con la cámara,
envía los frames a la API (`TizonApiIntegration`) y muestra el diagnóstico en vivo: sano, tizón temprano o tizón
tardío. Si la hoja es de otra planta, avisa "No es una hoja de papa" y no muestra diagnóstico.

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

## APK *release* firmada
La firma se lee de variables de entorno (RNF-SEG-11, D-05). El keystore vive **fuera del repositorio** (`*.jks`
está en `.gitignore`). Sin las variables, `assembleRelease` genera una APK sin firmar. R8 reduce y ofusca el código
(`isMinifyEnabled`, `isShrinkResources`); las reglas propias están en `composeApp/proguard-rules.pro`.

| Variable | Contenido |
|---|---|
| `TIZON_KEYSTORE_PATH` | Ruta del keystore en el equipo. `scripts/gradlew-docker.sh` lo monta en solo lectura dentro del contenedor. |
| `TIZON_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `TIZON_KEY_ALIAS` | Alias de la llave |
| `TIZON_KEY_PASSWORD` | Contraseña de la llave (en PKCS12 es la misma del keystore) |

1. Crear el keystore **una sola vez** y respaldarlo fuera del repo. Si se pierde, las APK ya instaladas no se pueden
   actualizar con una versión nueva.
   ```bash
   keytool -genkeypair -keystore ~/tizon/tizon-release.jks -storetype PKCS12 -alias tizon -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Compilar con la URL HTTPS de la API. `read -rs` evita que la contraseña quede en el historial de la terminal.
   ```bash
   export TIZON_KEYSTORE_PATH=~/tizon/tizon-release.jks TIZON_KEY_ALIAS=tizon
   read -rsp "Contraseña del keystore: " TIZON_KEYSTORE_PASSWORD; echo
   export TIZON_KEYSTORE_PASSWORD TIZON_KEY_PASSWORD="$TIZON_KEYSTORE_PASSWORD"
   scripts/gradlew-docker.sh :composeApp:assembleRelease -Ptizon.apiBaseUrl=https://<PUBLIC_HOST>/api/
   ```
3. Verificar la firma y registrar los SHA-256 del entregable (09-despliegue): el del certificado se mantiene entre
   versiones y sirve para comprobar que la APK es auténtica; el del archivo identifica esa compilación.
   ```bash
   scripts/apk-sha256.sh composeApp/build/outputs/apk/release/TizonMovile-1.1.0-release.apk
   ```

Para instalarla, en el teléfono se habilita "Instalar apps desconocidas" para el explorador de archivos. Si antes
había una APK *debug*, hay que desinstalarla, porque la firma es distinta.

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

