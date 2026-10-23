package com.lfmm.tizonmovile.data.repository

import com.lfmm.tizonmovile.data.api.TizonApiClient
import com.lfmm.tizonmovile.data.model.*


/**
 * Repositorio que abstrae el origen de datos del análisis.
 *
 * Los ViewModels dependen de esta interfaz, no de TizonApiClient
 * directamente. Esto permite:
 *   - Tests unitarios con implementaciones fake.
 *   - Cambiar la fuente de datos sin tocar los ViewModels.
 */
interface AnalysisRepository {
    suspend fun login(username: String, password: String): Result<LoginResponse>
    fun logout()
    fun isAuthenticated(): Boolean
    suspend fun config(): Result<AppConfig>
    suspend fun startSession(request: StartSessionRequest = StartSessionRequest()): Result<SessionSummary>
    suspend fun endSession(sessionId: String): Result<SessionSummary>
    suspend fun getHistory(page: Int = 0, size: Int = 20): Result<SessionPage>
    suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location? = null): Result<FrameAnalysis>
}

/** Implementación real que delega en TizonApiClient. */
class AnalysisRepositoryImpl(
    private val apiClient: TizonApiClient
) : AnalysisRepository {

    override suspend fun login(username: String, password: String) =
        apiClient.login(username, password)

    override fun logout() = apiClient.logout()

    override fun isAuthenticated() = apiClient.isAuthenticated()

    override suspend fun config() = apiClient.config()

    override suspend fun startSession(request: StartSessionRequest) = apiClient.startSession(request)

    override suspend fun endSession(sessionId: String) = apiClient.endSession(sessionId)

    override suspend fun getHistory(page: Int, size: Int) = apiClient.sessions(page, size)

    override suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location?) =
        apiClient.analyzeFrame(sessionId, frameB64, location)
}
