package com.lfmm.tizonmovile.app

import android.app.Application
import com.lfmm.tizonmovile.data.auth.AuthState
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.di.appModule
import com.lfmm.tizonmovile.work.AlertNotifier
import com.lfmm.tizonmovile.work.AlertsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TizonApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TizonApplication)
            modules(appModule)
        }
        AlertNotifier.createChannel(this)

        // El sondeo de alertas corre solo mientras haya sesión iniciada (RF-11 CA2).
        val session: SessionManager by inject()
        appScope.launch {
            session.state.collect { state ->
                if (state is AuthState.LoggedIn) AlertsWorker.schedule(this@TizonApplication)
                else AlertsWorker.cancel(this@TizonApplication)
            }
        }
    }
}
