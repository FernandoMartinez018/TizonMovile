package com.lfmm.tizonmovile.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

actual fun provideHttpClient(): HttpClient {
    return HttpClient(OkHttp)
}
