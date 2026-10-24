package com.lfmm.tizonmovile.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    data class Content(val summary: SessionSummary) : DetailUiState
}

/** Resumen al finalizar y detalle desde el historial: GET /sessions/{id} (con recomendaciones y alerta). */
class SessionDetailViewModel(private val sessionId: String, private val repo: ScanRepository) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            repo.session(sessionId)
                .onSuccess { _state.value = DetailUiState.Content(it) }
                .onFailure { _state.value = DetailUiState.Error }
        }
    }
}
