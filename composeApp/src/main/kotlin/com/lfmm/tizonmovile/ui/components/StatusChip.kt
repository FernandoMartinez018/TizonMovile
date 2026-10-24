package com.lfmm.tizonmovile.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

/** Estados visibles del escaneo (RF-07 CA3). */
enum class ScanStatus(@param:StringRes val label: Int) {
    READY(R.string.status_ready),
    ANALYZING(R.string.status_analyzing),
    NO_LEAF(R.string.status_no_leaf),
    SERVICE_UNAVAILABLE(R.string.status_unavailable),
    OFFLINE(R.string.status_offline),
    PAUSED(R.string.status_paused),
}

/** Chip opaco (legible sobre la imagen de la cámara) con icono + texto; anuncia los cambios al lector. */
@Composable
fun StatusChip(status: ScanStatus, modifier: Modifier = Modifier) {
    val (icon, tint) = visuals(status)
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (status == ScanStatus.ANALYZING) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(stringResource(status.label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun visuals(status: ScanStatus): Pair<ImageVector, Color> = when (status) {
    ScanStatus.READY -> Icons.Filled.CameraAlt to MaterialTheme.colorScheme.primary
    ScanStatus.ANALYZING -> Icons.Filled.CameraAlt to MaterialTheme.colorScheme.primary
    ScanStatus.NO_LEAF -> Icons.Filled.ImageSearch to MaterialTheme.colorScheme.onSurfaceVariant
    ScanStatus.SERVICE_UNAVAILABLE -> Icons.Filled.SyncProblem to TizonTheme.severity.warning
    ScanStatus.OFFLINE -> Icons.Filled.CloudOff to MaterialTheme.colorScheme.error
    ScanStatus.PAUSED -> Icons.Filled.PauseCircle to MaterialTheme.colorScheme.onSurfaceVariant
}
