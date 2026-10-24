package com.lfmm.tizonmovile.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.components.EmptyState
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

enum class PermissionStatus { NOT_REQUESTED, GRANTED, DENIED }

data class PermissionsUiState(
    val camera: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val location: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val notifications: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    /** POST_NOTIFICATIONS solo existe en Android 13+. */
    val askNotifications: Boolean = true,
)

/**
 * Explica cada permiso antes de pedirlo al sistema. La cámara es obligatoria; si se rechaza se
 * muestra cómo activarla, pero la app no se cierra (RNF-SEG-12, TX-7).
 */
@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (state.camera == PermissionStatus.DENIED) {
            EmptyState(
                icon = Icons.Filled.NoPhotography,
                title = stringResource(R.string.perm_camera_denied_title),
                body = stringResource(R.string.perm_camera_denied_body),
                actionLabel = stringResource(R.string.action_open_settings),
                onAction = onOpenSettings,
            )
        } else {
            PermissionList(state, onRequestCamera, onRequestLocation, onRequestNotifications, onContinue)
        }
    }
}

@Composable
private fun PermissionList(
    state: PermissionsUiState,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.perm_intro), style = MaterialTheme.typography.bodyLarge)
        PermissionCard(Icons.Filled.CameraAlt, R.string.perm_camera_title, R.string.perm_camera_body, state.camera, onRequestCamera)
        PermissionCard(Icons.Filled.LocationOn, R.string.perm_location_title, R.string.perm_location_body, state.location, onRequestLocation)
        if (state.askNotifications) {
            PermissionCard(Icons.Filled.Notifications, R.string.perm_notifications_title, R.string.perm_notifications_body,
                state.notifications, onRequestNotifications)
        }
        Button(
            onClick = onContinue,
            enabled = state.camera == PermissionStatus.GRANTED,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.scanButton),
            shape = MaterialTheme.shapes.extraLarge,
        ) { Text(stringResource(R.string.action_start), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: Int, body: Int, status: PermissionStatus, onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                if (status == PermissionStatus.GRANTED) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.perm_granted), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start)
                    }
                } else {
                    OutlinedButton(onClick = onRequest, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                        Text(stringResource(R.string.perm_allow))
                    }
                }
            }
        }
    }
}

@TizonPreviews
@Composable
private fun PermisosInicialPreview() = TizonTheme {
    PermissionsScreen(PermissionsUiState(), {}, {}, {}, {}, {})
}

@TizonPreviews
@Composable
private fun PermisosCamaraConcedidaPreview() = TizonTheme {
    PermissionsScreen(PermissionsUiState(camera = PermissionStatus.GRANTED), {}, {}, {}, {}, {})
}

@TizonPreviews
@Composable
private fun PermisosCamaraRechazadaPreview() = TizonTheme {
    PermissionsScreen(PermissionsUiState(camera = PermissionStatus.DENIED), {}, {}, {}, {}, {})
}
