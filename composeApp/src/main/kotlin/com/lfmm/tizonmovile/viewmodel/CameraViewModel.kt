package com.lfmm.tizonmovile.viewmodel

import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.model.CameraUiState
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.data.repository.AnalysisRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * ViewModel principal de la pantalla de cámara.
 *
 * Responsabilidades:
 * 1. Gestionar el ciclo de vida de la sesión de escaneo.
 * 2. Recibir frames de la cámara (via [onNewFrame]) y procesarlos.
 * 3. Hacer throttle de los frames (canal CONFLATED = solo el más reciente).
 * 4. Llamar a la API de análisis y actualizar el estado de la UI.
 * 5. Exponer el historial de sesiones para la pantalla de reportes.
 *
 * Diseño del canal de frames:
 *   La cámara produce frames a ~30fps, pero la API solo puede procesar ~2fps.
 *   Se usa un Channel.CONFLATED: si el canal tiene un frame esperando y llega
 *   uno nuevo, el viejo se descarta. Solo se procesa el frame más reciente.
 *   Esto evita colas de frames que provocarían lag visual.
 */
class CameraViewModel(private val repository: AnalysisRepository) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Estado de la cámara ───────────────────────────────────────────────
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    // ── Historial de sesiones ────────────────────────────────────────────
    private val _history = MutableStateFlow<List<SessionSummary>>(emptyList())
    val history: StateFlow<List<SessionSummary>> = _history.asStateFlow()

    // ── Canal de frames ───────────────────────────────────────────────────
    // CONFLATED: buffer de 1 elemento. Si el consumidor está ocupado,
    // el frame antiguo se reemplaza por el nuevo automáticamente.
    private val frameBus = Channel<String>(Channel.CONFLATED)

    // Job del bucle de análisis (puede cancelarse al pausar)
    private var analysisJob: Job? = null

    // ══════════════════════════════════════════════════════════════════════
    // GESTIÓN DE SESIONES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inicia una nueva sesión de escaneo en el servidor.
     * Llamar al entrar a la pantalla de cámara.
     */
    fun startSession() {
        scope.launch {
            repository.startSession()
                .onSuccess { session ->
                    _state.update { it.copy(sessionId = session.sessionId, stats = null, currentResult = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = userMessage(error)) }
                }
        }
    }

    /**
     * Finaliza la sesión activa y carga el historial actualizado.
     * Llamar al navegar a la pantalla de reportes.
     */
    fun endSession() {
        val sessionId = _state.value.sessionId ?: return
        stopAnalysis()

        scope.launch {
            repository.endSession(sessionId)
                .onSuccess {
                    _state.update { st -> st.copy(isActive = false, sessionId = null) }
                    loadHistory()
                }
                .onFailure { error ->
                    _state.update { it.copy(error = userMessage(error)) }
                }
        }
    }

    /** Carga el historial de sesiones completadas. */
    fun loadHistory() {
        scope.launch {
            repository.getHistory()
                .onSuccess { page -> _history.value = page.items }
                .onFailure { error ->
                    _state.update { it.copy(error = userMessage(error)) }
                }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONTROL DE CÁMARA Y ANÁLISIS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Activa o desactiva el análisis de frames.
     * La cámara siempre muestra el feed; solo se controla el envío a la API.
     */
    fun toggleCamera() {
        if (_state.value.isActive) stopAnalysis() else startAnalysis()
    }

    private fun startAnalysis() {
        _state.update { it.copy(isActive = true, error = null) }

        analysisJob = scope.launch {
            // El bucle corre mientras el job esté activo y isActive = true
            while (isActive && _state.value.isActive) {
                // Espera un frame del canal (suspende hasta que llegue uno)
                val frame = frameBus.receive()
                processFrame(frame)
                // Pausa entre frames para no saturar la API
                // El Channel.CONFLATED ya descartó frames intermedios
                delay(500L)  // ~2 fps
            }
        }
    }

    private fun stopAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _state.update { it.copy(isActive = false) }
    }

    /**
     * Recibe un frame de la cámara (llamado desde la capa de plataforma).
     *
     * El frame en Base64 se envía al canal CONFLATED.
     * Si el canal ya tiene un frame esperando, este lo reemplaza.
     * El análisis nunca se acumula en cola.
     */
    fun onNewFrame(frameB64: String) {
        if (_state.value.isActive) {
            frameBus.trySend(frameB64)
        }
    }

    private suspend fun processFrame(frameB64: String) {
        val sessionId = _state.value.sessionId ?: return
        _state.update { it.copy(isAnalyzing = true) }

        repository.analyzeFrame(sessionId, frameB64)
            .onSuccess { result ->
                _state.update { current ->
                    current.copy(
                        currentResult = result,
                        stats         = result.sessionStats ?: current.stats,
                        isAnalyzing   = false,
                        error         = null
                    )
                }
            }
            .onFailure { error ->
                // Un frame corrupto generado por la propia app no se muestra (RF-07 CA2).
                val message = if (error == AppError.InvalidImage) null else userMessage(error)
                _state.update { it.copy(isAnalyzing = false, error = message) }
            }
    }

    // Textos provisionales: F3-MOB-03 los pasa a strings.xml con los estados de RF-07.
    private fun userMessage(error: Throwable): String = when (error) {
        AppError.Network -> "Sin conexión"
        AppError.IaUnavailable, AppError.IaTimeout -> "Servicio no disponible. Reintentando…"
        AppError.SessionNotActive -> "Este escaneo ya terminó. Inicia uno nuevo."
        AppError.Unauthorized -> "Tu sesión expiró, inicia sesión de nuevo"
        else -> "El servicio no está disponible, intenta más tarde"
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun dispose() {
        analysisJob?.cancel()
        scope.cancel()
    }
}