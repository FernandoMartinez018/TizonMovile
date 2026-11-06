package com.lfmm.tizonmovile.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.components.ErrorBanner
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme

enum class AuthMode { LOGIN, REGISTER }

/** Errores visibles del login/registro (06-ux-ui §3). */
enum class AuthError(@param:StringRes val message: Int) {
    BAD_CREDENTIALS(R.string.error_bad_credentials),
    OFFLINE(R.string.error_offline),
    SERVER(R.string.error_server),
    SESSION_EXPIRED(R.string.error_session_expired),
    USERNAME_TAKEN(R.string.error_username_taken),
    TOO_MANY_ATTEMPTS(R.string.error_too_many_attempts),
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    /** Errores por campo ("username", "password", "full_name") como recursos de texto. */
    val fieldErrors: Map<String, Int> = emptyMap(),
)

@Composable
fun AuthScreen(
    state: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onSwitchMode: () -> Unit,
) {
    val isRegister = state.mode == AuthMode.REGISTER
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(if (isRegister) R.string.auth_register_title else R.string.auth_login_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        state.error?.let { ErrorBanner(stringResource(it.message)) }

        if (isRegister) {
            Field(state.fullName, onFullNameChange, R.string.auth_full_name, state.fieldErrors["full_name"])
        }
        Field(state.username, onUsernameChange, R.string.auth_username, state.fieldErrors["username"])
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            isError = state.fieldErrors["password"] != null,
            supportingText = state.fieldErrors["password"]?.let { { Text(stringResource(it)) } },
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (state.passwordVisible) R.string.auth_hide_password else R.string.auth_show_password),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.scanButton),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(if (isRegister) R.string.auth_register else R.string.auth_login), style = MaterialTheme.typography.titleMedium)
            }
        }
        TextButton(onClick = onSwitchMode, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(if (isRegister) R.string.auth_to_login else R.string.auth_to_register), style = MaterialTheme.typography.labelLarge)
        }
    }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, @StringRes label: Int, @StringRes error: Int?) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Preview(state: AuthUiState) = TizonTheme { AuthScreen(state, {}, {}, {}, {}, {}, {}) }

@TizonPreviews @Composable
private fun LoginVacioPreview() = Preview(AuthUiState())

@TizonPreviews @Composable
private fun LoginCredencialesIncorrectasPreview() =
    Preview(AuthUiState(username = "ana.perez", password = "123456789", error = AuthError.BAD_CREDENTIALS))

@TizonPreviews @Composable
private fun LoginSinConexionPreview() =
    Preview(AuthUiState(username = "ana.perez", password = "123456789", error = AuthError.OFFLINE))

@TizonPreviews @Composable
private fun LoginCargandoPreview() =
    Preview(AuthUiState(username = "ana.perez", password = "123456789", isLoading = true))

@TizonPreviews @Composable
private fun RegistroConErroresPreview() = Preview(
    AuthUiState(
        mode = AuthMode.REGISTER, username = "an", password = "123", fullName = "",
        fieldErrors = mapOf(
            "full_name" to R.string.error_required,
            "username" to R.string.error_username_format,
            "password" to R.string.error_password_short,
        ),
    ),
)
