package com.lfmm.tizonmovile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

/**
 * Actividad principal de Android.
 *
 * Responsabilidades:
 * 1. Solicitar permiso de CÁMARA al usuario si no está concedido.
 * 2. Mostrar la UI de Compose (App).
 *
 * El permiso de cámara se solicita al iniciar porque CameraPreviewContainer
 * lo necesita desde el momento en que la pantalla de cámara se muestra.
 * Si el usuario rechaza el permiso, la app cierra (comportamiento simple
 * para esta versión — en producción mostrar un diálogo explicativo).
 */
class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) {
            // En producción: mostrar diálogo explicativo con rationale
            // Por ahora cerramos la app si el permiso es rechazado
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya tenemos el permiso
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // Solicitar si no está concedido
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            // La UI se muestra independientemente del permiso de cámara.
            // CameraPreviewContainer manejará el caso sin permiso mostrando
            // una pantalla negra (CameraX lanzará excepción capturada).
            App()
        }
    }
}