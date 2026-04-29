package com.lfmm.tizonmovile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform