package com.lfmm.tizonmovile.ui.scan

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.lfmm.tizonmovile.data.model.Region
import kotlin.math.max

/**
 * Mapea una región relativa (0–1) del frame enviado a la vista previa de CameraX en FILL_CENTER
 * (RF-05 CA4): el frame se escala hasta cubrir la vista y se recorta centrado.
 * Requiere que la vista previa y el análisis usen la misma relación de aspecto (4:3, ver CameraPreview).
 */
object PreviewTransform {

    fun toView(region: Region, frameWidth: Int, frameHeight: Int, view: Size): Rect {
        if (frameWidth <= 0 || frameHeight <= 0) {
            // Sin tamaño de frame (no debería ocurrir): estirar a la vista.
            return Rect(
                (region.x * view.width).toFloat(), (region.y * view.height).toFloat(),
                ((region.x + region.width) * view.width).toFloat(), ((region.y + region.height) * view.height).toFloat(),
            )
        }
        val scale = max(view.width / frameWidth, view.height / frameHeight)
        val scaledW = frameWidth * scale
        val scaledH = frameHeight * scale
        val offsetX = (view.width - scaledW) / 2f   // negativo: parte del frame queda fuera de la vista
        val offsetY = (view.height - scaledH) / 2f
        return Rect(
            left = offsetX + (region.x * scaledW).toFloat(),
            top = offsetY + (region.y * scaledH).toFloat(),
            right = offsetX + ((region.x + region.width) * scaledW).toFloat(),
            bottom = offsetY + ((region.y + region.height) * scaledH).toFloat(),
        )
    }
}
