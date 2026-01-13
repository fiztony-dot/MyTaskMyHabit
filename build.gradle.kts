

// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Fichero: C:/Users/antoniof/AndroidStudioProjects/MisTareasApp/build.gradle.kts
// (Fichero build.gradle.kts a nivel de proyecto)

plugins {
    // Declara los plugins que los módulos pueden aplicar.
    // 'apply false' significa que el plugin no se aplica al proyecto raíz,
    // sino que se hace disponible para los submódulos.

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // ✨ CORRECCIÓN: El alias correcto es 'compose.compiler', no 'kotlin.compose'
    alias(libs.plugins.compose.compiler) apply false

    // ✨ AÑADIDO: También debes declarar el plugin de KSP aquí
    alias(libs.plugins.google.ksp) apply false

    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false



}
