package com.lfmm.tizonmovile.data.auth

import com.lfmm.tizonmovile.data.model.LoginResponse
import com.lfmm.tizonmovile.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.OffsetDateTime

sealed interface AuthState {
    data object LoggedOut : AuthState
    data class LoggedIn(val user: User) : AuthState
    /** Un 401 con sesión iniciada: se muestra "Tu sesión expiró, inicia sesión de nuevo" (RF-02 CA5). */
    data object Expired : AuthState
}

/** Estado de autenticación de la app. Sin dependencias de Android (se prueba en JVM). */
class SessionManager(
    private val store: TokenStore,
    private val now: () -> Instant = Instant::now,
) {
    @Volatile
    private var current: StoredSession? = store.load()?.takeIf { it.isValid() } ?: run { store.clear(); null }

    private val _state = MutableStateFlow<AuthState>(current?.let { AuthState.LoggedIn(it.user) } ?: AuthState.LoggedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun token(): String? = current?.token

    fun user(): User? = current?.user

    fun onLogin(response: LoginResponse) {
        val session = StoredSession(response.token, response.expiresAt, response.user)
        store.save(session)
        current = session
        _state.value = AuthState.LoggedIn(response.user)
    }

    fun logout() {
        store.clear()
        current = null
        _state.value = AuthState.LoggedOut
    }

    /** Invocado ante cualquier 401 de una petición autenticada. */
    fun expire() {
        if (current == null) return
        store.clear()
        current = null
        _state.value = AuthState.Expired
    }

    /** Al reabrir la app antes de expires_at se entra directo (RF-02 CA3). */
    private fun StoredSession.isValid(): Boolean =
        runCatching { OffsetDateTime.parse(expiresAt).toInstant().isAfter(now()) }.getOrDefault(false)
}
