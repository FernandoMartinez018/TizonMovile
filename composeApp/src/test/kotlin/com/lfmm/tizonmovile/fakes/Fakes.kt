package com.lfmm.tizonmovile.fakes

import com.lfmm.tizonmovile.data.auth.StoredSession
import com.lfmm.tizonmovile.data.auth.TokenStore
import com.lfmm.tizonmovile.data.location.LocationProvider
import com.lfmm.tizonmovile.data.model.*
import com.lfmm.tizonmovile.data.repository.AuthRepository
import com.lfmm.tizonmovile.data.repository.ScanRepository
import com.lfmm.tizonmovile.data.settings.UserPreferences

// Dobles de prueba en memoria para los ViewModels (sin red ni Android).

val testUser = User("u-1", "ana.perez", "Ana Pérez", Role.AGRICULTOR, true)

fun appConfig(frameIntervalMs: Long = 700) = AppConfig(
    classes = listOf(ClassConfig(ClassId.SANO, false), ClassConfig(ClassId.TIZON_TARDIO, true)),
    severity = SeverityThresholds(10.0, 30.0), alertThresholdPct = 30.0,
    frameIntervalMs = frameIntervalMs, modelVersion = "test-v1",
)

fun summary(id: String, status: SessionStatus = SessionStatus.ACTIVE) = SessionSummary(
    sessionId = id, username = testUser.username, status = status, startedAt = "2026-11-03T10:15:00-05:00",
    totalFrames = 0, analyzedFrames = 0, affectedFrames = 0, noVegetationFrames = 0,
    affectionPct = 0.0, severity = Severity.NONE,
)

fun detected(label: ClassId = ClassId.TIZON_TARDIO, affected: Int = 1, analyzed: Int = 1) = FrameAnalysis(
    frameId = "f-$affected-$analyzed", sessionId = "s-1", status = FrameStatus.DETECTED, label = label, confidence = 0.9,
    probabilities = Probabilities(0.1, 0.9), regions = listOf(Region(0.1, 0.1, 0.5, 0.5, label, 0.9, Probabilities(0.1, 0.9))),
    frameWidth = 480, frameHeight = 640, modelVersion = "test-v1", latencyMs = 300,
    sessionStats = SessionStats(analyzed, analyzed, affected, 0, affected * 100.0 / analyzed, Severity.CRITICAL, 0.9, label),
)

val noVegetation = FrameAnalysis(
    frameId = "f-nv", sessionId = "s-1", status = FrameStatus.NO_VEGETATION, regions = emptyList(),
    frameWidth = 480, frameHeight = 640, modelVersion = "test-v1", latencyMs = 200,
    sessionStats = SessionStats(1, 0, 0, 1, 0.0, Severity.NONE),
)

class FakeScanRepository : ScanRepository {
    var configResult: Result<AppConfig> = Result.success(appConfig())
    var startResult: Result<SessionSummary> = Result.success(summary("s-1"))
    val frameResults = ArrayDeque<Result<FrameAnalysis>>()
    var endResult: Result<SessionSummary> = Result.success(summary("s-1", SessionStatus.COMPLETED))
    var historyPages: (Int) -> Result<SessionPage> = { Result.success(SessionPage(emptyList(), it, 20, 0)) }

    val startRequests = mutableListOf<StartSessionRequest>()
    var framesSent = 0
    var ended = 0
    var cancelled = 0
    val historyRequests = mutableListOf<Int>()

    override suspend fun config() = configResult
    override suspend fun startSession(request: StartSessionRequest): Result<SessionSummary> {
        startRequests += request
        return startResult
    }
    override suspend fun analyzeFrame(sessionId: String, frameB64: String, location: Location?): Result<FrameAnalysis> {
        framesSent++
        return frameResults.removeFirstOrNull() ?: Result.success(detected())
    }
    override suspend fun endSession(sessionId: String): Result<SessionSummary> {
        ended++
        return endResult
    }
    override suspend fun cancelSession(sessionId: String): Result<SessionSummary> {
        cancelled++
        return Result.success(summary(sessionId, SessionStatus.CANCELLED))
    }
    override suspend fun history(page: Int, size: Int): Result<SessionPage> {
        historyRequests += page
        return historyPages(page)
    }
    override suspend fun session(sessionId: String) = Result.success(summary(sessionId, SessionStatus.COMPLETED))
}

class FakeAuthRepository : AuthRepository {
    var result: Result<User> = Result.success(testUser)
    var logins = 0
    var registrations = 0
    override suspend fun login(username: String, password: String): Result<User> { logins++; return result }
    override suspend fun register(username: String, password: String, fullName: String): Result<User> { registrations++; return result }
    override fun logout() = Unit
}

class FakeLocationProvider(var location: Location? = Location(4.71, -74.07, 12.0)) : LocationProvider {
    override suspend fun current() = location
}

class InMemoryPreferences : UserPreferences {
    override var welcomeSeen = false
    override var lastFieldName: String? = null
    override var lastAlertsCheck: String? = null
}

class InMemoryTokenStore(var session: StoredSession? = null) : TokenStore {
    override fun load() = session
    override fun save(session: StoredSession) { this.session = session }
    override fun clear() { session = null }
}
