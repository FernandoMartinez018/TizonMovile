package com.lfmm.tizonmovile.data.repository

import com.lfmm.tizonmovile.data.api.TizonApiClient
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.model.*

// Repositorios por funcionalidad: interfaces pequeñas para que los ViewModels se prueben con fakes.

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun register(username: String, password: String, fullName: String): Result<User>
    fun logout()
}

interface ScanRepository {
    suspend fun config(): Result<AppConfig>
    suspend fun startSession(request: StartSessionRequest): Result<SessionSummary>
    suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location?): Result<FrameAnalysis>
    suspend fun endSession(sessionId: String): Result<SessionSummary>
    suspend fun cancelSession(sessionId: String): Result<SessionSummary>
    suspend fun history(page: Int, size: Int): Result<SessionPage>
    suspend fun session(sessionId: String): Result<SessionSummary>
}

interface AlertRepository {
    suspend fun alerts(status: AlertStatus? = null, since: String? = null): Result<List<Alert>>
    suspend fun acknowledge(alertId: String): Result<Alert>
}

interface AdminRepository {
    suspend fun users(): Result<List<User>>
    suspend fun update(userId: String, role: Role? = null, enabled: Boolean? = null): Result<User>
}

class ApiAuthRepository(private val api: TizonApiClient, private val session: SessionManager) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> =
        api.login(username.trim(), password).map { response ->
            session.onLogin(response)
            response.user
        }

    /** Tras registrarse, inicia sesión de inmediato (RF-01 CA1). */
    override suspend fun register(username: String, password: String, fullName: String): Result<User> =
        api.register(RegisterRequest(username.trim(), password, fullName.trim()))
            .fold(onSuccess = { login(username, password) }, onFailure = { Result.failure(it) })

    override fun logout() = session.logout()
}

class ApiScanRepository(private val api: TizonApiClient) : ScanRepository {

    /** La configuración cambia poco: se guarda en memoria después de la primera lectura. */
    @Volatile
    private var cachedConfig: AppConfig? = null

    override suspend fun config(): Result<AppConfig> =
        cachedConfig?.let { Result.success(it) } ?: api.config().onSuccess { cachedConfig = it }

    override suspend fun startSession(request: StartSessionRequest) = api.startSession(request)
    override suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location?) =
        api.analyzeFrame(sessionId, frameB64, location)
    override suspend fun endSession(sessionId: String) = api.endSession(sessionId)
    override suspend fun cancelSession(sessionId: String) = api.cancelSession(sessionId)
    override suspend fun history(page: Int, size: Int) = api.sessions(page, size)
    override suspend fun session(sessionId: String) = api.session(sessionId)
}

class ApiAlertRepository(private val api: TizonApiClient) : AlertRepository {
    override suspend fun alerts(status: AlertStatus?, since: String?) = api.alerts(status, since)
    override suspend fun acknowledge(alertId: String) = api.acknowledgeAlert(alertId)
}

class ApiAdminRepository(private val api: TizonApiClient) : AdminRepository {
    override suspend fun users() = api.users()
    override suspend fun update(userId: String, role: Role?, enabled: Boolean?) =
        api.updateUser(userId, UpdateUserRequest(role, enabled))
}
