package com.lfmm.tizonmovile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.SessionStats
import com.lfmm.tizonmovile.ui.certaintyRes
import com.lfmm.tizonmovile.ui.color
import com.lfmm.tizonmovile.ui.formatPct
import com.lfmm.tizonmovile.ui.icon
import com.lfmm.tizonmovile.ui.labelRes
import com.lfmm.tizonmovile.ui.theme.AffectionNumberStyle
import com.lfmm.tizonmovile.ui.theme.Spacing

/**
 * Tarjeta inferior plegable del escaneo: diagnóstico del último frame en texto grande, certeza en
 * lenguaje simple, % afectado de la sesión con su severidad y número de hojas analizadas.
 * Plegada muestra solo el diagnóstico y el % para dejar ver más la cámara.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultSheet(
    lastLabel: ClassId?,
    lastConfidence: Double?,
    stats: SessionStats?,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.scan_last_result),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onToggleExpanded != null) {
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = stringResource(if (expanded) R.string.scan_sheet_collapse else R.string.scan_sheet_expand),
                        )
                    }
                }
            }

            if (lastLabel == null) {
                Text(stringResource(R.string.scan_no_result_yet), style = MaterialTheme.typography.bodyLarge)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(lastLabel.icon(), contentDescription = null, tint = lastLabel.color(), modifier = Modifier.size(32.dp))
                    Text(stringResource(lastLabel.labelRes()), style = MaterialTheme.typography.headlineSmall, color = lastLabel.color())
                    if (!expanded && stats != null) {
                        Text(
                            stringResource(R.string.summary_affected_pct, formatPct(stats.affectionPct)),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (lastLabel != null && lastConfidence != null) {
                        Text(
                            stringResource(R.string.certainty_label, stringResource(certaintyRes(lastConfidence))),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (stats != null) {
                        HorizontalDivider()
                        Text(stringResource(R.string.summary_affected_pct, formatPct(stats.affectionPct)), style = AffectionNumberStyle)
                        Text(
                            stringResource(R.string.scan_affected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SeverityMeter(stats.affectionPct, stats.severity)
                        Text(
                            pluralStringResource(R.plurals.scan_leaves_analyzed, stats.analyzedFrames, stats.analyzedFrames),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
