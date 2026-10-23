package com.lfmm.tizonmovile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════
// PALETA DE COLORES
// Inspirada en el diseño de la imagen: dark green agrícola
// ══════════════════════════════════════════════════════════════════════════

object TizonColors {
    // Fondos
    val BgDark        = Color(0xFF0F1A14)  // Fondo principal
    val BgCard        = Color(0xFF1A2820)  // Fondo de tarjetas
    val BgCardDark    = Color(0xFF111F17)  // Fondo de inputs
    val BorderColor   = Color(0xFF2A3D30)  // Bordes sutiles

    // Texto
    val TextPrimary   = Color(0xFFF0F4F1)  // Texto principal
    val TextMuted     = Color(0xFF8A9E90)  // Texto secundario

    // Semánticos
    val GreenPrimary  = Color(0xFF2ECC71)  // Sano / Activo / Éxito
    val GreenLight    = Color(0xFF27AE60)  // Verde oscuro
    val OrangeWarn    = Color(0xFFF39C12)  // Tizón Temprano / Advertencia
    val RedDanger     = Color(0xFFE74C3C)  // Tizón Tardío / Peligro
    val YellowMid     = Color(0xFFF1C40F)  // Nivel moderado

    // Colores por nivel de severidad
    fun severityColor(pct: Float): Color = when {
        pct > 30f -> RedDanger
        pct > 15f -> OrangeWarn
        pct > 5f  -> YellowMid
        else      -> GreenPrimary
    }
}

private val TizonColorScheme = darkColorScheme(
    primary          = TizonColors.GreenPrimary,
    onPrimary        = Color(0xFF0A1510),
    primaryContainer = TizonColors.BgCard,
    secondary        = TizonColors.GreenLight,
    background       = TizonColors.BgDark,
    surface          = TizonColors.BgCard,
    onBackground     = TizonColors.TextPrimary,
    onSurface        = TizonColors.TextPrimary,
    error            = TizonColors.RedDanger,
)

@Composable
fun TizonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TizonColorScheme,
        content     = content
    )
}