package com.lfmm.tizonmovile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lfmm.tizonmovile.di.initKoin

fun main() = application {
    initKoin(
        apiBaseUrl = "http://56.125.227.216:8080/api"
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "TizonMovile",
    ) {
        App()
    }
}