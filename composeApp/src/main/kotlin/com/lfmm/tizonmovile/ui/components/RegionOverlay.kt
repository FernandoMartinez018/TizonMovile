package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.data.model.Region
import com.lfmm.tizonmovile.ui.color
import com.lfmm.tizonmovile.ui.labelRes
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

/**
 * Cajas de regiones de hoja: borde de 3 dp del color de la clase y etiqueta sobre fondo opaco.
 * [toView] convierte el rectángulo relativo (0–1) del frame a píxeles de la vista; por defecto se
 * estira al tamaño de la vista. F3-MOB-03 lo reemplaza por el mapeo con el recorte de FILL_CENTER.
 */
@Composable
fun RegionOverlay(
    regions: List<Region>,
    modifier: Modifier = Modifier,
    toView: (Region, Size) -> Rect = { r, size ->
        Rect(
            left = (r.x * size.width).toFloat(),
            top = (r.y * size.height).toFloat(),
            right = ((r.x + r.width) * size.width).toFloat(),
            bottom = ((r.y + r.height) * size.height).toFloat(),
        )
    },
) {
    if (regions.isEmpty()) return
    val strokePx = with(LocalDensity.current) { Spacing.regionStroke.toPx() }
    val colors = regions.map { it.label.color() }

    // Las cajas son decorativas para el lector de pantalla: el diagnóstico se lee en ResultSheet.
    BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val viewSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        Canvas(Modifier.fillMaxSize()) {
            regions.forEachIndexed { i, region ->
                val rect = toView(region, viewSize)
                drawRect(colors[i], topLeft = Offset(rect.left, rect.top), size = rect.size, style = Stroke(strokePx))
            }
        }
        val density = LocalDensity.current
        regions.forEachIndexed { i, region ->
            val rect = toView(region, viewSize)
            Box(Modifier.offset(x = with(density) { rect.left.toDp() }, y = with(density) { (rect.top.toDp() - 28.dp).coerceAtLeast(0.dp) })) {
                Surface(color = colors[i], contentColor = TizonTheme.severity.onSeverity, shape = MaterialTheme.shapes.extraSmall) {
                    Text(
                        stringResource(region.label.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
