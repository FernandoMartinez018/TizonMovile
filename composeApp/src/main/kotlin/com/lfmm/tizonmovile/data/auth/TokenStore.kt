package com.lfmm.tizonmovile.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lfmm.tizonmovile.data.api.TizonApiClient
import com.lfmm.tizonmovile.data.model.User
import kotlinx.serialization.encodeToString

/** Sesión guardada en el dispositivo: token, vencimiento (ISO-8601) y usuario. */
data class StoredSession(val token: String, val expiresAt: String, val user: User)

interface TokenStore {
    fun load(): StoredSession?
    fun save(session: StoredSession)
    fun clear()
}

/**
 * Token cifrado con EncryptedSharedPreferences (RF-02 CA3, RNF-SEG-08): la llave vive en el
 * Android Keystore y el archivo no se puede leer aunque se extraiga del teléfono.
 */
@Suppress("DEPRECATION") // security-crypto 1.1 está deprecado pero es lo que exige RF-02 y sigue soportado.
class EncryptedTokenStore(context: Context) : TokenStore {

    private val prefs: SharedPreferences = open(context)

    override fun load(): StoredSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = prefs.getString(KEY_EXPIRES_AT, null) ?: return null
        val user = prefs.getString(KEY_USER, null)
            ?.let { runCatching { TizonApiClient.JSON.decodeFromString<User>(it) }.getOrNull() }
            ?: return null
        return StoredSession(token, expiresAt, user)
    }

    override fun save(session: StoredSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_EXPIRES_AT, session.expiresAt)
            .putString(KEY_USER, TizonApiClient.JSON.encodeToString(session.user))
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "tizon_session"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER = "user"

        fun open(context: Context): SharedPreferences {
            val create = {
                EncryptedSharedPreferences.create(
                    context, FILE,
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }
            // Si la llave del Keystore se perdió (p. ej. restauración de backup), se descarta la sesión.
            return runCatching(create).getOrElse {
                context.deleteSharedPreferences(FILE)
                create()
            }
        }
    }
}
