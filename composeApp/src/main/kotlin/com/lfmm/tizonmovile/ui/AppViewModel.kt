package com.lfmm.tizonmovile.ui

import androidx.lifecycle.ViewModel
import com.lfmm.tizonmovile.data.auth.AuthState
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.settings.UserPreferences
import kotlinx.coroutines.flow.StateFlow

/** Destino inicial y estado de autenticación para la navegación raíz. */
class AppViewModel(private val session: SessionManager, private val prefs: UserPreferences) : ViewModel() {

    enum class Start { WELCOME, AUTH, MAIN }

    val authState: StateFlow<AuthState> = session.state

    /** Bienvenida y permisos solo la primera vez; luego login o pantalla principal (RF-02 CA3). */
    val start: Start = when {
        !prefs.welcomeSeen -> Start.WELCOME
        session.state.value is AuthState.LoggedIn -> Start.MAIN
        else -> Start.AUTH
    }

    fun onWelcomeDone() {
        prefs.welcomeSeen = true
    }

    fun isLoggedIn() = session.state.value is AuthState.LoggedIn
}
