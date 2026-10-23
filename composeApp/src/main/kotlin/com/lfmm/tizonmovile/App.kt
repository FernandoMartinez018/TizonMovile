package com.lfmm.tizonmovile

import androidx.compose.runtime.*
import com.lfmm.tizonmovile.ui.screens.CameraScreen
import com.lfmm.tizonmovile.ui.screens.LoginScreen
import com.lfmm.tizonmovile.ui.screens.ReportScreen
import com.lfmm.tizonmovile.ui.theme.TizonTheme
import com.lfmm.tizonmovile.viewmodel.CameraViewModel
import com.lfmm.tizonmovile.viewmodel.LoginViewModel
import org.koin.compose.koinInject

private enum class Screen { LOGIN, CAMERA, REPORT }

@Composable
fun App() {
    TizonTheme {
        val loginVm: LoginViewModel = koinInject()
        val cameraVm: CameraViewModel = koinInject()
        var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

        when (currentScreen) {
            Screen.LOGIN  -> LoginScreen(viewModel = loginVm, onLoginSuccess = { currentScreen = Screen.CAMERA })
            Screen.CAMERA -> CameraScreen(viewModel = cameraVm, onNavigateToReport = { currentScreen = Screen.REPORT })
            Screen.REPORT -> ReportScreen(viewModel = cameraVm, onBack = { currentScreen = Screen.CAMERA })
        }
    }
}