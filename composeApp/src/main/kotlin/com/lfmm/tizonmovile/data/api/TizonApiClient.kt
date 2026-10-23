package com.lfmm.tizonmovile.data.api

import com.lfmm.tizonmovile.BuildConfig
import com.lfmm.tizonmovile.data.model.*
import com.lfmm.tizonmovile.network.provideHttpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class TizonApiClient(
    private val baseUrl: String
) {

    private var jwtToken: String? = null

    private val httpClient: HttpClient = provideHttpClient().config {

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        // Solo en debug, sin cuerpos (frames Base64) y ocultando el token (RNF-SEG-08).
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 35_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 35_000
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            jwtToken?.let { bearerAuth(it) }
        }
    }

    // 🔐 LOGIN
    suspend fun login(username: String, password: String): Result<LoginResponse> =
        runCatching {
            val response: LoginResponse = httpClient.post("auth/login") {
                setBody(LoginRequest(username, password))
            }.body()

            jwtToken = response.token
            response
        }

    fun logout() {
        jwtToken = null
    }

    fun isAuthenticated(): Boolean = jwtToken != null

    // 📡 SESIONES
    suspend fun startSession(): Result<SessionStartResponse> =
        runCatching {
            httpClient.post("analysis/sessions/start").body()
        }

    suspend fun endSession(sessionId: String): Result<SessionSummary> =
        runCatching {
            httpClient.post("analysis/sessions/$sessionId/end").body()
        }

    suspend fun getHistory(): Result<List<SessionSummary>> =
        runCatching {
            httpClient.get("analysis/sessions/history").body()
        }

    // 🧠 ANÁLISIS
    suspend fun analyzeFrame(
        frameB64: String,
        sessionId: String?
    ): Result<AnalysisResult> =
        runCatching {

            val response = httpClient.post("analysis/frame") {

                jwtToken?.let {
                    header("Authorization", "Bearer $it")
                }

                setBody(FrameRequest(frameB64, sessionId))
            }

            if (!response.status.isSuccess()) {
                val error = response.bodyAsText()
                throw Exception("Error ${response.status}: $error")
            }

            response.body<AnalysisResult>()
        }

    private fun HttpRequestBuilder.addAuth() {
        jwtToken?.let {
            header("Authorization", "Bearer $it")
        }
    }


    fun close() = httpClient.close()
}
