// Archivo: app/build.gradle.kts

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)
    kotlin("plugin.serialization") version "1.9.0"
    // Aquí NO ponemos versión porque ya la pusimos arriba
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    toolVersion = "1.23.5"
    buildUponDefaultConfig = true
    allRules = false
    // Nota: HEMOS QUITADO jvmTarget de aquí porque ya no existe en este nivel
}

// 3. ESTA ES LA CLAVE: Configuramos el jvmTarget dentro de todas las tareas de detekt
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "1.8" // O "17" si prefieres, pero esto ya no saldrá en rojo
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

// Leer la API key desde local.properties de forma segura
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
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

        // Exponer la API key como BuildConfig (de forma segura)
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
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
        viewBinding = true
        buildConfig = true  // Habilitar BuildConfig para usar variables de configuración
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
    /*implementation("com.google.ai.client.generativeai:generativeai:0.9.0") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    }*/

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
    /*implementation("androidx.compose.material:material-icons-extended:1.7.6")*/

    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")



}