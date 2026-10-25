package com.lfmm.tizonmovile.data.api

import com.lfmm.tizonmovile.data.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F2-MOB-01: los modelos leen JSON con la forma del contrato (fixtures en src/test/resources). */
class ModelsDeserializationTest {

    private inline fun <reified T> fixture(name: String): T {
        val text = javaClass.getResource("/fixtures/$name")!!.readText()
        return TizonApiClient.JSON.decodeFromString(text)
    }

    @Test
    fun loginResponse() {
        val login = fixture<LoginResponse>("login_response.json")
        assertEquals("2026-11-08T10:15:00-05:00", login.expiresAt)
        assertEquals(Role.AGRICULTOR, login.user.role)
        assertEquals("Ana Pérez", login.user.fullName)
    }

    @Test
    fun appConfig() {
        val config = fixture<AppConfig>("config.json")
        assertEquals(30.0, config.severity.criticalPct)
        assertEquals(700L, config.frameIntervalMs)
        assertTrue(config.classes.single { it.id == ClassId.TIZON_TARDIO }.affected)
        assertTrue(config.classes.single { it.id == ClassId.TIZON_TEMPRANO }.affected)
    }

    @Test
    fun frameDelModeloAnteriorSinTizonTemprano() {
        val frame = fixture<FrameAnalysis>("frame_detected.json")
        assertNull(frame.probabilities!!.tizonTemprano)
        assertEquals(0, frame.sessionStats!!.notPotatoFrames)
        assertTrue(frame.sessionStats!!.labelCounts.isEmpty())
    }

    @Test
    fun frameTizonTempranoConConteoPorClase() {
        val frame = fixture<FrameAnalysis>("frame_early_blight.json")
        assertEquals(ClassId.TIZON_TEMPRANO, frame.label)
        assertEquals(0.88, frame.probabilities!!.tizonTemprano)
        val stats = frame.sessionStats!!
        assertEquals(mapOf(ClassId.SANO to 1, ClassId.TIZON_TEMPRANO to 2, ClassId.TIZON_TARDIO to 0), stats.labelCounts)
        assertEquals(1, stats.notPotatoFrames)
    }

    @Test
    fun hojaDeOtraPlantaEsUnResultadoSinDiagnostico() {
        val frame = fixture<FrameAnalysis>("frame_not_potato.json")
        assertEquals(FrameStatus.NOT_POTATO, frame.status)
        assertNull(frame.label)
        assertEquals(1, frame.sessionStats!!.notPotatoFrames)
    }

    @Test
    fun frameDetected() {
        val frame = fixture<FrameAnalysis>("frame_detected.json")
        assertEquals(FrameStatus.DETECTED, frame.status)
        assertEquals(ClassId.TIZON_TARDIO, frame.label)
        assertEquals(0.92, frame.probabilities!!.tizonTardio)
        assertEquals(ClassId.TIZON_TARDIO, frame.regions.single().label)
        assertEquals(Severity.CRITICAL, frame.sessionStats!!.severity)
    }

    @Test
    fun frameNoVegetationEsUnResultadoConLabelNull() {
        val frame = fixture<FrameAnalysis>("frame_no_vegetation.json")
        assertEquals(FrameStatus.NO_VEGETATION, frame.status)
        assertNull(frame.label)
        assertNull(frame.probabilities)
        assertTrue(frame.regions.isEmpty())
        assertNull(frame.sessionStats!!.dominantLabel)
    }

    @Test
    fun sessionPageConEstadisticasPlanas() {
        val page = fixture<SessionPage>("session_page.json")
        val session = page.items.single()
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(50.0, session.affectionPct)
        assertEquals("Lote 3", session.fieldName)
        assertEquals(12.5, session.location!!.accuracyM)
        assertEquals(ClassId.TIZON_TARDIO, session.recommendations.single().disease)
        assertEquals("ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19", session.recommendations.single().source)
    }

    @Test
    fun errorBody() {
        val error = fixture<ApiErrorBody>("error_validation.json")
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals("req-1", error.requestId)
        assertTrue(error.fields!!.containsKey("password"))
    }
}
