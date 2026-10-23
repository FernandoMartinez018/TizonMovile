package com.lfmm.tizonmovile.ui.components

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Implementación Android: CameraX + ImageAnalysis → JPEG → Base64.
 * Preview siempre activo. Análisis solo cuando isCapturing = true.
 * Resolución de análisis 224×224. JPEG quality 70%.
 */
@Composable
fun CameraPreviewContainer(
    modifier: Modifier,
    onNewFrame: (frameB64: String) -> Unit,
    isCapturing: Boolean
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(isCapturing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            provider.unbindAll()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val useCases = mutableListOf<UseCase>(preview)

            if (isCapturing) {
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(224, 224))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(analysisExecutor) { imageProxy ->
                            processFrame(imageProxy, onNewFrame)
                        }
                    }
                useCases.add(analysis)
            }

            try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray()
                )
            } catch (ex: Exception) { ex.printStackTrace() }

        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }
    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun processFrame(imageProxy: ImageProxy, onNewFrame: (String) -> Unit) {
    try {
        if (imageProxy.format != ImageFormat.YUV_420_888) return

        val yBuf = imageProxy.planes[0].buffer
        val uBuf = imageProxy.planes[1].buffer
        val vBuf = imageProxy.planes[2].buffer
        val nv21 = ByteArray(yBuf.remaining() + uBuf.remaining() + vBuf.remaining())
        yBuf.get(nv21, 0, yBuf.remaining())
        val ySize = imageProxy.planes[0].buffer.capacity()
        vBuf.rewind(); uBuf.rewind()
        vBuf.get(nv21, ySize, vBuf.remaining())
        uBuf.get(nv21, ySize + vBuf.capacity(), uBuf.remaining())

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 70, out)
        onNewFrame(Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
    } catch (ex: Exception) {
        ex.printStackTrace()
    } finally {
        imageProxy.close()
    }
}