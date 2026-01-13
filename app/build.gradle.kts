// Archivo: app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)

    // AÑADE ESTA LÍNEA AQUÍ ABAJO:
    kotlin("plugin.serialization") version "1.9.0" // Usa la versión de tu Kotlin
}

android {
    namespace = "com.example.mistareasapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mistareasapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Dependencias de AndroidX y Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Dependencia de Navegación ---
    implementation(libs.navigation.compose)

    // --- Dependencias para Room ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.places)
    implementation(libs.generativeai)
    ksp(libs.androidx.room.compiler)

    // --- Dependencias de Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended")

    // --- GEMINI (Asegúrate de que esta línea esté dentro de las llaves de dependencies) ---
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Ktor: El motor para hacer peticiones REST
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")

    // Para que Ktor entienda y genere JSON
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Para implementar notificaciones en Android que se activen exactamente en la fecha y hora límite, necesitamos usar WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")


}