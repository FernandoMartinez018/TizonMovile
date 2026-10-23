package com.lfmm.tizonmovile.app

import android.app.Application
import com.lfmm.tizonmovile.BuildConfig
import com.lfmm.tizonmovile.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class TizonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin(
            apiBaseUrl = BuildConfig.API_BASE_URL
        ) {
            androidContext(this@TizonApplication)
        }
    }
}