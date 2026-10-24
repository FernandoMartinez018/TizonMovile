package com.lfmm.tizonmovile.ui.scan

import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.fakes.FakeLocationProvider
import com.lfmm.tizonmovile.fakes.FakeScanRepository
import com.lfmm.tizonmovile.fakes.InMemoryPreferences
import com.lfmm.tizonmovile.fakes.detected
import com.lfmm.tizonmovile.fakes.noVegetation
import com.lfmm.tizonmovile.fakes.notPotato
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.ui.components.ScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F3-MOB-03: ciclo de escaneo con repositorio falso (RF-04, RF-05 CA3, RF-07 CA3). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeScanRepository()
    private val prefs = InMemoryPreferences().apply { lastFieldName = "Lote 3" }
    private val frame = EncodedFrame("A".repeat(200), 480, 640)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.started(): ScanViewModel {
        val vm = ScanViewModel(repo, FakeLocationProvider(), prefs)
        vm.start()
        runCurrent()
        return vm
    }

    @Test
    fun iniciarCreaLaSesionConUbicacionYFincaYPideUnFrame() = runTest(dispatcher) {
        val vm = started()
        assertTrue(vm.state.value.isScanning)
        assertTrue(vm.state.value.hasLocation)
        assertEquals("Lote 3", repo.startRequests.single().fieldName)
        assertEquals(4.71, repo.startRequests.single().location?.latitude)
        assertTrue(vm.wantsFrame.value)
    }

    @Test
    fun sinPermisoDeUbicacionElEscaneoSigueSinUbicacion() = runTest(dispatcher) {
        val vm = ScanViewModel(repo, FakeLocationProvider(location = null), prefs)
        vm.start(); runCurrent()
        assertTrue(vm.state.value.isScanning)
        assertFalse(vm.state.value.hasLocation)
        assertNull(repo.startRequests.single().location)
    }

    @Test
    fun unSoloFrameEnCursoYElSiguienteTrasElIntervalo() = runTest(dispatcher) {
        val vm = started()
        vm.onFrame(frame)
        vm.onFrame(frame)   // ignorado: ya hay uno en curso
        runCurrent()
        assertEquals(1, repo.framesSent)
        assertEquals(1, vm.state.value.stats?.affectedFrames)
        assertEquals(ClassId.TIZON_TARDIO, vm.state.value.lastLabel)
        assertFalse(vm.wantsFrame.value)

        advanceTimeBy(699); runCurrent()
        assertFalse(vm.wantsFrame.value)
        advanceTimeBy(2); runCurrent()
        assertTrue(vm.wantsFrame.value, "el siguiente frame se pide frame_interval_ms después de la respuesta")
    }

    @Test
    fun sinHojaMuestraElEstadoYConservaElUltimoDiagnostico() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.success(detected(ClassId.SANO, affected = 0, analyzed = 1))
        repo.frameResults += Result.success(noVegetation)
        vm.onFrame(frame); runCurrent()
        advanceTimeBy(700); runCurrent()
        vm.onFrame(frame); runCurrent()
        assertEquals(ScanStatus.NO_LEAF, vm.state.value.status)
        assertEquals(ClassId.SANO, vm.state.value.lastLabel)
    }

    @Test
    fun hojaDeOtraPlantaMuestraElEstadoSinCambiarElDiagnostico() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.success(detected(ClassId.TIZON_TEMPRANO, affected = 1, analyzed = 1))
        repo.frameResults += Result.success(notPotato)
        vm.onFrame(frame); runCurrent()
        advanceTimeBy(700); runCurrent()
        vm.onFrame(frame); runCurrent()
        assertEquals(ScanStatus.NOT_POTATO, vm.state.value.status)
        assertEquals(ClassId.TIZON_TEMPRANO, vm.state.value.lastLabel)
    }

    @Test
    fun servicioNoDisponibleReintentaCon2Y4Segundos() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.failure(AppError.IaUnavailable)
        repo.frameResults += Result.failure(AppError.IaTimeout)
        vm.onFrame(frame); runCurrent()
        assertEquals(ScanStatus.SERVICE_UNAVAILABLE, vm.state.value.status)
        advanceTimeBy(1_999); runCurrent()
        assertFalse(vm.wantsFrame.value)
        advanceTimeBy(2); runCurrent()
        assertTrue(vm.wantsFrame.value, "primer reintento a los 2 s")

        vm.onFrame(frame); runCurrent()
        advanceTimeBy(3_999); runCurrent()
        assertFalse(vm.wantsFrame.value)
        advanceTimeBy(2); runCurrent()
        assertTrue(vm.wantsFrame.value, "segundo reintento a los 4 s")

        vm.onFrame(frame); runCurrent()   // éxito: vuelve a Analizando y no persiste ningún error como resultado
        assertEquals(ScanStatus.ANALYZING, vm.state.value.status)
        assertEquals(ClassId.TIZON_TARDIO, vm.state.value.lastLabel)
    }

    @Test
    fun sinConexionMuestraSinConexion() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.failure(AppError.Network)
        vm.onFrame(frame); runCurrent()
        assertEquals(ScanStatus.OFFLINE, vm.state.value.status)
    }

    @Test
    fun frameInvalidoSeDescartaSinCambiarElEstado() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.failure(AppError.InvalidImage)
        vm.onFrame(frame); runCurrent()
        assertEquals(ScanStatus.ANALYZING, vm.state.value.status)
        advanceTimeBy(701); runCurrent()
        assertTrue(vm.wantsFrame.value)
    }

    @Test
    fun sesionNoActivaTerminaElEscaneoYAvisa() = runTest(dispatcher) {
        val vm = started()
        repo.frameResults += Result.failure(AppError.SessionNotActive)
        vm.onFrame(frame); runCurrent()
        assertFalse(vm.state.value.isScanning)
        assertEquals(R.string.scan_session_closed, vm.state.value.notice)
        advanceUntilIdle()
        assertFalse(vm.wantsFrame.value)
    }

    @Test
    fun finalizarEmiteElResumenYLimpiaLaSesion() = runTest(dispatcher) {
        val vm = started()
        vm.onFrame(frame); runCurrent()
        vm.finish(); runCurrent()
        assertEquals(ScanEvent.Finished("s-1"), vm.events.first())
        assertEquals(1, repo.ended)
        assertFalse(vm.state.value.isScanning)
        assertNull(vm.state.value.stats)
        assertEquals("Lote 3", vm.state.value.fieldName, "la finca se recuerda para el próximo escaneo")
        val sent = repo.framesSent
        vm.onFrame(frame); runCurrent()
        assertEquals(sent, repo.framesSent, "sin sesión no se envían frames")
    }

    @Test
    fun descartarCancelaLaSesion() = runTest(dispatcher) {
        val vm = started()
        vm.discard(); runCurrent()
        assertEquals(1, repo.cancelled)
        assertFalse(vm.state.value.isScanning)
    }

    @Test
    fun pausarDetieneLosFramesYContinuarLosReanuda() = runTest(dispatcher) {
        val vm = started()
        vm.togglePause()
        assertEquals(ScanStatus.PAUSED, vm.state.value.status)
        assertFalse(vm.wantsFrame.value)
        vm.togglePause(); runCurrent()
        assertTrue(vm.wantsFrame.value)
    }

    @Test
    fun cambiarDeFincaSeGuarda() = runTest(dispatcher) {
        val vm = ScanViewModel(repo, FakeLocationProvider(), prefs)
        vm.setFieldName("  Finca El Roble ")
        assertEquals("Finca El Roble", vm.state.value.fieldName)
        assertEquals("Finca El Roble", prefs.lastFieldName)
    }
}
