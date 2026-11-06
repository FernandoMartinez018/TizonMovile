package com.lfmm.tizonmovile.data.settings

import android.content.Context

/** Preferencias no sensibles del usuario en este dispositivo. */
interface UserPreferences {
    var welcomeSeen: Boolean
    /** RF-10 CA4: se recuerda la última finca o lote usado. */
    var lastFieldName: String?
    /** Último sondeo de alertas (ISO-8601) para pedir solo las nuevas (RF-11 CA2). */
    var lastAlertsCheck: String?
}

class SharedPrefsUserPreferences(context: Context) : UserPreferences {

    private val prefs = context.getSharedPreferences("tizon_prefs", Context.MODE_PRIVATE)

    override var welcomeSeen: Boolean
        get() = prefs.getBoolean("welcome_seen", false)
        set(value) = prefs.edit().putBoolean("welcome_seen", value).apply()

    override var lastFieldName: String?
        get() = prefs.getString("last_field_name", null)
        set(value) = prefs.edit().putString("last_field_name", value).apply()

    override var lastAlertsCheck: String?
        get() = prefs.getString("last_alerts_check", null)
        set(value) = prefs.edit().putString("last_alerts_check", value).apply()
}
