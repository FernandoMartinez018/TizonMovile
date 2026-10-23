package com.lfmm.tizonmovile.di

import com.lfmm.tizonmovile.data.api.TizonApiClient
import com.lfmm.tizonmovile.data.repository.AnalysisRepository
import com.lfmm.tizonmovile.data.repository.AnalysisRepositoryImpl
import com.lfmm.tizonmovile.viewmodel.CameraViewModel
import com.lfmm.tizonmovile.viewmodel.LoginViewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Módulo Koin con todas las dependencias de la aplicación.
 *
 * Jerarquía de dependencias:
 *   TizonApiClient (singleton)
 *     └── AnalysisRepositoryImpl (singleton)
 *           ├── LoginViewModel (singleton de sesión)
 *           └── CameraViewModel (singleton de sesión)
 *
 * Se usa singleton para los ViewModels porque deben sobrevivir
 * a las recomposiciones de Compose.
 */
val appModule = module {

    // ── Configuración (URL de la API) ─────────────────────────────────────
    single<String>(qualifier = org.koin.core.qualifier.named("apiBaseUrl")) {
        // Valor por defecto: se sobreescribe en cada plataforma
        // Android: BuildConfig.API_BASE_URL
        // iOS: Info.plist o constante hardcodeada
        "http://56.125.227.216:8080/api"
    }

    // ── Capa de red ───────────────────────────────────────────────────────
    single {
        TizonApiClient(
            baseUrl = get(qualifier = named("apiBaseUrl"))
        )
    }

    // ── Repositorio ───────────────────────────────────────────────────────
    single<AnalysisRepository> {
        AnalysisRepositoryImpl(apiClient = get())
    }

    // ── ViewModels ────────────────────────────────────────────────────────
    single { LoginViewModel(repository = get()) }
    single { CameraViewModel(repository = get()) }
}

/**
 * Inicializa Koin. Llamar desde el punto de entrada de cada plataforma:
 *   - Android: Application.onCreate()
 *   - iOS: MainViewController (antes de ComposeUIViewController)
 */
fun initKoin(
    apiBaseUrl: String,
    appDeclaration: KoinAppDeclaration = {}
) {
    startKoin {
        appDeclaration()
        modules(
            // Módulo de configuración con la URL real de la plataforma
            module {
                single<String>(qualifier = org.koin.core.qualifier.named("apiBaseUrl")) {
                    apiBaseUrl
                }
            },
            appModule
        )
    }
}