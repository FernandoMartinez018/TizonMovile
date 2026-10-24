package com.lfmm.tizonmovile.di

import com.lfmm.tizonmovile.BuildConfig
import com.lfmm.tizonmovile.data.api.TizonApiClient
import com.lfmm.tizonmovile.data.auth.EncryptedTokenStore
import com.lfmm.tizonmovile.data.auth.SessionManager
import com.lfmm.tizonmovile.data.auth.TokenStore
import com.lfmm.tizonmovile.data.location.AndroidLocationProvider
import com.lfmm.tizonmovile.data.location.LocationProvider
import com.lfmm.tizonmovile.data.repository.*
import com.lfmm.tizonmovile.data.settings.SharedPrefsUserPreferences
import com.lfmm.tizonmovile.data.settings.UserPreferences
import com.lfmm.tizonmovile.ui.AppViewModel
import com.lfmm.tizonmovile.ui.alerts.AlertsViewModel
import com.lfmm.tizonmovile.ui.auth.AuthViewModel
import com.lfmm.tizonmovile.ui.history.HistoryViewModel
import com.lfmm.tizonmovile.ui.profile.ProfileViewModel
import com.lfmm.tizonmovile.ui.profile.UsersViewModel
import com.lfmm.tizonmovile.ui.scan.ScanViewModel
import com.lfmm.tizonmovile.ui.summary.SessionDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Dependencias de la app (Koin). Los ViewModels son de AndroidX: sobreviven a la rotación. */
val appModule = module {
    single<TokenStore> { EncryptedTokenStore(androidContext()) }
    single { SessionManager(get()) }
    single<UserPreferences> { SharedPrefsUserPreferences(androidContext()) }
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }

    single {
        val session = get<SessionManager>()
        TizonApiClient(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = session::token,
            onUnauthorized = session::expire,
        )
    }

    single<AuthRepository> { ApiAuthRepository(get(), get()) }
    single<ScanRepository> { ApiScanRepository(get()) }
    single<AlertRepository> { ApiAlertRepository(get()) }
    single<AdminRepository> { ApiAdminRepository(get()) }

    viewModel { AppViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { ScanViewModel(get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { (sessionId: String) -> SessionDetailViewModel(sessionId, get()) }
    viewModel { AlertsViewModel(get(), get()) }
    viewModel { (appVersion: String) -> ProfileViewModel(get(), get(), get(), appVersion) }
    viewModel { UsersViewModel(get(), get()) }
}
