// Fichero: settings.gradle.kts

/**
 * Este bloque gestiona dónde Gradle debe buscar los plugins.
 * Es crucial para que encuentre los plugins de Android, Kotlin y Compose.
 */
pluginManagement {
    repositories {
        // Repositorio de Google para plugins de Android y relacionados.
        // La sección 'content' optimiza la búsqueda, es una buena práctica.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Repositorio central de Maven.
        mavenCentral()
        // Repositorio oficial de plugins de Gradle. Aquí se encuentra el plugin del compilador de Compose.
        gradlePluginPortal()
    }
}

/**
 * Este bloque gestiona dónde Gradle debe buscar las librerías y dependencias
 * (como Room, Compose UI, etc.).
 */
dependencyResolutionManagement {
    // Configuración recomendada que evita que los repositorios se declaren a nivel de módulo.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Define el nombre del proyecto raíz.
rootProject.name = "MisTareasApp"

// Incluye el módulo ':app' en el build del proyecto.
include(":app")

