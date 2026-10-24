package com.lfmm.tizonmovile.data.api

import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F2-MOB-01 / RF-02 CA5: mapeo de errores del contrato a [AppError] y manejo del 401 (MockEngine de Ktor). */
class TizonApiClientTest {

    private val requests = mutableListOf<HttpRequestData>()
    private var token: String? = null
    private var unauthorizedCalls = 0

    private fun client(status: HttpStatusCode, body: String = ""): TizonApiClient =
        TizonApiClient(
            "http://api.test/api/",
            tokenProvider = { token },
            onUnauthorized = { unauthorizedCalls++ },
            engine = MockEngine { request ->
                requests += request
                respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )

    private fun error(status: Int, code: String, fields: String = "") =
        """{"status":$status,"code":"$code","message":"msg","timestamp":"2026-11-03T10:15:00Z"$fields}"""

    private suspend fun frameError(status: HttpStatusCode, code: String): Throwable? =
        client(status, error(status.value, code)).analyzeFrame("s1", "x".repeat(120)).exceptionOrNull()

    @Test
    fun mapeaCadaCodigoDelContrato() = runTest {
        assertEquals(AppError.Unauthorized, frameError(HttpStatusCode.Unauthorized, "UNAUTHORIZED"))
        assertEquals(AppError.SessionNotActive, frameError(HttpStatusCode.Conflict, "SESSION_NOT_ACTIVE"))
        assertEquals(AppError.IaUnavailable, frameError(HttpStatusCode.BadGateway, "IA_UNAVAILABLE"))
        assertEquals(AppError.IaTimeout, frameError(HttpStatusCode.GatewayTimeout, "IA_TIMEOUT"))
        assertEquals(AppError.InvalidImage, frameError(HttpStatusCode.BadRequest, "INVALID_IMAGE"))
        assertEquals(AppError.Server, frameError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR"))
        assertEquals(AppError.TooManyAttempts, frameError(HttpStatusCode.TooManyRequests, "TOO_MANY_ATTEMPTS"))
    }

    @Test
    fun loginConCredencialesInvalidasEsUnauthorizedYNoCierraSesion() = runTest {
        val result = client(HttpStatusCode.Unauthorized, error(401, "BAD_CREDENTIALS")).login("u", "p")
        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun un401ConSesionAvisaQueElTokenExpiro() = runTest {
        token = "vencido"
        client(HttpStatusCode.Unauthorized, error(401, "UNAUTHORIZED")).sessions()
        assertEquals(1, unauthorizedCalls)
    }

    @Test
    fun validacionConservaLosCampos() = runTest {
        val body = error(400, "VALIDATION_ERROR", ""","fields":{"password":"Mínimo 8 caracteres."}""")
        val result = client(HttpStatusCode.BadRequest, body).login("u", "p")
        val validation = assertIs<AppError.Validation>(result.exceptionOrNull())
        assertEquals("Mínimo 8 caracteres.", validation.fields["password"])
    }

    @Test
    fun cuerpoNoJsonSeMapeaPorCodigoHttp() = runTest {
        val result = client(HttpStatusCode.BadGateway, "<html>bad gateway</html>").config()
        assertEquals(AppError.IaUnavailable, result.exceptionOrNull())
    }

    @Test
    fun sinRedEsNetwork() = runTest {
        val api = TizonApiClient("http://api.test/api/", engine = MockEngine { throw IOException("sin red") })
        assertEquals(AppError.Network, api.config().exceptionOrNull())
    }

    @Test
    fun enviaUnSoloTokenYUnRequestIdPorPeticion() = runTest {
        val login = javaClass.getResource("/fixtures/login_response.json")!!.readText()
        val api = client(HttpStatusCode.OK, login)
        val response = api.login("agricultor1", "clave-segura").getOrThrow()
        assertTrue(requests.single().headers[HttpHeaders.Authorization] == null, "el login no lleva token")

        token = response.token   // lo que hace el SessionManager al iniciar sesión
        api.me()

        val second = requests.last()
        assertEquals("http://api.test/api/auth/me", second.url.toString())
        assertEquals(listOf("Bearer eyJhbGciOiJIUzI1NiJ9.e30.x"), second.headers.getAll(HttpHeaders.Authorization))
        val ids = requests.map { it.headers["X-Request-Id"] }
        assertNotNull(ids[0])
        assertNotEquals(ids[0], ids[1])
    }
}
