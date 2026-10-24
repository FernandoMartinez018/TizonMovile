package com.lfmm.tizonmovile.ui.scan

import androidx.compose.ui.geometry.Size
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.Probabilities
import com.lfmm.tizonmovile.data.model.Region
import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-05 CA4: las cajas se alinean con la vista previa en FILL_CENTER (escala + recorte centrado). */
class PreviewTransformTest {

    private fun region(x: Double, y: Double, w: Double, h: Double) =
        Region(x, y, w, h, ClassId.SANO, 0.9, Probabilities(sano = 0.9, tizonTardio = 0.1))

    @Test
    fun vistaMasAltaQueElFrameRecortaLosLados() {
        // Frame 480×640 (3:4) en una vista 1080×2160: escala 3,375, ancho 1620 → se recortan 270 px por lado.
        val rect = PreviewTransform.toView(region(0.5, 0.25, 0.25, 0.5), 480, 640, Size(1080f, 2160f))
        assertEquals(540f, rect.left, 0.01f)
        assertEquals(945f, rect.right, 0.01f)
        assertEquals(540f, rect.top, 0.01f)
        assertEquals(1620f, rect.bottom, 0.01f)
    }

    @Test
    fun vistaMasAnchaQueElFrameRecortaArribaYAbajo() {
        // Frame 480×640 en una vista 1200×1200: escala 2,5, alto 1600 → se recortan 200 px arriba y abajo.
        val rect = PreviewTransform.toView(region(0.0, 0.0, 1.0, 1.0), 480, 640, Size(1200f, 1200f))
        assertEquals(0f, rect.left, 0.01f)
        assertEquals(1200f, rect.right, 0.01f)
        assertEquals(-200f, rect.top, 0.01f)
        assertEquals(1400f, rect.bottom, 0.01f)
    }

    @Test
    fun mismaProporcionSoloEscala() {
        val rect = PreviewTransform.toView(region(0.1, 0.2, 0.3, 0.4), 480, 640, Size(960f, 1280f))
        assertEquals(96f, rect.left, 0.01f)
        assertEquals(256f, rect.top, 0.01f)
        assertEquals(384f, rect.right, 0.01f)
        assertEquals(768f, rect.bottom, 0.01f)
    }

    @Test
    fun tamanoDeFrameDesconocidoEstiraALaVista() {
        val rect = PreviewTransform.toView(region(0.5, 0.5, 0.5, 0.5), 0, 0, Size(100f, 200f))
        assertEquals(50f, rect.left, 0.01f)
        assertEquals(100f, rect.top, 0.01f)
    }

    @Test
    fun elFrameSeEscalaA640PorElLadoMayor() {
        assertEquals(480 to 640, FrameEncoder.targetSize(720, 960))
        assertEquals(640 to 480, FrameEncoder.targetSize(1920, 1440))
        assertEquals(300 to 200, FrameEncoder.targetSize(300, 200), "no agranda imágenes pequeñas")
    }
}
