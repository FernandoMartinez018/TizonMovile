package com.lfmm.tizonmovile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Historial paginado con pull-to-refresh (RF-09). */
class HistoryViewModel(private val repo: ScanRepository) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val items = mutableListOf<SessionSummary>()
    private var nextPage = 0
    private var total = Long.MAX_VALUE
    private var loading = false

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

    private fun load(reset: Boolean) {
        loading = true
        val page = if (reset) 0 else nextPage
        viewModelScope.launch {
            repo.history(page, PAGE_SIZE)
                .onSuccess { p ->
                    if (reset) items.clear()
                    items += p.items
                    nextPage = page + 1
                    total = p.total
                    _state.value = HistoryUiState.Content(items.toList())
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
