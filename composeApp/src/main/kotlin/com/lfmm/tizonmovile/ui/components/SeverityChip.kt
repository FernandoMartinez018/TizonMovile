package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.Severity
import com.lfmm.tizonmovile.ui.color
import com.lfmm.tizonmovile.ui.icon
import com.lfmm.tizonmovile.ui.labelRes
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

/** Severidad con icono + texto + color. [filled] usa el color de fondo (encabezados de resumen). */
@Composable
fun SeverityChip(severity: Severity, modifier: Modifier = Modifier, filled: Boolean = false) {
    val color = severity.color()
    val label = stringResource(severity.labelRes())
    val description = stringResource(R.string.severity_cd, label)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = if (filled) color else MaterialTheme.colorScheme.surface,
        contentColor = if (filled) TizonTheme.severity.onSeverity else color,
        border = if (filled) null else BorderStroke(1.5.dp, color),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(severity.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Barra de % afectado coloreada por severidad, más el chip con el texto. */
@Composable
fun SeverityMeter(affectionPct: Double, severity: Severity, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        LinearProgressIndicator(
            progress = { (affectionPct / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = severity.color(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        SeverityChip(severity)
    }
}
