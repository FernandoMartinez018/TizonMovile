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

/** F2-MOB-01: mapeo de las respuestas de error del contrato a [AppError] (MockEngine de Ktor). */
class TizonApiClientTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode, body: String = ""): TizonApiClient =
        TizonApiClient("http://api.test/api/", MockEngine { request ->
            requests += request
            respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, "application/json"))
        })

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
    }

    @Test
    fun loginConCredencialesInvalidasEsUnauthorized() = runTest {
        val result = client(HttpStatusCode.Unauthorized, error(401, "BAD_CREDENTIALS")).login("u", "p")
        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
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
        val api = TizonApiClient("http://api.test/api/", MockEngine { throw IOException("sin red") })
        assertEquals(AppError.Network, api.config().exceptionOrNull())
    }

    @Test
    fun enviaUnSoloTokenYUnRequestIdPorPeticion() = runTest {
        val login = javaClass.getResource("/fixtures/login_response.json")!!.readText()
        val api = client(HttpStatusCode.OK, login)
        assertNull(api.login("agricultor1", "clave-segura").exceptionOrNull())
        api.login("agricultor1", "clave-segura")

        val second = requests.last()
        assertEquals("http://api.test/api/auth/login", second.url.toString())
        assertEquals(listOf("Bearer eyJhbGciOiJIUzI1NiJ9.e30.x"), second.headers.getAll(HttpHeaders.Authorization))
        val ids = requests.map { it.headers["X-Request-Id"] }
        assertNotNull(ids[0])
        assertNotEquals(ids[0], ids[1])
    }
}
