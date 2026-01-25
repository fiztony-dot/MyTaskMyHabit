

// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Fichero: C:/Users/antoniof/AndroidStudioProjects/MisTareasApp/build.gradle.kts
// (Fichero build.gradle.kts a nivel de proyecto)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.ksp) apply false

    // Aquí declaramos la versión, pero con 'apply false'
    id("io.gitlab.arturbosch.detekt") version "1.23.5" apply false
}
