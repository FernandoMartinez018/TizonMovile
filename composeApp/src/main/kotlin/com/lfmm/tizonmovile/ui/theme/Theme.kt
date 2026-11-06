package com.lfmm.tizonmovile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSeverityColors = staticCompositionLocalOf { LightSeverity }

/** Tema de la app. Por defecto sigue el modo claro/oscuro del sistema (06-ux-ui). */
@Composable
fun TizonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSeverityColors provides if (darkTheme) DarkSeverity else LightSeverity) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = TizonTypography,
            shapes = TizonShapes,
            content = content,
        )
    }
}

object TizonTheme {
    val severity: SeverityColors
        @Composable @ReadOnlyComposable get() = LocalSeverityColors.current
}
