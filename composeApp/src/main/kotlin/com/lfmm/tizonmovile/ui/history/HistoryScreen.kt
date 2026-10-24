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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
    /** [filter]: usuario filtrado por un TECNICO o ADMIN; null = sin filtro. */
    data class Content(
        val items: List<SessionSummary>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val filter: String? = null,
    ) : HistoryUiState
}

/**
 * Historial (RF-09): tarjetas con fecha local, finca, severidad y %; pull-to-refresh y paginación.
 * [isStaff]: TECNICO o ADMIN ven el usuario de cada sesión y pueden filtrar por usuario (CA4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (SessionSummary) -> Unit,
    onStartScan: () -> Unit,
    isStaff: Boolean = false,
    onFilter: (String) -> Unit = {},
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (isStaff) UserFilter(onFilter)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    HistoryUiState.Loading -> LoadingState()
                    HistoryUiState.Error -> Column(Modifier.padding(Spacing.l)) {
                        ErrorBanner(stringResource(R.string.history_error), onRetry = onRefresh)
                    }
                    is HistoryUiState.Content -> if (state.items.isEmpty() && state.filter != null) {
                        EmptyState(
                            icon = Icons.Filled.PersonSearch,
                            title = stringResource(R.string.history_filter_empty_title, state.filter),
                            body = stringResource(R.string.history_filter_empty_body),
                        )
                    } else if (state.items.isEmpty()) {
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
                                items(state.items, key = { it.sessionId }) { session -> HistoryCard(session, isStaff) { onOpen(session) } }
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
}

@Composable
private fun UserFilter(onFilter: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
        label = { Text(stringResource(R.string.history_filter_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (text.isEmpty()) null else {
            {
                IconButton(onClick = { text = ""; onFilter("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.history_filter_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onFilter(text) }),
    )
}

@Composable
private fun HistoryCard(session: SessionSummary, isStaff: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(formatShortDateTime(session.startedAt), style = MaterialTheme.typography.titleMedium)
            if (isStaff) Text(stringResource(R.string.history_by_user, session.username), style = MaterialTheme.typography.bodyMedium)
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
private fun Preview(state: HistoryUiState, isStaff: Boolean = false) =
    TizonTheme { HistoryScreen(state, {}, {}, {}, {}, isStaff = isStaff) }

@TizonPreviews @Composable
private fun HistorialCargandoPreview() = Preview(HistoryUiState.Loading)

@TizonPreviews @Composable
private fun HistorialErrorPreview() = Preview(HistoryUiState.Error)

@TizonPreviews @Composable
private fun HistorialVacioPreview() = Preview(HistoryUiState.Content(emptyList()))

@TizonPreviews @Composable
private fun HistorialConDatosPreview() = Preview(HistoryUiState.Content(PreviewData.history))

@TizonPreviews @Composable
private fun HistorialTecnicoPreview() = Preview(HistoryUiState.Content(PreviewData.history), isStaff = true)

@TizonPreviews @Composable
private fun HistorialFiltroSinResultadosPreview() =
    Preview(HistoryUiState.Content(emptyList(), filter = PreviewData.users.last().username), isStaff = true)
