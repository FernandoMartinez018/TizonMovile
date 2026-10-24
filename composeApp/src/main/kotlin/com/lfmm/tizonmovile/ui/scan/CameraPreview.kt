package com.lfmm.tizonmovile.ui.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Vista previa (FILL_CENTER) + análisis de CameraX. El analizador solo codifica un frame cuando
 * [wantsFrame] es true (un envío a la vez, RF-05 CA3); el resto se descarta sin costo.
 */
@Composable
fun CameraPreview(modifier: Modifier, wantsFrame: () -> Boolean, onFrame: (EncodedFrame) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            // Misma relación de aspecto en vista previa y análisis: el recorte de FILL_CENTER coincide.
            val aspect = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            val preview = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspect).build())
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(aspect)
                        .setResolutionStrategy(ResolutionStrategy(Size(960, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analysis.setAnalyzer(executor) { image ->
                image.use {
                    if (wantsFrame()) {
                        runCatching { FrameEncoder.encode(it) }
                            .onSuccess(onFrame)
                            .onFailure { e -> Log.w("CameraPreview", "No se pudo codificar el frame", e) }
                    }
                }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { Log.e("CameraPreview", "No se pudo abrir la cámara", it) }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** RF-05 CA1/CA2: Bitmap correcto (strides), rotado a la orientación de pantalla, lado mayor 640 px, JPEG 80. */
object FrameEncoder {
    const val MAX_SIDE = 640
    const val JPEG_QUALITY = 80

    /** Tamaño final conservando la proporción; no agranda imágenes pequeñas. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toFloat() / longest
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    fun encode(image: ImageProxy): EncodedFrame {
        val source = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val (targetW, _) = targetSize(source.width, source.height)
        val scale = targetW.toFloat() / source.width
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val frame = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val bytes = ByteArrayOutputStream().use { out ->
            frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        val encoded = EncodedFrame(Base64.encodeToString(bytes, Base64.NO_WRAP), frame.width, frame.height)
        if (frame !== source) frame.recycle()
        source.recycle()
        return encoded
    }
}
