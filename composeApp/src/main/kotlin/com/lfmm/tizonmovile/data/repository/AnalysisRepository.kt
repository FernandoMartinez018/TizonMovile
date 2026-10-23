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
 *   - Agregar caché local en el futuro.
 */
interface AnalysisRepository {
    suspend fun login(username: String, password: String): Result<LoginResponse>
    fun logout()
    fun isAuthenticated(): Boolean
    suspend fun startSession(): Result<SessionStartResponse>
    suspend fun endSession(sessionId: String): Result<SessionSummary>
    suspend fun getHistory(): Result<List<SessionSummary>>
    suspend fun analyzeFrame(frameB64: String, sessionId: String?): Result<AnalysisResult>
}

/** Implementación real que delega en TizonApiClient. */
class AnalysisRepositoryImpl(
    private val apiClient: TizonApiClient
) : AnalysisRepository {

    override suspend fun login(username: String, password: String) =
        apiClient.login(username, password)

    override fun logout() = apiClient.logout()

    override fun isAuthenticated() = apiClient.isAuthenticated()

    override suspend fun startSession() = apiClient.startSession()

    override suspend fun endSession(sessionId: String) = apiClient.endSession(sessionId)

    override suspend fun getHistory() = apiClient.getHistory()

    override suspend fun analyzeFrame(frameB64: String, sessionId: String?) =
        apiClient.analyzeFrame(frameB64, sessionId)
}