package com.lfmm.tizonmovile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfmm.tizonmovile.data.model.CameraUiState
import com.lfmm.tizonmovile.ui.components.BoundingBoxOverlay
import com.lfmm.tizonmovile.ui.components.CameraPreviewContainer
import com.lfmm.tizonmovile.ui.components.SeverityBar
import com.lfmm.tizonmovile.ui.theme.TizonColors
import com.lfmm.tizonmovile.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToReport: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.startSession() }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = TizonColors.BgDark,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CameraTopBar(onNavigateToReport = onNavigateToReport)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                CameraPreviewContainer(
                    modifier    = Modifier.fillMaxSize(),
                    onNewFrame  = { frameB64 -> viewModel.onNewFrame(frameB64) },
                    isCapturing = state.isActive
                )

                state.currentResult?.boundingBoxes?.let { bboxes ->
                    if (bboxes.isNotEmpty()) {
                        BoundingBoxOverlay(
                            modifier      = Modifier.fillMaxSize(),
                            boundingBoxes = bboxes
                        )
                    }
                }

                StatusBadge(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    state    = state
                )

                if (state.isAnalyzing) {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color      = TizonColors.GreenPrimary,
                        trackColor = Color.Transparent
                    )
                }

                CameraToggleButton(
                    modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    isActive  = state.isActive,
                    onClick   = { viewModel.toggleCamera() }
                )
            }

            MetricsPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                state = state
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CameraTopBar(onNavigateToReport: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🌿", fontSize = 24.sp)
            Column {
                Text("Análisis de Imagen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TizonColors.TextPrimary)
                Text("Cultivos de Papa",   fontSize = 12.sp, color = TizonColors.GreenPrimary)
            }
        }
        IconButton(onClick = onNavigateToReport) { Text("📋", fontSize = 20.sp) }
    }
}

@Composable
private fun StatusBadge(modifier: Modifier = Modifier, state: CameraUiState) {
    val result = state.currentResult
    val (bgColor, icon, text) = when {
        !state.isActive                    -> Triple(TizonColors.TextMuted,    "⏸", "Pausado")
        state.isAnalyzing                  -> Triple(TizonColors.OrangeWarn,   "⏳", "Analizando…")
        result == null                     -> Triple(TizonColors.GreenPrimary, "📷", "Listo")
        result.label == "Tizon Tardio"    -> Triple(TizonColors.RedDanger,    "⚠", "Tizón Tardío")
        result.label == "Tizon Temprano"  -> Triple(TizonColors.OrangeWarn,   "⚠", "Tizón Temprano")
        result.label == "Sano"            -> Triple(TizonColors.GreenPrimary, "✓", "Sano")
        else                               -> Triple(TizonColors.TextMuted,    "—", "Sin detección")
    }

    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = bgColor.copy(alpha = 0.20f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(icon, fontSize = 12.sp)
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = bgColor)

            result?.let {
                val pct = (it.confidencePct * 10).toInt() / 10.0
                Text("$pct%", fontSize = 11.sp, color = bgColor.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun CameraToggleButton(modifier: Modifier = Modifier, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick         = onClick,
        modifier        = modifier.size(64.dp),
        shape           = CircleShape,
        color           = if (isActive) TizonColors.RedDanger else TizonColors.GreenPrimary,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (isActive) "⏹" else "▶", fontSize = 26.sp)
        }
    }
}

@Composable
private fun MetricsPanel(modifier: Modifier = Modifier, state: CameraUiState) {
    val pct = (state.affectionPct * 10).toInt() / 10.0

    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = TizonColors.BgCard) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Porcentaje afectado", fontSize = 12.sp, color = TizonColors.TextMuted)
                    Text(
                        text       = "$pct%",
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TizonColors.severityColor(state.affectionPct)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${state.framesAnalyzed}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TizonColors.TextPrimary)
                    Text("frames analizados", fontSize = 10.sp, color = TizonColors.TextMuted)
                }
            }

            SeverityBar(affectionPct = state.affectionPct)

            AnimatedVisibility(visible = state.currentResult != null) {
                state.currentResult?.let { result ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(color = TizonColors.BorderColor)
                        Text("Último frame:", fontSize = 12.sp, color = TizonColors.TextMuted)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            result.probabilities.forEach { (label, prob) ->
                                val pctProb = (prob * 1000).toInt() / 10.0

                                val color = when (label) {
                                    "Sano"           -> TizonColors.GreenPrimary
                                    "Tizon Temprano" -> TizonColors.OrangeWarn
                                    "Tizon Tardio"   -> TizonColors.RedDanger
                                    else             -> TizonColors.TextMuted
                                }

                                val shortLabel = when (label) {
                                    "Tizon Temprano" -> "Temp."
                                    "Tizon Tardio"   -> "Tardío"
                                    else             -> label
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$pctProb%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                                    Text(shortLabel, fontSize = 10.sp, color = TizonColors.TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
