package com.lfmm.tizonmovile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable
expect fun CameraPreviewContainer(
    modifier: Modifier = Modifier,
    onNewFrame: (frameB64: String) -> Unit,
    isCapturing: Boolean
)

