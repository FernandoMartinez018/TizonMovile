import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Proyecto solo Android (decisión D-01): módulo Android con Jetpack Compose, sin Kotlin Multiplatform.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// URL de la API: -Ptizon.apiBaseUrl=... o la clave tizon.apiBaseUrl de local.properties (fuera de git).
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiBaseUrl: Provider<String> = providers.gradleProperty("tizon.apiBaseUrl")
    .orElse(providers.provider { localProperties.getProperty("tizon.apiBaseUrl") })

android {
    namespace = "com.lfmm.tizonmovile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        applicationId = "com.lfmm.tizonmovile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.getOrElse("http://10.0.2.2:8080/api/")}\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.getOrElse("")}\"")
        }
    }
    testOptions {
        // android.util.Log (logger de Ktor en debug) devuelve valores por defecto en pruebas JVM.
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// La APK release solo puede apuntar a HTTPS (RNF-SEG-06).
val checkReleaseApiBaseUrl by tasks.registering {
    val url = apiBaseUrl.orElse("")
    doLast {
        require(url.get().startsWith("https://")) {
            "La variante release requiere -Ptizon.apiBaseUrl=https://<host>/api/ (valor actual: '${url.get()}')"
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseApiBaseUrl) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
