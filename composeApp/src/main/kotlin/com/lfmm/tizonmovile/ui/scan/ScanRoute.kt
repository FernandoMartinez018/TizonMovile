package com.lfmm.tizonmovile.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.components.EmptyState

/** Conecta la pantalla Escanear con el ViewModel, la cámara y los permisos. */
@Composable
fun ScanRoute(vm: ScanViewModel, onFinished: (String) -> Unit, onExit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCamera by rememberSaveable {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    // Tras un rechazo, el botón lleva a los ajustes del sistema (Android ya no vuelve a preguntar).
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
        cameraDenied = !granted
    }
    var showExit by rememberSaveable { mutableStateOf(false) }
    var showField by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.events.collect { event -> if (event is ScanEvent.Finished) onFinished(event.sessionId) }
    }
    // Salir de la pestaña o mandar la app a segundo plano pausa el escaneo (la cámara se libera).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onLeaveScreen() }
    DisposableEffect(vm) { onDispose { vm.onLeaveScreen() } }
    BackHandler(enabled = state.isScanning) { showExit = true }

    // Al volver de los ajustes se revisa de nuevo el permiso.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasCamera) {
        // La app no se cierra si se rechaza la cámara (RNF-SEG-12, TX-7).
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EmptyState(
                icon = Icons.Filled.NoPhotography,
                title = stringResource(R.string.scan_camera_needed),
                body = stringResource(R.string.perm_camera_denied_body),
                actionLabel = stringResource(if (cameraDenied) R.string.action_open_settings else R.string.perm_allow),
                onAction = {
                    if (cameraDenied) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    } else {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            )
        }
        return
    }

    ScanScreen(
        state = state,
        onPrimary = vm::onPrimary,
        onTogglePause = vm::togglePause,
        camera = { modifier -> CameraPreview(modifier, wantsFrame = { vm.wantsFrame.value }, onFrame = vm::onFrame) },
        onEditField = { showField = true },
    )

    if (showExit) {
        ScanExitDialog(
            onFinish = { showExit = false; vm.finish() },
            onKeepScanning = { showExit = false },
            onDiscard = { showExit = false; vm.discard(); onExit() },
        )
    }
    if (showField) {
        FieldNameDialog(
            initial = state.fieldName.orEmpty(),
            onSave = { showField = false; vm.setFieldName(it) },
            onDismiss = { showField = false },
        )
    }
}

/** RF-10 CA4: nombre opcional de la finca o lote; se recuerda el último usado. */
@Composable
private fun FieldNameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_field_dialog_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 120) value = it },
                label = { Text(stringResource(R.string.scan_field_hint)) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.scan_field_dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
