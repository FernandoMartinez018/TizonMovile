package com.lfmm.tizonmovile.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*

actual fun provideHttpClient(): HttpClient {
    return HttpClient(CIO)
}
