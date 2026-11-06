package com.lfmm.tizonmovile.ui.scan

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.Region
import com.lfmm.tizonmovile.data.model.SessionStats
import com.lfmm.tizonmovile.data.model.Severity
import com.lfmm.tizonmovile.ui.components.ErrorBanner
import com.lfmm.tizonmovile.ui.components.PrimaryScanButton
import com.lfmm.tizonmovile.ui.components.RegionOverlay
import com.lfmm.tizonmovile.ui.components.ResultSheet
import com.lfmm.tizonmovile.ui.components.ScanStatus
import com.lfmm.tizonmovile.ui.components.StatusChip
import com.lfmm.tizonmovile.ui.preview.PreviewData
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.CameraBackdrop
import com.lfmm.tizonmovile.ui.theme.OnCameraBackdrop
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

data class ScanUiState(
    val status: ScanStatus = ScanStatus.READY,
    val isScanning: Boolean = false,
    val isPaused: Boolean = false,
    val fieldName: String? = null,
    val hasLocation: Boolean = false,
    val regions: List<Region> = emptyList(),
    val lastLabel: ClassId? = null,
    val lastConfidence: Double? = null,
    val stats: SessionStats? = null,
    /** Tamaño del frame analizado (ya rotado): base para mapear las cajas a la vista previa. */
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    /** Aviso puntual (p. ej. la sesión ya había terminado o no se pudo iniciar). */
    @param:StringRes val notice: Int? = null,
)

/**
 * Escanear: vista previa a pantalla completa con marco guía, chip de estado, finca e indicador de GPS
 * arriba; cajas sobre la imagen; tarjeta de resultado y botón principal abajo (zona del pulgar).
 * [camera] dibuja la vista previa real (CameraX); en los @Preview es un fondo oscuro.
 */
@Composable
fun ScanScreen(
    state: ScanUiState,
    onPrimary: () -> Unit,
    onTogglePause: () -> Unit,
    camera: @Composable (Modifier) -> Unit = { CameraPlaceholder(it) },
    initiallyExpanded: Boolean = true,
    onEditField: (() -> Unit)? = null,
) {
    var sheetExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    BoxWithConstraints(Modifier.fillMaxSize().background(CameraBackdrop)) {
        camera(Modifier.fillMaxSize())
        RegionOverlay(
            state.regions, Modifier.fillMaxSize(),
            toView = { region, view -> PreviewTransform.toView(region, state.frameWidth, state.frameHeight, view) },
        )
        if (state.regions.isEmpty()) GuideFrame(Modifier.align(Alignment.Center).padding(bottom = maxHeight / 4))

        TopIndicators(
            state, onEditField.takeIf { !state.isScanning },
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(Spacing.l),
        )

        // La tarjeta se desplaza si no cabe (fuente al 200 %); el botón principal siempre queda visible.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .heightIn(max = maxHeight * 0.75f).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (state.status == ScanStatus.SERVICE_UNAVAILABLE) ErrorBanner(stringResource(R.string.status_unavailable_retry))
                if (state.status == ScanStatus.OFFLINE) ErrorBanner(stringResource(R.string.status_offline))
                if (state.status == ScanStatus.NO_LEAF) ErrorBanner(stringResource(R.string.status_no_leaf_hint))
                if (state.status == ScanStatus.NOT_POTATO) ErrorBanner(stringResource(R.string.status_not_potato_hint))
                state.notice?.let { ErrorBanner(stringResource(it)) }
                if (state.isScanning) {
                    ResultSheet(
                        state.lastLabel, state.lastConfidence, state.stats,
                        expanded = sheetExpanded, onToggleExpanded = { sheetExpanded = !sheetExpanded },
                    )
                }
            }
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                PrimaryScanButton(state.isScanning, state.isPaused, onPrimary, onTogglePause, Modifier.padding(Spacing.s))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopIndicators(state: ScanUiState, onEditField: (() -> Unit)?, modifier: Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(state.status)
        // La finca o lote se puede cambiar antes de iniciar el escaneo.
        val fieldEditCd = stringResource(R.string.scan_field_edit_cd)
        Surface(
            onClick = { onEditField?.invoke() },
            enabled = onEditField != null,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.semantics { if (onEditField != null) contentDescription = fieldEditCd },
        ) {
            Row(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Filled.Agriculture, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(state.fieldName ?: stringResource(R.string.scan_field_none), style = MaterialTheme.typography.labelLarge)
            }
        }
        val gpsLabel = stringResource(if (state.hasLocation) R.string.scan_gps_on else R.string.scan_gps_off)
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.semantics { contentDescription = gpsLabel }) {
            Icon(
                if (state.hasLocation) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                contentDescription = null,
                tint = if (state.hasLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.s).size(24.dp),
            )
        }
    }
}

