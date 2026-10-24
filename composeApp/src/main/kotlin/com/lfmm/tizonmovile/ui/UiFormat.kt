package com.lfmm.tizonmovile.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.Role
import com.lfmm.tizonmovile.data.model.Severity
import com.lfmm.tizonmovile.ui.theme.TizonTheme
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Traducción de identificadores técnicos a lo que ve el usuario. La severidad siempre se muestra
// con icono + texto + color (nunca solo color).

@StringRes
fun ClassId.labelRes(): Int = when (this) {
    ClassId.SANO -> R.string.label_sano
    ClassId.TIZON_TARDIO -> R.string.label_tizon_tardio
}

@StringRes
fun Severity.labelRes(): Int = when (this) {
    Severity.NONE -> R.string.severity_none
    Severity.LOW -> R.string.severity_low
    Severity.WARNING -> R.string.severity_warning
    Severity.CRITICAL -> R.string.severity_critical
}

fun Severity.icon(): ImageVector = when (this) {
    Severity.NONE -> Icons.Filled.CheckCircle
    Severity.LOW -> Icons.Filled.Info
    Severity.WARNING -> Icons.Filled.Warning
    Severity.CRITICAL -> Icons.Filled.Dangerous
}

@Composable
@ReadOnlyComposable
fun Severity.color(): Color = when (this) {
    Severity.NONE -> TizonTheme.severity.none
    Severity.LOW -> TizonTheme.severity.low
    Severity.WARNING -> TizonTheme.severity.warning
    Severity.CRITICAL -> TizonTheme.severity.critical
}

/** Color de una región o diagnóstico por clase: sano = verde, tizón = rojo. */
@Composable
@ReadOnlyComposable
fun ClassId.color(): Color = when (this) {
    ClassId.SANO -> TizonTheme.severity.none
    ClassId.TIZON_TARDIO -> TizonTheme.severity.critical
}

fun ClassId.icon(): ImageVector = when (this) {
    ClassId.SANO -> Icons.Filled.CheckCircle
    ClassId.TIZON_TARDIO -> Icons.Filled.Dangerous
}

/** Certeza en lenguaje simple: ≥ 0,85 Alta, ≥ 0,6 Media, el resto Baja (06-ux-ui). */
@StringRes
fun certaintyRes(confidence: Double): Int = when {
    confidence >= 0.85 -> R.string.certainty_high
    confidence >= 0.6 -> R.string.certainty_medium
    else -> R.string.certainty_low
}

@StringRes
fun Role.labelRes(): Int = when (this) {
    Role.AGRICULTOR -> R.string.role_agricultor
    Role.TECNICO -> R.string.role_tecnico
    Role.ADMIN -> R.string.role_admin
}

private val BOGOTA: ZoneId = ZoneId.of("America/Bogota")
private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.forLanguageTag("es-CO"))

/** "lun 3 nov, 10:15" en hora local de Colombia (RF-09 CA1). Devuelve el texto original si no es ISO-8601. */
fun formatShortDateTime(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(BOGOTA).format(SHORT_DATE).replace(".", "")
}.getOrDefault(iso)

/** Porcentaje con un decimal y coma decimal: 12,5. */
fun formatPct(pct: Double): String = String.format(Locale.forLanguageTag("es-CO"), "%.1f", pct)
