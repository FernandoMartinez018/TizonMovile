# Reglas de R8 para la APK release (F5-MOB-01).
# kotlinx-serialization, Ktor, Compose y WorkManager traen sus propias reglas; aquí solo va lo que se resuelve
# por reflexión en tiempo de ejecución y no se puede probar sin dispositivo.

# Ktor busca el serializador de cada respuesta por su KType (Companion/serializer() por reflexión).
-keep class com.lfmm.tizonmovile.data.model.** { *; }

# Rutas tipadas de Navigation Compose: navigate(ruta) busca el serializador por la clase.
-keep class com.lfmm.tizonmovile.ui.navigation.*Route { *; }
-keep class com.lfmm.tizonmovile.ui.navigation.*Tab { *; }

# Tink (security-crypto) referencia anotaciones de Error Prone que solo existen al compilar.
-dontwarn com.google.errorprone.annotations.**
