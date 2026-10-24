package com.lfmm.tizonmovile.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de las pantallas v1. Se elimina cuando F3-MOB-02..04 reemplacen esas pantallas
// por las del sistema visual nuevo (Color.kt / Theme.kt).
@Deprecated("Pantallas v1: usar MaterialTheme.colorScheme y TizonTheme.severity")
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
