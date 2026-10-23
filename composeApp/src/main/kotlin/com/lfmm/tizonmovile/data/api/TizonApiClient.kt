package com.lfmm.tizonmovile.data.api

import com.lfmm.tizonmovile.BuildConfig
import com.lfmm.tizonmovile.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cliente de la API pública (contracts/tizon-api.yaml). Todas las llamadas devuelven
 * [Result]; los errores llegan como [AppError] (nunca como texto libre).
 */
class TizonApiClient(
    baseUrl: String,
    engine: HttpClientEngine = OkHttp.create(),
) {

    @Volatile
    private var jwtToken: String? = null

    private val http: HttpClient = HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) { json(JSON) }

        // Solo en debug, sin cuerpos (frames Base64) y ocultando el token (RNF-SEG-08).
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }

        // App → Spring: 15 s (contrato, regla 7).
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }

        // Se evalúa en cada petición: token vigente e identificador de correlación nuevo (RF-07 CA4).
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header("X-Request-Id", UUID.randomUUID().toString())
            jwtToken?.let { bearerAuth(it) }
        }
    }

    // ── Autenticación ─────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<LoginResponse> =
        call<LoginResponse> { post("auth/login") { setBody(LoginRequest(username, password)) } }
            .onSuccess { jwtToken = it.token }

    fun logout() {
        jwtToken = null
    }

    fun isAuthenticated(): Boolean = jwtToken != null

    suspend fun me(): Result<User> = call { get("auth/me") }

    suspend fun config(): Result<AppConfig> = call { get("config") }

    // ── Sesiones ──────────────────────────────────────────────────────────

    suspend fun startSession(request: StartSessionRequest = StartSessionRequest()): Result<SessionSummary> =
        call { post("sessions") { setBody(request) } }

    suspend fun sessions(page: Int = 0, size: Int = 20): Result<SessionPage> =
        call {
            get("sessions") {
                parameter("page", page)
                parameter("size", size)
            }
        }

    suspend fun session(sessionId: String): Result<SessionSummary> = call { get("sessions/$sessionId") }

    suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location? = null): Result<FrameAnalysis> =
        call { post("sessions/$sessionId/frames") { setBody(FrameRequest(frameB64, location)) } }

    suspend fun endSession(sessionId: String): Result<SessionSummary> = call { post("sessions/$sessionId/end") }

    fun close() = http.close()

    private suspend inline fun <reified T> call(crossinline request: suspend HttpClient.() -> HttpResponse): Result<T> =
        try {
            val response = http.request()
            if (response.status.isSuccess()) {
                Result.success(response.body<T>())
            } else {
                val body = runCatching { response.body<ApiErrorBody>() }.getOrNull()
                Result.failure(AppError.from(response.status.value, body))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(AppError.Network)   // incluye timeouts del cliente y conexión rechazada
        } catch (e: Exception) {
            Result.failure(AppError.Unknown(null, e.message))
        }

    companion object {
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
