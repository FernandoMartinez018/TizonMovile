package com.lfmm.tizonmovile.data.auth

import com.lfmm.tizonmovile.data.model.LoginResponse
import com.lfmm.tizonmovile.fakes.InMemoryTokenStore
import com.lfmm.tizonmovile.fakes.testUser
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-02 CA3–CA5: sesión persistida, vencimiento, cierre y expiración por 401. */
class SessionManagerTest {

    private val now = Instant.parse("2026-11-03T15:00:00Z")

    @Test
    fun sesionVigenteEntraDirecto() {
        val store = InMemoryTokenStore(StoredSession("t", "2026-11-04T10:00:00-05:00", testUser))
        val session = SessionManager(store) { now }
        assertEquals(AuthState.LoggedIn(testUser), session.state.value)
        assertEquals("t", session.token())
    }

    @Test
    fun sesionVencidaSeDescarta() {
        val store = InMemoryTokenStore(StoredSession("t", "2026-11-03T09:00:00-05:00", testUser))
        val session = SessionManager(store) { now }
        assertEquals(AuthState.LoggedOut, session.state.value)
        assertNull(store.session)
        assertNull(session.token())
    }

    @Test
    fun loginGuardaYLogoutBorra() {
        val store = InMemoryTokenStore()
        val session = SessionManager(store) { now }
        session.onLogin(LoginResponse("nuevo", "2026-11-04T10:00:00-05:00", testUser))
        assertEquals("nuevo", store.session?.token)
        assertEquals(AuthState.LoggedIn(testUser), session.state.value)
        session.logout()
        assertNull(store.session)
        assertEquals(AuthState.LoggedOut, session.state.value)
    }

    @Test
    fun un401ConSesionLaExpira() {
        val store = InMemoryTokenStore(StoredSession("t", "2026-11-04T10:00:00-05:00", testUser))
        val session = SessionManager(store) { now }
        session.expire()
        assertEquals(AuthState.Expired, session.state.value)
        assertNull(store.session)
    }

    @Test
    fun un401SinSesionNoCambiaNada() {
        val session = SessionManager(InMemoryTokenStore()) { now }
        session.expire()
        assertEquals(AuthState.LoggedOut, session.state.value)
    }
}
