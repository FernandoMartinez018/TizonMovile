package com.lfmm.tizonmovile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos de la API pública según contracts/tizon-api.yaml (JSON en snake_case).
// Los identificadores técnicos (SANO, TIZON_TARDIO…) se traducen a texto solo en la UI.

// ── Enumeraciones ─────────────────────────────────────────────────────────

@Serializable
enum class Role { AGRICULTOR, TECNICO, ADMIN }

@Serializable
enum class ClassId { SANO, TIZON_TARDIO }

@Serializable
enum class Severity { NONE, LOW, WARNING, CRITICAL }

@Serializable
enum class SessionStatus { ACTIVE, COMPLETED, CANCELLED }

@Serializable
enum class FrameStatus { DETECTED, NO_VEGETATION }

// ── Autenticación ─────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class User(
    val id: String,
    val username: String,
    @SerialName("full_name") val fullName: String,
    val role: Role,
    val enabled: Boolean,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: User,
)

// ── Configuración funcional (GET /config) ─────────────────────────────────

@Serializable
data class ClassConfig(val id: ClassId, val affected: Boolean)

@Serializable
data class SeverityThresholds(
    @SerialName("warning_pct") val warningPct: Double,
    @SerialName("critical_pct") val criticalPct: Double,
)

@Serializable
data class AppConfig(
    val classes: List<ClassConfig>,
    val severity: SeverityThresholds,
    @SerialName("alert_threshold_pct") val alertThresholdPct: Double,
    @SerialName("frame_interval_ms") val frameIntervalMs: Long,
    @SerialName("model_version") val modelVersion: String,
)

// ── Sesiones y frames ─────────────────────────────────────────────────────

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
)

@Serializable
data class StartSessionRequest(
    val location: Location? = null,
    @SerialName("field_name") val fieldName: String? = null,
)

@Serializable
data class FrameRequest(
    @SerialName("frame_b64") val frameB64: String,
    val location: Location? = null,
)

@Serializable
data class Probabilities(
    @SerialName("SANO") val sano: Double,
    @SerialName("TIZON_TARDIO") val tizonTardio: Double,
)

/** Región de hoja analizada; coordenadas relativas (0–1) al frame enviado, ya rotado. */
@Serializable
data class Region(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val label: ClassId,
    val confidence: Double,
    val probabilities: Probabilities,
)

@Serializable
data class SessionStats(
    @SerialName("total_frames") val totalFrames: Int,
    @SerialName("analyzed_frames") val analyzedFrames: Int,
    @SerialName("affected_frames") val affectedFrames: Int,
    @SerialName("no_vegetation_frames") val noVegetationFrames: Int,
    @SerialName("affection_pct") val affectionPct: Double,
    val severity: Severity,
    @SerialName("avg_confidence") val avgConfidence: Double? = null,
    @SerialName("dominant_label") val dominantLabel: ClassId? = null,
)

@Serializable
data class FrameAnalysis(
    @SerialName("frame_id") val frameId: String,
    @SerialName("session_id") val sessionId: String,
    val status: FrameStatus,
    val label: ClassId? = null,
    val confidence: Double? = null,
    val probabilities: Probabilities? = null,
    val regions: List<Region> = emptyList(),
    @SerialName("frame_width") val frameWidth: Int,
    @SerialName("frame_height") val frameHeight: Int,
    @SerialName("model_version") val modelVersion: String,
    @SerialName("latency_ms") val latencyMs: Long,
    @SerialName("session_stats") val sessionStats: SessionStats? = null,
)

@Serializable
data class Recommendation(val severity: Severity, val text: String, val source: String)

/** Contrato: SessionStats + datos de la sesión (allOf), por eso los campos van planos. */
@Serializable
data class SessionSummary(
    @SerialName("session_id") val sessionId: String,
    val username: String,
    val status: SessionStatus,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    val location: Location? = null,
    @SerialName("field_name") val fieldName: String? = null,
    @SerialName("alert_id") val alertId: String? = null,
    val recommendations: List<Recommendation> = emptyList(),
    @SerialName("total_frames") val totalFrames: Int,
    @SerialName("analyzed_frames") val analyzedFrames: Int,
    @SerialName("affected_frames") val affectedFrames: Int,
    @SerialName("no_vegetation_frames") val noVegetationFrames: Int,
    @SerialName("affection_pct") val affectionPct: Double,
    val severity: Severity,
    @SerialName("avg_confidence") val avgConfidence: Double? = null,
    @SerialName("dominant_label") val dominantLabel: ClassId? = null,
)

@Serializable
data class SessionPage(
    val items: List<SessionSummary>,
    val page: Int,
    val size: Int,
    val total: Long,
)

// ── Alertas ───────────────────────────────────────────────────────────────

@Serializable
enum class AlertStatus { OPEN, ACKNOWLEDGED }

@Serializable
data class Alert(
    @SerialName("alert_id") val alertId: String,
    @SerialName("session_id") val sessionId: String,
    val username: String,
    val severity: Severity,
    @SerialName("affection_pct") val affectionPct: Double,
    val location: Location? = null,
    @SerialName("field_name") val fieldName: String? = null,
    val status: AlertStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("acknowledged_by") val acknowledgedBy: String? = null,
)

// ── Errores ───────────────────────────────────────────────────────────────

/** Cuerpo Error del contrato. `message` viene en español, apto para mostrarse. */
@Serializable
data class ApiErrorBody(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: String,
    @SerialName("request_id") val requestId: String? = null,
    val fields: Map<String, String>? = null,
)

// ── Estados de UI (pantallas actuales; se rediseñan en F3) ────────────────

/** Estado genérico para operaciones asíncronas */
sealed class UiState<out T> {
    data object Idle    : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class  Success<T>(val data: T) : UiState<T>()
    data class  Error(val message: String) : UiState<Nothing>()
}

/** Estado de la pantalla de cámara. Las estadísticas vienen del backend (session_stats). */
data class CameraUiState(
    val isActive: Boolean = false,
    val currentResult: FrameAnalysis? = null,
    val sessionId: String? = null,
    val stats: SessionStats? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
) {
    val affectionPct: Float get() = stats?.affectionPct?.toFloat() ?: 0f
    val framesAnalyzed: Int get() = stats?.analyzedFrames ?: 0
}
