package com.lfmm.tizonmovile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.model.Role
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Historial paginado con pull-to-refresh (RF-09). */
class HistoryViewModel(private val repo: ScanRepository, session: SessionManager) : ViewModel() {

    /** RF-09 CA4: TECNICO y ADMIN ven las sesiones de todos y pueden filtrar por usuario. */
    val isStaff = session.user()?.role.let { it == Role.TECNICO || it == Role.ADMIN }

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val items = mutableListOf<SessionSummary>()
    private var nextPage = 0
    private var total = Long.MAX_VALUE
    private var loading = false
    private var filter: String? = null
    private var generation = 0

    init {
        refresh()
    }

    fun refresh() {
        if (loading) return
        val current = _state.value
        if (current is HistoryUiState.Content) _state.value = current.copy(isRefreshing = true) else _state.value = HistoryUiState.Loading
        load(reset = true)
    }

    fun loadMore() {
        if (loading || items.size.toLong() >= total || _state.value !is HistoryUiState.Content) return
        _state.value = (_state.value as HistoryUiState.Content).copy(isLoadingMore = true)
        load(reset = false)
    }

    /** Texto vacío quita el filtro. */
    fun applyFilter(username: String) {
        if (!isStaff) return
        filter = username.trim().ifEmpty { null }
        generation++   // la respuesta de una carga en curso con el filtro anterior se descarta
        loading = false
        _state.value = HistoryUiState.Loading
        load(reset = true)
    }

    private fun load(reset: Boolean) {
        loading = true
        val page = if (reset) 0 else nextPage
        val gen = generation
        val username = filter
        viewModelScope.launch {
            val result = repo.history(page, PAGE_SIZE, username)
            if (gen != generation) return@launch
            result
                .onSuccess { p ->
                    if (reset) items.clear()
                    items += p.items
                    nextPage = page + 1
                    total = p.total
                    _state.value = HistoryUiState.Content(items.toList(), filter = username)
                }
                .onFailure {
                    val current = _state.value
                    _state.value = if (current is HistoryUiState.Content && items.isNotEmpty()) {
                        current.copy(isRefreshing = false, isLoadingMore = false)
                    } else {
                        HistoryUiState.Error
                    }
                }
            loading = false
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
