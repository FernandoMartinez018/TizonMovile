package com.lfmm.tizonmovile.ui.scan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.location.LocationProvider
import com.lfmm.tizonmovile.data.model.FrameAnalysis
import com.lfmm.tizonmovile.data.model.FrameStatus
import com.lfmm.tizonmovile.data.model.StartSessionRequest
import com.lfmm.tizonmovile.data.repository.ScanRepository
import com.lfmm.tizonmovile.data.settings.UserPreferences
import com.lfmm.tizonmovile.ui.components.ScanStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Frame ya rotado (orientación de la pantalla), escalado y comprimido en JPEG Base64. */
data class EncodedFrame(val base64: String, val width: Int, val height: Int)

sealed interface ScanEvent {
    data class Finished(val sessionId: String) : ScanEvent
}

/**
 * Escaneo (RF-04, RF-05, RF-07). La sesión se crea al pulsar "Iniciar escaneo"; solo hay un frame
 * en curso y el siguiente se pide [frame_interval_ms] después de la respuesta (no hay delay fijo).
 * 502/504 y falta de red se reintentan con espera de 2, 4 y 8 s. Un error técnico nunca se muestra
 * como diagnóstico.
 */
class ScanViewModel(
    private val repo: ScanRepository,
    private val location: LocationProvider,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState(fieldName = prefs.lastFieldName))
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events: Flow<ScanEvent> = _events.receiveAsFlow()

    /** El analizador de CameraX entrega un frame solo cuando esto es true. */
    private val _wantsFrame = MutableStateFlow(false)
    val wantsFrame: StateFlow<Boolean> = _wantsFrame.asStateFlow()

    private var sessionId: String? = null
    private var frameIntervalMs = DEFAULT_FRAME_INTERVAL_MS
    private var retryAttempt = 0
    private var nextFrameJob: Job? = null
    private var busy = false

    init {
        viewModelScope.launch { repo.config().onSuccess { frameIntervalMs = it.frameIntervalMs } }
    }

    fun onPrimary() = if (_state.value.isScanning) finish() else start()

    fun setFieldName(name: String) {
        val value = name.trim().takeIf { it.isNotEmpty() }
        prefs.lastFieldName = value
        _state.update { it.copy(fieldName = value) }
    }

    fun start() {
        if (_state.value.isScanning || busy) return
        busy = true
        _state.update { it.copy(notice = null, status = ScanStatus.READY) }
        viewModelScope.launch {
            val position = location.current()   // null si no hay permiso o se agotan los 10 s
            repo.startSession(StartSessionRequest(position, _state.value.fieldName))
                .onSuccess { session ->
                    sessionId = session.sessionId
                    retryAttempt = 0
                    _state.update {
                        ScanUiState(
                            status = ScanStatus.ANALYZING, isScanning = true, fieldName = it.fieldName,
                            hasLocation = position != null,
                        )
                    }
                    requestFrame(0)
                }
                .onFailure { e -> _state.update { it.copy(status = statusFor(e), notice = noticeFor(e)) } }
            busy = false
        }
    }

    /** Lo llama el analizador de la cámara (hilo de cámara); ignora frames que no se pidieron. */
    fun onFrame(frame: EncodedFrame) {
        if (!_wantsFrame.compareAndSet(expect = true, update = false)) return
        val id = sessionId ?: return
        viewModelScope.launch { analyze(id, frame) }
    }

    fun togglePause() {
        if (!_state.value.isScanning) return
        val paused = !_state.value.isPaused
        _state.update { it.copy(isPaused = paused, status = if (paused) ScanStatus.PAUSED else ScanStatus.ANALYZING) }
        if (paused) stopFrames() else requestFrame(0)
    }

    /** Al salir de la pestaña o ir a segundo plano, la sesión queda en pausa (la cámara se libera). */
    fun onLeaveScreen() {
        if (_state.value.isScanning && !_state.value.isPaused) togglePause()
    }

    fun finish() {
        val id = sessionId ?: return
        if (busy) return
        busy = true
        stopFrames()
        viewModelScope.launch {
            repo.endSession(id)
                .onSuccess { summary ->
                    reset()
                    _events.send(ScanEvent.Finished(summary.sessionId))
                }
                .onFailure { e ->
                    if (e == AppError.SessionNotActive) reset(R.string.scan_session_closed)
                    else _state.update { it.copy(status = statusFor(e), notice = noticeFor(e)) }   // se puede reintentar
                }
            busy = false
        }
    }

    /** RF-04 CA6 "Descartar": la sesión queda CANCELLED y no genera alerta. */
    fun discard() {
        val id = sessionId ?: return
        stopFrames()
        reset()
        viewModelScope.launch { repo.cancelSession(id) }   // si falla, el backend la cancela a los 30 min
    }

    private suspend fun analyze(id: String, frame: EncodedFrame) {
        repo.analyzeFrame(id, frame.base64, null)
            .onSuccess { onResult(it) }
            .onFailure { e ->
                when (e) {
                    // Frame corrupto generado por la propia app: se descarta sin molestar (RF-07 CA2).
                    AppError.InvalidImage -> requestFrame(frameIntervalMs)
                    AppError.SessionNotActive -> reset(R.string.scan_session_closed)
                    AppError.Unauthorized -> stopFrames()   // el SessionManager lleva al login
                    else -> {
                        _state.update { it.copy(status = statusFor(e)) }
                        requestFrame(nextBackoffMs())
                    }
                }
            }
    }

    private fun onResult(r: FrameAnalysis) {
        retryAttempt = 0
        _state.update {
            it.copy(
                status = if (r.status == FrameStatus.NO_VEGETATION) ScanStatus.NO_LEAF else ScanStatus.ANALYZING,
                regions = r.regions,
                frameWidth = r.frameWidth,
                frameHeight = r.frameHeight,
                lastLabel = r.label ?: it.lastLabel,
                lastConfidence = r.confidence ?: it.lastConfidence,
                stats = r.sessionStats ?: it.stats,
            )
        }
        requestFrame(frameIntervalMs)
    }

    private fun requestFrame(afterMs: Long) {
        nextFrameJob?.cancel()
        nextFrameJob = viewModelScope.launch {
            delay(afterMs)
            if (sessionId != null && !_state.value.isPaused) _wantsFrame.value = true
        }
    }

    private fun stopFrames() {
        nextFrameJob?.cancel()
        _wantsFrame.value = false
    }

    private fun reset(@StringRes notice: Int? = null) {
        stopFrames()
        sessionId = null
        _state.update { ScanUiState(fieldName = it.fieldName, notice = notice) }
    }

    /** 2 s, 4 s y luego 8 s mientras persista la falla (RF-07 CA3). */
    private fun nextBackoffMs(): Long = BACKOFF_MS[retryAttempt.coerceAtMost(BACKOFF_MS.lastIndex)].also { retryAttempt++ }

    private fun statusFor(e: Throwable) = if (e == AppError.Network) ScanStatus.OFFLINE else ScanStatus.SERVICE_UNAVAILABLE

    @StringRes
    private fun noticeFor(e: Throwable): Int = if (e == AppError.Network) R.string.error_offline else R.string.error_server

    companion object {
        const val DEFAULT_FRAME_INTERVAL_MS = 700L
        val BACKOFF_MS = longArrayOf(2_000, 4_000, 8_000)
    }
}
