package com.lfmm.tizonmovile.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lfmm.tizonmovile.MainActivity
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.api.AppError
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.model.Alert
import com.lfmm.tizonmovile.data.model.AlertStatus
import com.lfmm.tizonmovile.data.repository.AlertRepository
import com.lfmm.tizonmovile.data.settings.UserPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/**
 * RF-11 CA2: cada 15 min (mientras haya sesión) consulta GET /alerts?status=OPEN&since=… y muestra
 * una notificación local si hay alertas nuevas. Sin Firebase ni Play Services (D-03).
 */
class AlertsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val session: SessionManager by inject()
    private val alerts: AlertRepository by inject()
    private val prefs: UserPreferences by inject()

    override suspend fun doWork(): Result {
        if (session.token() == null) return Result.success()
        return alerts.alerts(AlertStatus.OPEN, prefs.lastAlertsCheck).fold(
            onSuccess = { found ->
                newestCreatedAt(found)?.let { prefs.lastAlertsCheck = it }
                if (found.isNotEmpty()) AlertNotifier.notify(applicationContext, found.size)
                Result.success()
            },
            onFailure = { e -> if (e == AppError.Network || e == AppError.Server) Result.retry() else Result.success() },
        )
    }

    companion object {
        private const val WORK_NAME = "tizon-alerts"

        /** El siguiente `since` es la alerta más reciente según el servidor (evita desfases de reloj). */
        fun newestCreatedAt(alerts: List<Alert>): String? =
            alerts.maxByOrNull { runCatching { OffsetDateTime.parse(it.createdAt) }.getOrNull() ?: OffsetDateTime.MIN }?.createdAt

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AlertsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

object AlertNotifier {
    private const val CHANNEL_ID = "alertas_cultivo"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.notif_channel_alerts), NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(context: Context, count: Int) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(context.getString(R.string.notif_alerts_title))
            .setContentText(context.resources.getQuantityString(R.plurals.notif_alerts_body, count, count))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
