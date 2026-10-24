package com.lfmm.tizonmovile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Tokens de 06-ux-ui §Sistema visual. Todo texto pasa WCAG AA (≥ 4,5:1) sobre su fondo.
// Ajustes frente a la tabla inicial: severityLow #9E7700 → #8A6800 y severityWarning #C25E00 → #A85200
// (4,1:1 y 4,3:1 no alcanzaban AA). No se usan colores fijos fuera de este archivo.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B30),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB5F1B0),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF7A5C2E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    background = Color(0xFFFAFAF5),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BC),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    tertiary = Color(0xFF38656A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF0),
    onTertiaryContainer = Color(0xFF002023),
    // Contenedores (tarjetas, barra inferior, diálogos): neutros verdosos; el texto de color pasa AA hasta #EEF0E8.
    surfaceDim = Color(0xFFDDDED7),
    surfaceBright = Color(0xFFFAFAF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8F4),
    surfaceContainer = Color(0xFFF4F5F0),
    surfaceContainerHigh = Color(0xFFF1F3EC),
    surfaceContainerHighest = Color(0xFFEEF0E8),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF1F1EA),
    inversePrimary = Color(0xFF8CD48A),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD48A),
    onPrimary = Color(0xFF0A3A0E),
    primaryContainer = Color(0xFF14521A),
    onPrimaryContainer = Color(0xFFB5F1B0),
    secondary = Color(0xFFE2C08D),
    onSecondary = Color(0xFF412D05),
    secondaryContainer = Color(0xFF5B4319),
    onSecondaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF1C1F1A),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483F),
    error = Color(0xFFFF8F85),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    tertiary = Color(0xFFA0CFD4),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1F4D52),
    onTertiaryContainer = Color(0xFFBCEBF0),
    surfaceDim = Color(0xFF121411),
    surfaceBright = Color(0xFF383A35),
    surfaceContainerLowest = Color(0xFF0C0F0B),
    surfaceContainerLow = Color(0xFF1A1C18),
    surfaceContainer = Color(0xFF1E201C),
    surfaceContainerHigh = Color(0xFF282B26),
    surfaceContainerHighest = Color(0xFF333531),
    inverseSurface = Color(0xFFE3E3DC),
    inverseOnSurface = Color(0xFF2F312D),
    inversePrimary = Color(0xFF2E6B30),
)

/** Colores de severidad (no existen en el ColorScheme de Material 3). */
@Immutable
data class SeverityColors(
    val none: Color,
    val low: Color,
    val warning: Color,
    val critical: Color,
    /** Texto sobre un fondo de color de severidad (chips rellenos, etiquetas de cajas). */
    val onSeverity: Color,
)

internal val LightSeverity = SeverityColors(
    none = Color(0xFF2E7D32),
    low = Color(0xFF8A6800),
    warning = Color(0xFFA85200),
    critical = Color(0xFFB3261E),
    onSeverity = Color(0xFFFFFFFF),
)

internal val DarkSeverity = SeverityColors(
    none = Color(0xFF7FCB83),
    low = Color(0xFFE3BC4E),
    warning = Color(0xFFF5A262),
    critical = Color(0xFFFF8F85),
    onSeverity = Color(0xFF121411),
)

/** Fondo de la vista previa de cámara cuando aún no hay imagen (no depende del tema). */
internal val CameraBackdrop = Color(0xFF101210)
internal val OnCameraBackdrop = Color(0xFFE3E3DC)
