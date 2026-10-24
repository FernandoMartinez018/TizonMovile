package com.lfmm.tizonmovile.ui.auth

import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.auth.StoredSession
import com.lfmm.tizonmovile.fakes.FakeAuthRepository
import com.lfmm.tizonmovile.fakes.InMemoryTokenStore
import com.lfmm.tizonmovile.fakes.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F3-MOB-02: login y registro (éxito, 401, sin red, validación). */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeAuthRepository()
    private val session = SessionManager(InMemoryTokenStore())

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(username: String = "ana.perez", password: String = "clave-segura") =
        AuthViewModel(repo, session).apply { onUsernameChange(username); onPasswordChange(password) }

    @Test
    fun loginExitosoLimpiaLaContrasena() = runTest(dispatcher) {
        val vm = vm()
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(1, repo.logins)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun credencialesIncorrectas() = runTest(dispatcher) {
        repo.result = Result.failure(AppError.Unauthorized)
        val vm = vm()
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(AuthError.BAD_CREDENTIALS, vm.state.value.error)
    }

    @Test
    fun sinRed() = runTest(dispatcher) {
        repo.result = Result.failure(AppError.Network)
        val vm = vm()
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(AuthError.OFFLINE, vm.state.value.error)
    }

    @Test
    fun servidorCaidoYBloqueoPorIntentos() = runTest(dispatcher) {
        repo.result = Result.failure(AppError.Server)
        val vm = vm()
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(AuthError.SERVER, vm.state.value.error)
        repo.result = Result.failure(AppError.TooManyAttempts)
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(AuthError.TOO_MANY_ATTEMPTS, vm.state.value.error)
    }

    @Test
    fun camposVaciosNoLlamanALaApi() = runTest(dispatcher) {
        val vm = vm(username = "", password = "")
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(0, repo.logins)
        assertEquals(R.string.error_required, vm.state.value.fieldErrors["username"])
        assertEquals(R.string.error_required, vm.state.value.fieldErrors["password"])
    }

    @Test
    fun registroValidaUsuarioContrasenaYNombre() = runTest(dispatcher) {
        val vm = vm(username = "a!", password = "corta")
        vm.onSwitchMode()
        vm.onPasswordChange("corta")
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(0, repo.registrations)
        assertEquals(R.string.error_username_format, vm.state.value.fieldErrors["username"])
        assertEquals(R.string.error_password_short, vm.state.value.fieldErrors["password"])
        assertEquals(R.string.error_required, vm.state.value.fieldErrors["full_name"])

        vm.onFullNameChange("Ana Pérez")
        assertNull(vm.state.value.fieldErrors["full_name"], "editar un campo borra su error")
    }

    @Test
    fun registroConUsuarioExistente() = runTest(dispatcher) {
        repo.result = Result.failure(AppError.UsernameTaken)
        val vm = vm()
        vm.onSwitchMode()
        vm.onPasswordChange("clave-segura"); vm.onFullNameChange("Ana Pérez")
        vm.onSubmit(); advanceUntilIdle()
        assertEquals(1, repo.registrations)
        assertEquals(AuthError.USERNAME_TAKEN, vm.state.value.error)
    }

    @Test
    fun erroresDeValidacionDelServidorVanACadaCampo() = runTest(dispatcher) {
        repo.result = Result.failure(AppError.Validation(mapOf("password" to "Mínimo 8 caracteres.")))
        val vm = vm()
        vm.onSubmit(); advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(R.string.error_password_short, vm.state.value.fieldErrors["password"])
    }

    @Test
    fun sesionExpiradaMuestraElMensaje() = runTest(dispatcher) {
        val store = InMemoryTokenStore(StoredSession("t", "2099-01-01T00:00:00Z", testUser))
        val expired = SessionManager(store).apply { expire() }
        val vm = AuthViewModel(repo, expired)
        assertEquals(AuthError.SESSION_EXPIRED, vm.state.value.error)
        assertTrue(vm.state.value.mode == AuthMode.LOGIN)
    }
}
