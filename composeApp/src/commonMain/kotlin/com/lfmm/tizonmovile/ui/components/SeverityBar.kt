package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfmm.tizonmovile.ui.theme.TizonColors

/**
 * Barra de progreso de severidad con leyenda de niveles.
 *
 * Refleja los rangos de la pantalla de la imagen de referencia:
 *   0–5%    → Verde  (nivel bajo)
 *   6–15%   → Amarillo (nivel moderado)
 *   16–30%  → Naranja (nivel alto)
 *   >30%    → Rojo   (nivel crítico)
 */
@Composable
fun SeverityBar(
    modifier: Modifier = Modifier,
    affectionPct: Float
) {
    val barColor = TizonColors.severityColor(affectionPct)
    val progress = (affectionPct / 100f).coerceIn(0f, 1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // Barra de progreso
        LinearProgressIndicator(
            progress         = { progress },
            modifier         = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color            = barColor,
            trackColor       = TizonColors.BorderColor,
        )

        // Leyenda de niveles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                "0–5%"  to TizonColors.GreenPrimary,
                "6–15%" to TizonColors.YellowMid,
                "16–30%" to TizonColors.OrangeWarn,
                ">30%"  to TizonColors.RedDanger
            ).forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, CircleShape)
                    )
                    Text(
                        text     = label,
                        fontSize = 10.sp,
                        color    = TizonColors.TextMuted
                    )
                }
            }
        }
    }
}