package com.lfmm.tizonmovile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfmm.tizonmovile.data.model.ClassId
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.ui.theme.TizonColors
import com.lfmm.tizonmovile.viewmodel.CameraViewModel

@Composable
fun ReportScreen(viewModel: CameraViewModel, onBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    val state   by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(modifier = Modifier.fillMaxSize().background(TizonColors.BgDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("← Volver", color = TizonColors.GreenPrimary, fontSize = 14.sp) }
            Text("Reporte de Escaneo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TizonColors.TextPrimary)
            Text("🔔", fontSize = 20.sp)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ActiveSessionCard(
                    framesAnalyzed = state.framesAnalyzed,
                    affectionPct   = state.affectionPct,
                    dominantLabel  = state.currentResult?.label?.let { if (it == ClassId.SANO) "Sano" else "Tizón tardío" } ?: "—",
                    onEndSession   = { viewModel.endSession() }
                )
            }
            item { RecommendationsCard(affectionPct = state.affectionPct) }
            if (history.isNotEmpty()) {
                item {
                    Text(
                        "Historial de sesiones",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TizonColors.TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(history) { session -> HistoryCard(session) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    framesAnalyzed: Int,
    affectionPct: Float,
    dominantLabel: String,
    onEndSession: () -> Unit
) {
    val affColor = TizonColors.severityColor(affectionPct)
    val pct = (affectionPct * 10).toInt() / 10.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TizonColors.BgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("% Afectacion Total", fontSize = 12.sp, color = TizonColors.TextMuted)
                    Text("$pct%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = affColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sesion activa", fontSize = 11.sp, color = TizonColors.GreenPrimary)
                    Text("$framesAnalyzed", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TizonColors.TextPrimary)
                    Text("frames", fontSize = 10.sp, color = TizonColors.TextMuted)
                }
            }

            LinearProgressIndicator(
                progress = { (affectionPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = affColor,
                trackColor = TizonColors.BorderColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Diagnostico dominante", fontSize = 11.sp, color = TizonColors.TextMuted)
                    Text(dominantLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TizonColors.TextPrimary)
                }
                OutlinedButton(
                    onClick = onEndSession,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TizonColors.RedDanger)
                ) {
                    Text("Finalizar sesion", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCard(affectionPct: Float) {
    val recs = when {
        affectionPct > 30f -> listOf(
            "🚨" to "Aplicacion urgente de fungicida sistemico",
            "📅" to "Monitoreo diario requerido",
            "🔴" to "Aislar zonas criticas"
        )
        affectionPct > 10f -> listOf(
            "⚠️" to "Aplicacion focalizada de fungicida preventivo",
            "📅" to "Monitoreo cada 5 dias",
            "🌿" to "Revisar condiciones de humedad"
        )
        affectionPct > 0f -> listOf(
            "✅" to "Nivel bajo — monitoreo preventivo",
            "📅" to "Proximo escaneo en 7 dias"
        )
        else -> listOf(
            "✅" to "Cultivo saludable detectado",
            "📅" to "Revision periodica recomendada"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TizonColors.BgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recomendaciones", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TizonColors.TextPrimary)

            recs.forEach { (icon, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = TizonColors.GreenPrimary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 16.sp)
                        }
                    }
                    Text(text, fontSize = 13.sp, color = TizonColors.TextPrimary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(session: SessionSummary) {
    val affColor = TizonColors.severityColor(session.affectionPct.toFloat())
    val pct = (session.affectionPct * 10).toInt() / 10.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TizonColors.BgCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    session.dominantLabel?.let { if (it == ClassId.SANO) "Sano" else "Tizón tardío" } ?: "Sin diagnóstico",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TizonColors.TextPrimary
                )
                Text(
                    "${session.totalFrames} frames · ${session.status}",
                    fontSize = 11.sp,
                    color = TizonColors.TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$pct%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = affColor)
                Text("afectacion", fontSize = 10.sp, color = TizonColors.TextMuted)
            }
        }
    }
}
