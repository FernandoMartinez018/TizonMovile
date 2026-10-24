package com.lfmm.tizonmovile.ui.history

import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.model.SessionPage
import com.lfmm.tizonmovile.data.model.SessionStatus
import com.lfmm.tizonmovile.fakes.FakeScanRepository
import com.lfmm.tizonmovile.fakes.summary
import com.lfmm.tizonmovile.work.AlertsWorker
import com.lfmm.tizonmovile.data.model.Alert
import com.lfmm.tizonmovile.data.model.AlertStatus
import com.lfmm.tizonmovile.data.model.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** F3-MOB-04 / RF-09: historial paginado, refresco y error; y el `since` del sondeo de alertas. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeScanRepository().apply {
        // 25 sesiones en páginas de 20.
        historyPages = { page ->
            val items = (0 until if (page == 0) 20 else 5).map { summary("s-$page-$it", SessionStatus.COMPLETED) }
            Result.success(SessionPage(items, page, 20, 25))
        }
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cargaPaginasHastaElTotal() = runTest(dispatcher) {
        val vm = HistoryViewModel(repo)
        advanceUntilIdle()
        assertEquals(20, assertIs<HistoryUiState.Content>(vm.state.value).items.size)

        vm.loadMore(); advanceUntilIdle()
        assertEquals(25, assertIs<HistoryUiState.Content>(vm.state.value).items.size)

        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 1), repo.historyRequests, "no pide más allá del total")
    }

    @Test
    fun refrescarVuelveALaPrimeraPagina() = runTest(dispatcher) {
        val vm = HistoryViewModel(repo)
        advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        vm.refresh(); advanceUntilIdle()
        assertEquals(20, assertIs<HistoryUiState.Content>(vm.state.value).items.size)
        assertEquals(listOf(0, 1, 0), repo.historyRequests)
    }

    @Test
    fun errorSinDatosMuestraError() = runTest(dispatcher) {
        repo.historyPages = { Result.failure(AppError.Network) }
        val vm = HistoryViewModel(repo)
        advanceUntilIdle()
        assertIs<HistoryUiState.Error>(vm.state.value)
    }

    @Test
    fun elSiguienteSinceEsLaAlertaMasReciente() {
        fun alert(id: String, created: String) =
            Alert(id, "s", "ana", Severity.CRITICAL, 40.0, status = AlertStatus.OPEN, createdAt = created)
        val alerts = listOf(alert("a", "2026-11-03T10:00:00-05:00"), alert("b", "2026-11-03T16:30:00Z"), alert("c", "2026-11-03T11:00:00-05:00"))
        assertEquals("2026-11-03T16:30:00Z", AlertsWorker.newestCreatedAt(alerts))
        assertEquals(null, AlertsWorker.newestCreatedAt(emptyList()))
    }
}
