package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.Region
import com.lfmm.tizonmovile.ui.theme.TizonColors

/**
 * Overlay que dibuja las bounding boxes de detección sobre el feed de cámara.
 *
 * Las coordenadas del servidor son relativas [0.0 – 1.0].
 * Se escalan al tamaño real del Canvas en tiempo de render.
 *
 * Colores por etiqueta:
 *   Sano           → Verde  (GreenPrimary)
 *   Tizon Temprano → Naranja (OrangeWarn)
 *   Tizon Tardio   → Rojo   (RedDanger)
 *
 * Estilo visual:
 *   - Rectángulo delgado (1.5px) con esquinas reforzadas (5px) — estilo "visor".
 *   - Label con fondo semitransparente superpuesto en la esquina superior.
 */
@Composable
fun BoundingBoxOverlay(
    modifier: Modifier = Modifier,
    boundingBoxes: List<Region>
) {
    if (boundingBoxes.isEmpty()) return

    // Capa 1: Canvas para los rectángulos y esquinas reforzadas
    Canvas(modifier = modifier) {
        val canvasW = size.width
        val canvasH = size.height

        boundingBoxes.forEach { box ->
            val color = colorForLabel(box.label)

            val left   = (box.x      * canvasW).toFloat()
            val top    = (box.y      * canvasH).toFloat()
            val width  = (box.width  * canvasW).toFloat()
            val height = (box.height * canvasH).toFloat()

            // Rectángulo principal (delgado)
            drawRect(
                color    = color,
                topLeft  = Offset(left, top),
                size     = Size(width, height),
                style    = Stroke(width = 1.5f)
            )

            // Esquinas reforzadas — estilo "visor táctico"
            val cornerLen = minOf(width, height) * 0.20f
            val sw = 4f

            // Superior izquierda
            drawLine(color, Offset(left, top), Offset(left + cornerLen, top), sw)
            drawLine(color, Offset(left, top), Offset(left, top + cornerLen), sw)
            // Superior derecha
            drawLine(color, Offset(left + width, top), Offset(left + width - cornerLen, top), sw)
            drawLine(color, Offset(left + width, top), Offset(left + width, top + cornerLen), sw)
            // Inferior izquierda
            drawLine(color, Offset(left, top + height), Offset(left + cornerLen, top + height), sw)
            drawLine(color, Offset(left, top + height), Offset(left, top + height - cornerLen), sw)
            // Inferior derecha
            drawLine(color, Offset(left + width, top + height), Offset(left + width - cornerLen, top + height), sw)
            drawLine(color, Offset(left + width, top + height), Offset(left + width, top + height - cornerLen), sw)
        }
    }

    // Capa 2: Labels Compose superpuestos (encima del Canvas)
    Box(modifier = modifier) {
        boundingBoxes.forEach { box ->
            val color = colorForLabel(box.label)

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val xDp = (box.x * maxWidth.value).dp
                val yDp = (box.y * maxHeight.value).dp - 24.dp

                Surface(
                    modifier = Modifier.offset(x = xDp, y = yDp.coerceAtLeast(0.dp)),
                    shape    = RoundedCornerShape(4.dp),
                    color    = color.copy(alpha = 0.88f)
                ) {
                    Text(
                        text       = "${labelShort(box.label)} ${(box.confidence * 100).toInt()}%",
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0A1510)
                    )
                }
            }
        }
    }
}

private fun colorForLabel(label: ClassId): Color = when (label) {
    ClassId.SANO         -> TizonColors.GreenPrimary
    ClassId.TIZON_TARDIO -> TizonColors.RedDanger
}

private fun labelShort(label: ClassId): String = when (label) {
    ClassId.SANO         -> "Sano"
    ClassId.TIZON_TARDIO -> "Tardío"
}
