package com.lfmm.tizonmovile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Paleta verde y tierra (decisión del usuario, 23-oct): verde hoja, arena, ocre, musgo, terracota y
// óxido; sin rojos puros, rosados ni azules. Tokens base de 06-ux-ui; todo texto pasa WCAG AA (≥ 4,5:1)
// sobre su fondo, verificado por cálculo. No se usan colores fijos fuera de este archivo.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B30),            // verde hoja
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E5B6),
    onPrimaryContainer = Color(0xFF0B3A0E),
    secondary = Color(0xFF7A5C2E),          // tierra
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEDCC0), // arena
    onSecondaryContainer = Color(0xFF2B1A05),
    tertiary = Color(0xFF5B6B2E),           // musgo
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE6B8),
    onTertiaryContainer = Color(0xFF1A2000),
    background = Color(0xFFFAFAF5),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BC),
    error = Color(0xFF9C3B1F),              // terracota oscura
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DDD2),     // terracota clara
    onErrorContainer = Color(0xFF3D1406),
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
    primaryContainer = Color(0xFF1E4E20),
    onPrimaryContainer = Color(0xFFC2E5B6),
    secondary = Color(0xFFE2C08D),
    onSecondary = Color(0xFF412D05),
    secondaryContainer = Color(0xFF5B4319),
    onSecondaryContainer = Color(0xFFEEDCC0),
    tertiary = Color(0xFFC2CF94),
    onTertiary = Color(0xFF2D3400),
    tertiaryContainer = Color(0xFF434C18),
    onTertiaryContainer = Color(0xFFDDE6B8),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF1C1F1A),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483F),
    error = Color(0xFFF2A68A),
    onError = Color(0xFF4E1A0A),
    errorContainer = Color(0xFF6E2A14),
    onErrorContainer = Color(0xFFFFDBCD),
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

/**
 * Colores de severidad (no existen en el ColorScheme de Material 3): de verde a ocre, terracota y
 * óxido. Siempre van con icono y texto, así que el color nunca es la única señal.
 */
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
    none = Color(0xFF2D7231),      // verde
    low = Color(0xFF7A6500),       // ocre
    warning = Color(0xFFA04A16),   // terracota
    critical = Color(0xFF8E2A1C),  // óxido
    onSeverity = Color(0xFFFFFFFF),
)

internal val DarkSeverity = SeverityColors(
    none = Color(0xFF7FCB83),
    low = Color(0xFFD6BE52),
    warning = Color(0xFFEBA26F),
    critical = Color(0xFFE58A72),
    onSeverity = Color(0xFF121411),
)

/** Fondo de la vista previa de cámara cuando aún no hay imagen (no depende del tema). */
internal val CameraBackdrop = Color(0xFF101210)
internal val OnCameraBackdrop = Color(0xFFE3E3DC)
