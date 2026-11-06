package com.lfmm.tizonmovile.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.lfmm.tizonmovile.data.model.Location
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

interface LocationProvider {
    /** Posición actual o null si no hay permiso, no hay proveedor o se agotó el tiempo. */
    suspend fun current(): Location?
}

/**
 * GPS open source (RF-10, D-03): android.location.LocationManager con los proveedores GPS y red;
 * sin Google Play Services. Usa la última posición si tiene menos de 2 min; si no, espera una
 * lectura nueva hasta 10 s. Sin permiso, el escaneo sigue sin ubicación.
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission") // se verifica en hasPermission()
    override suspend fun current(): Location? {
        if (!hasPermission()) return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null

        val recent = providers.mapNotNull { manager.getLastKnownLocation(it) }
            .filter { System.currentTimeMillis() - it.time <= MAX_AGE_MS }
            .minByOrNull { it.accuracy }
        if (recent != null) return recent.toModel()

        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location.toModel())
                    }
                }
                providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        }
    }

    private fun hasPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun android.location.Location.toModel() =
        Location(latitude = latitude, longitude = longitude, accuracyM = if (hasAccuracy()) accuracy.toDouble() else null)

    private companion object {
        const val MAX_AGE_MS = 2 * 60 * 1000L
        const val TIMEOUT_MS = 10_000L
    }
}
