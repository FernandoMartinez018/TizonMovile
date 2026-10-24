package com.lfmm.tizonmovile.ui.preview

import com.lfmm.tizonmovile.data.model.*

/** Datos ficticios solo para los @Preview (no son datos reales de campo). */
object PreviewData {

    val agricultor = User("u-1", "ana.perez", "Ana Pérez", Role.AGRICULTOR, true, "2026-10-20T08:00:00-05:00")
    val tecnico = User("u-2", "carlos.tec", "Carlos Gómez", Role.TECNICO, true, "2026-10-20T08:00:00-05:00")
    val admin = User("u-3", "admin", "Administrador", Role.ADMIN, true, "2026-10-01T08:00:00-05:00")

    val thresholds = SeverityThresholds(warningPct = 10.0, criticalPct = 30.0)

    private val probsTizon = Probabilities(sano = 0.08, tizonTardio = 0.92)
    private val probsSano = Probabilities(sano = 0.94, tizonTardio = 0.06)

    val regions = listOf(
        Region(0.12, 0.30, 0.38, 0.30, ClassId.TIZON_TARDIO, 0.92, probsTizon),
        Region(0.56, 0.42, 0.30, 0.26, ClassId.SANO, 0.94, probsSano),
    )

    fun stats(analyzed: Int, affected: Int, severity: Severity) = SessionStats(
        totalFrames = analyzed + 2,
        analyzedFrames = analyzed,
        affectedFrames = affected,
        noVegetationFrames = 2,
        affectionPct = if (analyzed == 0) 0.0 else affected * 100.0 / analyzed,
        severity = severity,
        avgConfidence = 0.86,
        dominantLabel = if (affected * 2 >= analyzed) ClassId.TIZON_TARDIO else ClassId.SANO,
    )

    private fun session(
        id: String, started: String, field: String?, analyzed: Int, affected: Int, severity: Severity,
        status: SessionStatus = SessionStatus.COMPLETED, alertId: String? = null,
        recommendations: List<Recommendation> = emptyList(),
    ) = SessionSummary(
        sessionId = id, username = agricultor.username, status = status, startedAt = started,
        endedAt = if (status == SessionStatus.COMPLETED) started.replace("10:15", "10:27") else null,
        location = Location(4.71099, -74.07209, 12.0), fieldName = field, alertId = alertId,
        recommendations = recommendations,
        totalFrames = analyzed + 2, analyzedFrames = analyzed, affectedFrames = affected, noVegetationFrames = 2,
        affectionPct = if (analyzed == 0) 0.0 else affected * 100.0 / analyzed, severity = severity,
        avgConfidence = 0.86, dominantLabel = if (affected * 2 >= analyzed) ClassId.TIZON_TARDIO else ClassId.SANO,
    )

    val genericRecommendation = Recommendation(
        Severity.CRITICAL, "Consulta a un técnico agrónomo", "Texto genérico pendiente de validación agronómica (P-05)",
    )

    val criticalSummary = session("s-1", "2026-11-03T10:15:00-05:00", "Lote 3", 24, 11, Severity.CRITICAL,
        alertId = "a-1", recommendations = listOf(genericRecommendation))
    val healthySummary = session("s-2", "2026-11-02T10:15:00-05:00", "Lote 1", 18, 0, Severity.NONE)

    val history = listOf(
        criticalSummary,
        session("s-3", "2026-11-02T15:40:00-05:00", "Lote 2", 20, 3, Severity.WARNING),
        healthySummary,
        session("s-4", "2026-11-01T09:05:00-05:00", null, 12, 1, Severity.LOW),
    )

    val alerts = listOf(
        Alert("a-1", "s-1", agricultor.username, Severity.CRITICAL, 45.8, Location(4.71099, -74.07209, 12.0),
            "Lote 3", AlertStatus.OPEN, "2026-11-03T10:27:00-05:00"),
        Alert("a-2", "s-5", "jose.r", Severity.CRITICAL, 33.3, null, "Finca El Roble", AlertStatus.ACKNOWLEDGED,
            "2026-10-31T16:10:00-05:00", acknowledgedBy = tecnico.username),
    )

    val users = listOf(admin, tecnico, agricultor, agricultor.copy(id = "u-4", username = "jose.r", fullName = "José Ruiz", enabled = false))
}
