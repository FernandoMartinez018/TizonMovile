package com.lfmm.tizonmovile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraPreviewContainer(
    modifier: Modifier,
    onNewFrame: (String) -> Unit,
    isCapturing: Boolean
) {
    Box(modifier = modifier) {
        Text("Cámara no disponible en Desktop")
    }
}
