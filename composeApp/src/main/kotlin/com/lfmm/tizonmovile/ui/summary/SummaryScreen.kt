package com.lfmm.tizonmovile.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.ui.components.SeverityChip
import com.lfmm.tizonmovile.ui.formatPct
import com.lfmm.tizonmovile.ui.formatShortDateTime
import com.lfmm.tizonmovile.ui.preview.PreviewData
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.AffectionNumberStyle
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme
import java.time.Duration
import java.time.OffsetDateTime

/** Resumen al finalizar (RF-08): severidad, % afectado, datos, recomendaciones con fuente y alerta. */
@Composable
fun SummaryScreen(summary: SessionSummary, onNewScan: () -> Unit, onViewHistory: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineMedium)
        SummaryContent(summary)
        Button(onClick = onNewScan, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.scanButton), shape = MaterialTheme.shapes.extraLarge) {
            Text(stringResource(R.string.summary_new_scan), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onViewHistory, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.summary_view_history))
        }
    }
    }
}

/** Detalle de una sesión del historial: mismo contenido que el resumen (RF-09 CA2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(summary: SessionSummary, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.detail_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back)) }
            },
        )
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.l)) {
            Text(formatShortDateTime(summary.startedAt), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Spacing.m))
            SummaryContent(summary)
        }
    }
}

@Composable
fun SummaryContent(summary: SessionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SeverityChip(summary.severity, filled = true)
                Text(stringResource(R.string.summary_affected_pct, formatPct(summary.affectionPct)), style = AffectionNumberStyle)
                Text(stringResource(R.string.scan_affected), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (summary.alertId != null) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Text(stringResource(R.string.summary_alert_created), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                DataRow(stringResource(R.string.summary_leaves), summary.analyzedFrames.toString())
                DataRow(stringResource(R.string.summary_affected), summary.affectedFrames.toString())
                summary.labelCounts[ClassId.TIZON_TEMPRANO]?.let { DataRow(stringResource(R.string.summary_early_blight), it.toString()) }
                summary.labelCounts[ClassId.TIZON_TARDIO]?.let { DataRow(stringResource(R.string.summary_late_blight), it.toString()) }
                if (summary.notPotatoFrames > 0) DataRow(stringResource(R.string.summary_not_potato), summary.notPotatoFrames.toString())
                durationMinutes(summary)?.let { DataRow(stringResource(R.string.summary_duration), stringResource(R.string.summary_duration_value, it)) }
                DataRow(stringResource(R.string.summary_field), summary.fieldName ?: stringResource(R.string.scan_field_none))
                DataRow(
                    stringResource(R.string.summary_location),
                    summary.location?.let { stringResource(R.string.summary_coordinates, it.latitude, it.longitude, it.accuracyM ?: 0.0) }
                        ?: stringResource(R.string.summary_no_location),
                )
            }
        }

        if (summary.recommendations.isNotEmpty()) {
            val title = when (summary.recommendations.first().disease) {
                ClassId.TIZON_TEMPRANO -> R.string.summary_recommendations_early
                ClassId.TIZON_TARDIO -> R.string.summary_recommendations_late
                ClassId.SANO -> R.string.summary_recommendations
            }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            summary.recommendations.forEach { rec ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(rec.text, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.summary_source, rec.source), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(Modifier.padding(top = Spacing.s), thickness = 0.5.dp)
    }
}

private fun durationMinutes(s: SessionSummary): Int? = runCatching {
    Duration.between(OffsetDateTime.parse(s.startedAt), OffsetDateTime.parse(s.endedAt ?: return null)).toMinutes().toInt()
}.getOrNull()

@TizonPreviews @Composable
private fun ResumenCriticoConAlertaPreview() = TizonTheme { SummaryScreen(PreviewData.criticalSummary, {}, {}) }

@TizonPreviews @Composable
private fun ResumenSanoPreview() = TizonTheme { SummaryScreen(PreviewData.healthySummary, {}, {}) }

@TizonPreviews @Composable
private fun DetalleSesionPreview() = TizonTheme { SessionDetailScreen(PreviewData.criticalSummary, {}) }
