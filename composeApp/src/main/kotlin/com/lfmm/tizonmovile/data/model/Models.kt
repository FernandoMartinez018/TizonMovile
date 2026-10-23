package com.lfmm.tizonmovile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════════════════════════════════
// AUTH
// ══════════════════════════════════════════════════════════════════════════

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val username: String,
    @SerialName("expiresIn") val expiresIn: Long
)

// ══════════════════════════════════════════════════════════════════════════
// ANÁLISIS DE FRAMES
// ══════════════════════════════════════════════════════════════════════════

@Serializable
data class FrameRequest(
    @SerialName("frame_b64") val frameB64: String,
    @SerialName("session_id") val sessionId: String? = null
)

/**
 * Bounding box con coordenadas relativas [0.0 – 1.0].
 *
 * Para convertir a píxeles del canvas:
 *   x_px = bbox.x * canvasWidth
 *   y_px = bbox.y * canvasHeight
 */
@Serializable
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String,
    val confidence: Float,
    val probabilities: Map<String, Float>
)

@Serializable
data class AnalysisResult(
    val label: String,
    val confidence: Float,
    val probabilities: Map<String, Float>,
    @SerialName("bounding_boxes") val boundingBoxes: List<BoundingBox> = emptyList(),
    @SerialName("is_vegetation")  val isVegetation: Boolean,
    @SerialName("frame_width")    val frameWidth: Int,
    @SerialName("frame_height")   val frameHeight: Int,
    @SerialName("session_id")     val sessionId: String? = null
) {
    val isHealthy: Boolean get() = label == "Sano"
    val isAffected: Boolean get() = label.contains("Tizon", ignoreCase = true)

    /** Confianza como porcentaje entero, ej: 92 */
    val confidencePct: Int get() = (confidence * 100).toInt()

    /** Porcentaje total de tizón (temprano + tardío) */
    val affectionPct: Float
        get() {
            val tardio   = probabilities["Tizon Tardio"]   ?: 0f
            val temprano = probabilities["Tizon Temprano"] ?: 0f
            return (tardio + temprano) * 100f
        }

    /** Color semántico para la UI según la etiqueta */
    val severityLevel: SeverityLevel
        get() = when {
            label == "Tizon Tardio"   -> SeverityLevel.CRITICAL
            label == "Tizon Temprano" -> SeverityLevel.WARNING
            label == "Sano"           -> SeverityLevel.HEALTHY
            else                      -> SeverityLevel.UNKNOWN
        }
}

enum class SeverityLevel { HEALTHY, WARNING, CRITICAL, UNKNOWN }

// ══════════════════════════════════════════════════════════════════════════
// SESIONES
// ══════════════════════════════════════════════════════════════════════════

@Serializable
data class SessionStartResponse(
    @SerialName("session_id") val sessionId: String,
    val status: String
)

@Serializable
data class SessionSummary(
    @SerialName("session_id")      val sessionId: String,
    @SerialName("total_frames")    val totalFrames: Int,
    @SerialName("affected_frames") val affectedFrames: Int,
    @SerialName("avg_confidence")  val avgConfidence: Float,
    @SerialName("dominant_label")  val dominantLabel: String?,
    val status: String,
    @SerialName("affection_pct")   val affectionPct: Float
)

// ══════════════════════════════════════════════════════════════════════════
// ESTADOS DE UI
// ══════════════════════════════════════════════════════════════════════════

/** Estado genérico para operaciones asíncronas */
sealed class UiState<out T> {
    data object Idle    : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class  Success<T>(val data: T) : UiState<T>()
    data class  Error(val message: String) : UiState<Nothing>()
}

/** Estado completo de la pantalla de cámara */
data class CameraUiState(
    val isActive: Boolean = false,
    val currentResult: AnalysisResult? = null,
    val sessionId: String? = null,
    val framesAnalyzed: Int = 0,
    val affectedFrames: Int = 0,
    val isAnalyzing: Boolean = false,
    val error: String? = null
) {
    val affectionPct: Float
        get() = if (framesAnalyzed > 0)
            (affectedFrames.toFloat() / framesAnalyzed) * 100f
        else 0f

    val severityLevel: SeverityLevel
        get() = when {
            affectionPct > 30f -> SeverityLevel.CRITICAL
            affectionPct > 10f -> SeverityLevel.WARNING
            affectionPct > 0f  -> SeverityLevel.HEALTHY
            else               -> SeverityLevel.UNKNOWN
        }
}