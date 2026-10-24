package com.lfmm.tizonmovile.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Cada estado de pantalla se revisa en claro, oscuro y con fuente al 200 % (06-ux-ui §Entregables).
 * Roborazzi captura todos los @Preview de com.lfmm.tizonmovile.ui como PNG (docs/ux/).
 */
@Preview(name = "claro", showBackground = true, widthDp = 360, heightDp = 780, locale = "es")
@Preview(
    name = "oscuro", showBackground = true, widthDp = 360, heightDp = 780, locale = "es",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "fuente-200", showBackground = true, widthDp = 360, heightDp = 780, locale = "es", fontScale = 2f)
annotation class TizonPreviews
