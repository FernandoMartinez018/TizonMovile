package com.lfmm.tizonmovile.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.Alert
import com.lfmm.tizonmovile.data.model.AlertStatus
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

sealed interface AlertsUiState {
    data object Loading : AlertsUiState
    data object Error : AlertsUiState
    /** [isStaff]: TECNICO o ADMIN ven el usuario de cada alerta y pueden marcarla como atendida. */
    data class Content(val alerts: List<Alert>, val isStaff: Boolean) : AlertsUiState
}

/** Alertas (RF-11 CA3): abiertas y atendidas, con severidad, usuario (para TECNICO), finca y fecha. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(state: AlertsUiState, onAcknowledge: (Alert) -> Unit, onRetry: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.alerts_title)) }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                AlertsUiState.Loading -> LoadingState()
                AlertsUiState.Error -> Column(Modifier.padding(Spacing.l)) {
                    ErrorBanner(stringResource(R.string.alerts_error), onRetry = onRetry)
                }
                is AlertsUiState.Content -> if (state.alerts.isEmpty()) {
                    EmptyState(Icons.Filled.NotificationsNone, stringResource(R.string.alerts_empty_title), stringResource(R.string.alerts_empty_body))
                } else {
                    LazyColumn(contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        items(state.alerts, key = { it.alertId }) { alert -> AlertCard(alert, state.isStaff) { onAcknowledge(alert) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert, isStaff: Boolean, onAcknowledge: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                SeverityChip(alert.severity)
                Text(stringResource(R.string.summary_affected_pct, formatPct(alert.affectionPct)), style = MaterialTheme.typography.titleMedium)
            }
            Text(formatShortDateTime(alert.createdAt), style = MaterialTheme.typography.titleMedium)
            Text(alert.fieldName ?: stringResource(R.string.scan_field_none), style = MaterialTheme.typography.bodyMedium)
            if (isStaff) Text(stringResource(R.string.alerts_by_user, alert.username), style = MaterialTheme.typography.bodyMedium)
            when {
                alert.status == AlertStatus.ACKNOWLEDGED -> Text(
                    stringResource(R.string.alerts_acknowledged, alert.acknowledgedBy ?: "—"),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                )
                isStaff -> OutlinedButton(onClick = onAcknowledge, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                    Text(stringResource(R.string.alerts_ack))
                }
                else -> Text(stringResource(R.string.alerts_open), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@TizonPreviews @Composable
private fun AlertasVaciasPreview() = TizonTheme { AlertsScreen(AlertsUiState.Content(emptyList(), isStaff = false), {}, {}) }

@TizonPreviews @Composable
private fun AlertasAgricultorPreview() = TizonTheme {
    AlertsScreen(AlertsUiState.Content(PreviewData.alerts.take(1), isStaff = false), {}, {})
}

@TizonPreviews @Composable
private fun AlertasTecnicoPreview() = TizonTheme { AlertsScreen(AlertsUiState.Content(PreviewData.alerts, isStaff = true), {}, {}) }
