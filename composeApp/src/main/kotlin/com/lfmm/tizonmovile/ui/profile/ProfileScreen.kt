package com.lfmm.tizonmovile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.model.Role
import com.lfmm.tizonmovile.data.model.User
import com.lfmm.tizonmovile.ui.labelRes
import com.lfmm.tizonmovile.ui.preview.PreviewData
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

data class ProfileUiState(
    val user: User,
    val appVersion: String,
    val modelVersion: String,
    val confirmLogout: Boolean = false,
)

/** Perfil: datos, "Cómo tomar buenas capturas", Acerca de (versiones), Usuarios (ADMIN) y cerrar sesión. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onLogoutClick: () -> Unit,
    onConfirmLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenTips: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(state.user.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(state.user.username, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(state.user.role.labelRes()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                if (state.user.role == Role.ADMIN) {
                    TextButton(onClick = onOpenUsers, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                        Icon(Icons.Filled.Group, contentDescription = null)
                        Text(stringResource(R.string.profile_users), modifier = Modifier.padding(start = Spacing.s))
                    }
                }
                TextButton(onClick = onOpenTips, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null)
                    Text(stringResource(R.string.profile_tips), modifier = Modifier.padding(start = Spacing.s))
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(R.string.profile_about), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.profile_app_version, state.appVersion), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.profile_model_version, state.modelVersion), style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text(stringResource(R.string.profile_logout), modifier = Modifier.padding(start = Spacing.s))
            }
        }
    }
    if (state.confirmLogout) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_body)) },
            confirmButton = { TextButton(onClick = onConfirmLogout) { Text(stringResource(R.string.profile_logout)) } },
            dismissButton = { TextButton(onClick = onDismissLogout) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Usuarios (ADMIN): interruptor de habilitado y selector de rol. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    users: List<User>,
    currentUsername: String,
    onRoleChange: (User, Role) -> Unit,
    onEnabledChange: (User, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.users_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back)) } },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            items(users, key = { it.id }) { user ->
                // Un ADMIN no se modifica a sí mismo (la API lo rechaza).
                val editable = user.username != currentUsername
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(user.username, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            Text(stringResource(R.string.users_role), style = MaterialTheme.typography.labelLarge)
                            RoleSelector(user.role, editable) { onRoleChange(user, it) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.users_enabled), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Switch(checked = user.enabled, onCheckedChange = { onEnabledChange(user, it) }, enabled = editable)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSelector(role: Role, enabled: Boolean, onSelect: (Role) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
        Text(stringResource(role.labelRes()))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Role.entries.forEach { r ->
            DropdownMenuItem(text = { Text(stringResource(r.labelRes())) }, onClick = { open = false; onSelect(r) })
        }
    }
}

@TizonPreviews @Composable
private fun PerfilAgricultorPreview() = TizonTheme {
    ProfileScreen(ProfileUiState(PreviewData.agricultor, "1.1.0", "mobilenetv2-2c-v1-sin-calibrar"), {}, {}, {}, {}, {})
}

@TizonPreviews @Composable
private fun PerfilAdminConfirmarSalidaPreview() = TizonTheme {
    ProfileScreen(ProfileUiState(PreviewData.admin, "1.1.0", "mobilenetv2-2c-v1-sin-calibrar", confirmLogout = true), {}, {}, {}, {}, {})
}

@TizonPreviews @Composable
private fun UsuariosAdminPreview() = TizonTheme {
    UsersScreen(PreviewData.users, currentUsername = PreviewData.admin.username, { _, _ -> }, { _, _ -> }, {})
}
