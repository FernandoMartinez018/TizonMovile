package com.lfmm.tizonmovile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.model.Role
import com.lfmm.tizonmovile.data.model.User
import com.lfmm.tizonmovile.data.repository.AdminRepository
import com.lfmm.tizonmovile.data.repository.AuthRepository
import com.lfmm.tizonmovile.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Perfil: datos del usuario, versiones de app y modelo, y cierre de sesión con confirmación (RF-02 CA4). */
class ProfileViewModel(
    session: SessionManager,
    private val auth: AuthRepository,
    scanRepo: ScanRepository,
    appVersion: String,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState?>(
        session.user()?.let { ProfileUiState(it, appVersion, modelVersion = "—") },
    )
    val state: StateFlow<ProfileUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            scanRepo.config().onSuccess { cfg -> _state.update { it?.copy(modelVersion = cfg.modelVersion) } }
        }
    }

    fun onLogoutClick() = _state.update { it?.copy(confirmLogout = true) }
    fun onDismissLogout() = _state.update { it?.copy(confirmLogout = false) }

    fun onConfirmLogout() {
        _state.update { it?.copy(confirmLogout = false) }
        auth.logout()   // la navegación reacciona al AuthState y vuelve al login
    }
}

/** Usuarios (ADMIN): cambiar rol y habilitación. */
class UsersViewModel(private val admin: AdminRepository, session: SessionManager) : ViewModel() {

    val currentUsername: String = session.user()?.username.orEmpty()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        viewModelScope.launch { admin.users().onSuccess { _users.value = it } }
    }

    fun changeRole(user: User, role: Role) = update(user.id) { admin.update(user.id, role = role) }

    fun changeEnabled(user: User, enabled: Boolean) = update(user.id) { admin.update(user.id, enabled = enabled) }

    private fun update(id: String, call: suspend () -> Result<User>) {
        viewModelScope.launch {
            call().onSuccess { updated -> _users.update { list -> list.map { if (it.id == id) updated else it } } }
        }
    }
}
