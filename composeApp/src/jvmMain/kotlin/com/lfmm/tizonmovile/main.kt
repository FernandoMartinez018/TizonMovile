package com.lfmm.tizonmovile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lfmm.tizonmovile.di.initKoin

fun main() = application {
    initKoin(
        apiBaseUrl = "http://52.67.50.46:8080/api"
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "TizonMovile",
    ) {
        App()
    }
}