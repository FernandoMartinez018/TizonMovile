package com.lfmm.tizonmovile.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.SessionStatus
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.ui.components.EmptyState
import com.lfmm.tizonmovile.ui.components.ErrorBanner
import com.lfmm.tizonmovile.ui.components.LoadingState
import com.lfmm.tizonmovile.ui.components.SeverityChip
import com.lfmm.tizonmovile.ui.formatPct
import com.lfmm.tizonmovile.ui.formatShortDateTime
import com.lfmm.tizonmovile.ui.preview.PreviewData
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Error : HistoryUiState
    data class Content(
        val items: List<SessionSummary>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
    ) : HistoryUiState
}

/** Historial (RF-09): tarjetas con fecha local, finca, severidad y %; pull-to-refresh y paginación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (SessionSummary) -> Unit,
    onStartScan: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                HistoryUiState.Loading -> LoadingState()
                HistoryUiState.Error -> Column(Modifier.padding(Spacing.l)) {
                    ErrorBanner(stringResource(R.string.history_error), onRetry = onRefresh)
                }
                is HistoryUiState.Content -> if (state.items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                        actionLabel = stringResource(R.string.history_empty_action),
                        onAction = onStartScan,
                    )
                } else {
                    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
                        LazyColumn(contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            items(state.items, key = { it.sessionId }) { session -> HistoryCard(session) { onOpen(session) } }
                            item {
                                if (state.isLoadingMore) {
                                    Box(Modifier.fillMaxWidth().padding(Spacing.l), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                } else {
                                    androidx.compose.runtime.LaunchedEffect(state.items.size) { onLoadMore() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(session: SessionSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(formatShortDateTime(session.startedAt), style = MaterialTheme.typography.titleMedium)
            Text(session.fieldName ?: stringResource(R.string.scan_field_none), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                SeverityChip(session.severity)
                Text(stringResource(R.string.summary_affected_pct, formatPct(session.affectionPct)), style = MaterialTheme.typography.titleMedium)
            }
            when (session.status) {
                SessionStatus.ACTIVE -> Text(stringResource(R.string.history_status_active), style = MaterialTheme.typography.labelMedium)
                SessionStatus.CANCELLED -> Text(stringResource(R.string.history_status_cancelled), style = MaterialTheme.typography.labelMedium)
                SessionStatus.COMPLETED -> Unit
            }
        }
    }
}

@Composable
private fun Preview(state: HistoryUiState) = TizonTheme { HistoryScreen(state, {}, {}, {}, {}) }

@TizonPreviews @Composable
private fun HistorialCargandoPreview() = Preview(HistoryUiState.Loading)

@TizonPreviews @Composable
private fun HistorialErrorPreview() = Preview(HistoryUiState.Error)

@TizonPreviews @Composable
private fun HistorialVacioPreview() = Preview(HistoryUiState.Content(emptyList()))

@TizonPreviews @Composable
private fun HistorialConDatosPreview() = Preview(HistoryUiState.Content(PreviewData.history))
