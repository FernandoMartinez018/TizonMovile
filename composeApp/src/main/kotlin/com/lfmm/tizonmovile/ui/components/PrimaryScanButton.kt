package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.theme.Spacing

/**
 * Botón principal (≥ 72 dp, zona del pulgar): "Iniciar escaneo" o "Finalizar", con la acción
 * secundaria "Pausar"/"Continuar" debajo mientras hay un escaneo activo.
 */
@Composable
fun PrimaryScanButton(
    isScanning: Boolean,
    isPaused: Boolean,
    onPrimary: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onPrimary,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.scanButton),
            shape = MaterialTheme.shapes.extraLarge,
            colors = if (isScanning) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Icon(
                if (isScanning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                stringResource(if (isScanning) R.string.scan_finish else R.string.scan_start),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.s),
            )
        }
        if (isScanning) {
            TextButton(
                onClick = onTogglePause,
                modifier = Modifier.heightIn(min = Spacing.touchTarget),
            ) {
                Icon(if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null)
                Text(
                    stringResource(if (isPaused) R.string.scan_resume else R.string.scan_pause),
                    modifier = Modifier.padding(start = Spacing.xs),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
