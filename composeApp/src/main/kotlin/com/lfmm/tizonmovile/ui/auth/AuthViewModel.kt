package com.lfmm.tizonmovile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.auth.AuthState
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Login y registro (RF-01, RF-02). Al iniciar sesión, la navegación reacciona al AuthState. */
class AuthViewModel(private val auth: AuthRepository, session: SessionManager) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(error = if (session.state.value == AuthState.Expired) AuthError.SESSION_EXPIRED else null),
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = edit("username") { copy(username = v) }
    fun onPasswordChange(v: String) = edit("password") { copy(password = v) }
    fun onFullNameChange(v: String) = edit("full_name") { copy(fullName = v) }
    fun onTogglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onSwitchMode() = _state.update {
        AuthUiState(mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN, username = it.username)
    }

    fun onSubmit() {
        val s = _state.value
        if (s.isLoading) return
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = if (s.mode == AuthMode.LOGIN) auth.login(s.username, s.password)
            else auth.register(s.username, s.password, s.fullName)
            result
                .onSuccess { _state.update { it.copy(isLoading = false, password = "") } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.toAuthError(),
                            fieldErrors = (e as? AppError.Validation)?.fields?.keys?.associateWith(::fieldMessage).orEmpty(),
                        )
                    }
                }
        }
    }

    private fun edit(field: String, change: AuthUiState.() -> AuthUiState) =
        _state.update { it.change().copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun validate(s: AuthUiState): Map<String, Int> = buildMap {
        if (s.username.isBlank()) put("username", R.string.error_required)
        if (s.password.isBlank()) put("password", R.string.error_required)
        if (s.mode == AuthMode.REGISTER) {
            if (s.fullName.trim().length < 2) put("full_name", R.string.error_required)
            if (s.username.isNotBlank() && !USERNAME.matches(s.username.trim())) put("username", R.string.error_username_format)
            if (s.password.isNotBlank() && s.password.length !in 8..72) put("password", R.string.error_password_short)
        }
    }

    private fun fieldMessage(field: String): Int = when (field) {
        "password" -> R.string.error_password_short
        "username" -> R.string.error_username_format
        else -> R.string.error_required
    }

    private fun Throwable.toAuthError(): AuthError? = when (this) {
        AppError.Unauthorized -> AuthError.BAD_CREDENTIALS
        AppError.Network -> AuthError.OFFLINE
        AppError.UsernameTaken -> AuthError.USERNAME_TAKEN
        AppError.TooManyAttempts -> AuthError.TOO_MANY_ATTEMPTS
        is AppError.Validation -> null   // se muestra en cada campo
        else -> AuthError.SERVER
    }

    private companion object {
        /** Mismo patrón que el contrato (RegisterRequest.username). */
        val USERNAME = Regex("^[a-zA-Z0-9._-]{4,40}$")
    }
}