@Composable
private fun GuideFrame(modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Surface(
            modifier = Modifier.size(220.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = androidx.compose.ui.graphics.Color.Transparent,
            border = BorderStroke(3.dp, OnCameraBackdrop),
        ) {}
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
            Text(stringResource(R.string.scan_guide), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs))
        }
    }
}

@Composable
private fun CameraPlaceholder(modifier: Modifier) {
    val description = stringResource(R.string.scan_camera_cd)
    Box(modifier.clip(MaterialTheme.shapes.extraSmall).background(CameraBackdrop).semantics { contentDescription = description })
}

/** RF-04 CA6: al salir con un escaneo activo. */
@Composable
fun ScanExitDialog(onFinish: () -> Unit, onKeepScanning: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepScanning,
        title = { Text(stringResource(R.string.scan_exit_title)) },
        text = { Text(stringResource(R.string.scan_exit_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = onFinish) { Text(stringResource(R.string.scan_exit_finish)) } },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onKeepScanning) { Text(stringResource(R.string.scan_exit_keep)) }
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.scan_exit_discard), color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

// ── Previews: un estado por cada caso de RF-07 CA3 ────────────────────────

private val activeStats = PreviewData.stats(analyzed = 12, affected = 5, severity = Severity.CRITICAL)

@Composable
private fun Preview(state: ScanUiState) = TizonTheme { ScanScreen(state, {}, {}) }

@TizonPreviews @Composable
private fun EscanearListoPreview() = Preview(ScanUiState(fieldName = "Lote 3", hasLocation = true))

@TizonPreviews @Composable
private fun EscanearAnalizandoConCajasPreview() = Preview(
    ScanUiState(
        status = ScanStatus.ANALYZING, isScanning = true, fieldName = "Lote 3", hasLocation = true,
        regions = PreviewData.regions, lastLabel = ClassId.TIZON_TARDIO, lastConfidence = 0.92, stats = activeStats,
    ),
)

@TizonPreviews @Composable
private fun EscanearTarjetaPlegadaPreview() = TizonTheme {
    ScanScreen(
        ScanUiState(
            status = ScanStatus.ANALYZING, isScanning = true, fieldName = "Lote 3", hasLocation = true,
            regions = PreviewData.regions, lastLabel = ClassId.TIZON_TARDIO, lastConfidence = 0.92, stats = activeStats,
        ),
        {}, {}, initiallyExpanded = false,
    )
}

@TizonPreviews @Composable
private fun EscanearTizonTempranoPreview() = Preview(
    ScanUiState(
        status = ScanStatus.ANALYZING, isScanning = true, fieldName = "Lote 3", hasLocation = true,
        regions = PreviewData.earlyBlightRegions, lastLabel = ClassId.TIZON_TEMPRANO, lastConfidence = 0.88,
        stats = PreviewData.stats(analyzed = 12, affected = 4, severity = Severity.CRITICAL, early = 4),
    ),
)

@TizonPreviews @Composable
private fun EscanearNoEsPapaPreview() = Preview(
    ScanUiState(status = ScanStatus.NOT_POTATO, isScanning = true, fieldName = "Lote 3", stats = activeStats,
        lastLabel = ClassId.SANO, lastConfidence = 0.7),
)

@TizonPreviews @Composable
private fun EscanearSinHojaPreview() = Preview(
    ScanUiState(status = ScanStatus.NO_LEAF, isScanning = true, fieldName = "Lote 3", stats = activeStats,
        lastLabel = ClassId.SANO, lastConfidence = 0.7),
)

@TizonPreviews @Composable
private fun EscanearServicioNoDisponiblePreview() = Preview(
    ScanUiState(status = ScanStatus.SERVICE_UNAVAILABLE, isScanning = true, stats = activeStats),
)

@TizonPreviews @Composable
private fun EscanearSinConexionPreview() = Preview(
    ScanUiState(status = ScanStatus.OFFLINE, isScanning = true, isPaused = true, stats = activeStats),
)

@TizonPreviews @Composable
private fun EscanearDialogoSalidaPreview() = TizonTheme {
    ScanScreen(ScanUiState(status = ScanStatus.PAUSED, isScanning = true, isPaused = true, stats = activeStats), {}, {})
    ScanExitDialog({}, {}, {})
}
