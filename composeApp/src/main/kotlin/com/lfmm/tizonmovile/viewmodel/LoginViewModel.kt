package com.lfmm.tizonmovile.viewmodel

import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.model.LoginResponse
import com.lfmm.tizonmovile.data.model.UiState
import com.lfmm.tizonmovile.data.repository.AnalysisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de login.
 *
 * Gestiona el estado de la operación de autenticación y
 * expone un StateFlow que la UI observa.
 *
 * Ciclo de vida:
 *   - Se instancia en [di.AppModule] como singleton de sesión.
 *   - Se libera llamando a [dispose] cuando la app se cierra.
 */
class LoginViewModel(private val repository: AnalysisRepository) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _uiState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
    val uiState: StateFlow<UiState<LoginResponse>> = _uiState.asStateFlow()

    /**
     * Intenta autenticar al usuario con las credenciales proporcionadas.
     *
     * Validaciones locales antes de llamar a la API:
     * - username y password no pueden estar vacíos.
     *
     * En caso de error de red o credenciales inválidas, el estado
     * pasa a [UiState.Error] con un mensaje descriptivo.
     */
    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _uiState.value = UiState.Error("Ingresa tu usuario.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = UiState.Error("Ingresa tu contraseña.")
            return
        }

        scope.launch {
            _uiState.value = UiState.Loading

            repository.login(username, password)
                .onSuccess { response ->
                    _uiState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    val message = when (error) {
                        AppError.Unauthorized -> "Usuario o contraseña incorrectos"
                        AppError.Network -> "Sin conexión. Revisa tu internet"
                        else -> "El servicio no está disponible, intenta más tarde"
                    }
                    _uiState.value = UiState.Error(message)
                }
        }
    }

    /** Resetea el estado a Idle. Llamar después de navegar post-login. */
    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun dispose() = scope.cancel()
}