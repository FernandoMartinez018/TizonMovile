package com.lfmm.tizonmovile.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.model.Alert
import com.lfmm.tizonmovile.data.model.AlertStatus
import com.lfmm.tizonmovile.data.model.Role
import com.lfmm.tizonmovile.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Alertas (RF-11 CA3) y contador de la pestaña. Se comparte en la actividad: la barra inferior
 * muestra cuántas siguen abiertas.
 */
class AlertsViewModel(private val repo: AlertRepository, session: SessionManager) : ViewModel() {

    private val isStaff = session.user()?.role.let { it == Role.TECNICO || it == Role.ADMIN }

    private val _state = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    val openCount: StateFlow<Int> = _state
        .map { s -> (s as? AlertsUiState.Content)?.alerts?.count { it.status == AlertStatus.OPEN } ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repo.alerts()
                .onSuccess { _state.value = AlertsUiState.Content(it, isStaff) }
                .onFailure { if (_state.value !is AlertsUiState.Content) _state.value = AlertsUiState.Error }
        }
    }

    fun acknowledge(alert: Alert) {
        if (!isStaff) return
        viewModelScope.launch {
            repo.acknowledge(alert.alertId).onSuccess { updated ->
                val current = _state.value as? AlertsUiState.Content ?: return@onSuccess
                _state.value = current.copy(alerts = current.alerts.map { if (it.alertId == updated.alertId) updated else it })
            }
        }
    }
}
