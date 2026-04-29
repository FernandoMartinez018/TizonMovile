package com.lfmm.tizonmovile.network

import io.ktor.client.HttpClient

expect fun provideHttpClient(): HttpClient
