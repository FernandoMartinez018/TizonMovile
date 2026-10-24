package com.lfmm.tizonmovile.data.api

import com.lfmm.tizonmovile.data.model.ApiErrorBody

/**
 * Errores que la UI sabe distinguir (RF-07 CA3). Se construyen a partir del cuerpo Error
 * del contrato; nunca a partir de textos libres.
 */
sealed class AppError(message: String? = null) : Exception(message) {
    /** Sin respuesta del servidor (sin red, DNS, conexión rechazada o timeout del cliente). */
    data object Network : AppError()

    /** 401: credenciales incorrectas, token ausente o expirado. */
    data object Unauthorized : AppError()

    /** 409 SESSION_NOT_ACTIVE: la sesión ya terminó; hay que iniciar una nueva. */
    data object SessionNotActive : AppError()

    /** 502 IA_UNAVAILABLE. Se reintenta con espera creciente. */
    data object IaUnavailable : AppError()

    /** 504 IA_TIMEOUT. Se reintenta con espera creciente. */
    data object IaTimeout : AppError()

    /** 400 INVALID_IMAGE: el frame no se pudo decodificar; se descarta sin molestar al usuario. */
    data object InvalidImage : AppError()

    /** 409 USERNAME_TAKEN en el registro. */
    data object UsernameTaken : AppError()

    /** 429 TOO_MANY_ATTEMPTS: 5 intentos fallidos de login bloquean el usuario 5 minutos. */
    data object TooManyAttempts : AppError()

    /** 400 VALIDATION_ERROR con el mensaje por campo (snake_case). */
    data class Validation(val fields: Map<String, String>) : AppError()

    /** 5xx del propio backend: "El servicio no está disponible, intenta más tarde". */
    data object Server : AppError()

    data class Unknown(val status: Int?, val detail: String?) : AppError(detail)

    companion object {
        fun from(status: Int, body: ApiErrorBody?): AppError = when (body?.code) {
            "UNAUTHORIZED", "BAD_CREDENTIALS", "TOKEN_EXPIRED" -> Unauthorized
            "SESSION_NOT_ACTIVE" -> SessionNotActive
            "IA_UNAVAILABLE" -> IaUnavailable
            "IA_TIMEOUT" -> IaTimeout
            "INVALID_IMAGE" -> InvalidImage
            "USERNAME_TAKEN" -> UsernameTaken
            "TOO_MANY_ATTEMPTS" -> TooManyAttempts
            "VALIDATION_ERROR" -> Validation(body.fields.orEmpty())
            else -> when {
                status == 401 -> Unauthorized
                status == 502 -> IaUnavailable
                status == 504 -> IaTimeout
                status >= 500 -> Server
                else -> Unknown(status, body?.message)
            }
        }
    }
}
